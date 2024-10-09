package refactoring_examples.extract_class

import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.{
  FriendlyWetWorkOperative,
  NonCombatant,
  PossibleEnemySpy
}
import refactoring_examples.extract_class.dependencies.OperatingMode.{OperatingMode1, OperatingMode2, OperatingMode3}
import refactoring_examples.extract_class.dependencies.User.UserId
import refactoring_examples.extract_class.dependencies.errors.{
  ExampleError,
  ExpectedUserNotFound,
  FailedSendingSummarisedFinancialData,
  FailedSummarisingFinancialData,
  FailedValidatingUserWithOperatingMode,
  ServiceFailedInAttemptingUserRetrievalError
}
import refactoring_examples.extract_class.dependencies.{FinancialSystemsError, *}

import java.time.{Clock, Instant, Period}

/**
 * No squirrelling things away hidden in traits etc. Making the dirtiness clear as it communicates how dirty the code
  * is. Sometimes cleaning up a test is doing the wrong thing. Everything should be clear what is operated on, if we
  * delete the code, then the question is can we rewrite it out from the test? That is one of the metrics a test should
  * be measured against. It is a tool to aid our understanding EASIER than reading the code, having a lot of test to
  * memorize as well as code to memorize defeats the purpose of trying to LOWER the cognitive load. The lower the
  * cognitive load, the more energy we have to do more.
  *
  * This is what our brain actually has to extrapolate to, whatever way we decide to organize it. Complexity is governed
  * by the mental steps we need to take and how much working memory we need to use.
  */
class PoorSeparationOfConcernsExampleSpec
    extends AnyFreeSpecLike
    with TableDrivenPropertyChecks
    with MockFactory
    with Matchers {

  // Keeping this at the top means it is easily referred to. We need to refer to things in the test easily
  // else we cannot spot problems. The ability you gain, the more problems you can spot.
  // So more senior members need to keep an eye on things as we can spot things others cannot.
  private val clock: Clock = mock[Clock]
  private val userService: UserService = mock[UserService]
  private val auditingService: AuditingService = mock[AuditingService]
  private val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
  private val summarizationReportingService: SummarizationReportingService =
    mock[SummarizationReportingService]

  private val poorSeparationOfConcernsExample = new PoorSeparationOfConcernsExample(
    clock,
    userService,
    auditingService,
    financialDetailsService,
    summarizationReportingService
  )

  def prettyWithClue[A](message: String)(a: => A): A = {
    withClue(message + " ->\n")(a)
  }

  private val userId = UserId(1)
  private val normalAdminUserId = UserId(2)
  private val superAdminUserId = UserId(3)

  // There should be 7, joy. Try doing this without looking at the code. BAD TDD, BAD TDD, TDD IS BROKEN!
  // We are starting to feel like we are building a test engine, the production code is the engine.
  private val validUserOperatingModeCombinations = Table(
    ("user returned", "operation"),
    (AdminUser(normalAdminUserId, false), OperatingMode2),
    (AdminUser(superAdminUserId, true), OperatingMode1),
    (AdminUser(normalAdminUserId, false), OperatingMode3),
    (AdminUser(superAdminUserId, true), OperatingMode3),
    // now for the 6 normal user cases
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode1),
    (NormalUser(normalAdminUserId, NonCombatant), OperatingMode1),
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode2),
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode3),
    (NormalUser(normalAdminUserId, NonCombatant), OperatingMode3),
    (NormalUser(normalAdminUserId, PossibleEnemySpy), OperatingMode3)
  )

  "sendSummarization" - {
    extension [A, B](either: Either[A, B]) {
      // We are using typed expections versus their messages to communicate type of failure
      // this makes it easy to compare to Right(classOf[CustomException])
      // We could use EitherValues, but it creates a less easy to read error when it is a Right
      // This will diff
      // got Right(x) expected Left(y)
      private def remapLeftToClass: Either[Class[? <: A], B] = either.left.map(_.getClass)
    }

    "should succeed" - {
      // keeping these things near so we can see them and not strain memorization
      // The issue with relying on memorization is we have limitied cognitive working memory
      // and we need to use it to make decisions. Everthing needs to be able to be worked on in memory and bouncing
      // around a file strains focus, this is why we split things up into files. Easier to visually memorize, not verbally
      // memorize which it feels like this type of organization relies upon. Visually is much more effective if you can do it.
      // This is really a poor test as it is doing so much, when at this point we are really trying to
      // make sure we haven't messed up the filtering logic
      val currentInstant = Instant.EPOCH.plus(Period.ofDays(66))
      val startOfExpectedSummarization = currentInstant.minus(Period.ofDays(33))
      val maxDaysToProcess = 33

      "when no FinancialPeriodStatements were found" in {
        runExpectedSuccess(List.empty, List.empty)
      }

      def runExpectedSuccess(
          pencePeriodStartCombinations: List[(Int, Pence, Instant)],
          expectedSummarizedStatementCombinations: List[(Int, Pence, Instant)]
      )(implicit position: Position) = {
        forAll(validUserOperatingModeCombinations) { (user: User, operatingMode: OperatingMode) =>
          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          def createStatement(recordId: Int, user: User, pence: Pence, periodStart: Instant) = {
            FinancialPeriodStatement(
              recordId,
              user.userId.value,
              1,
              pence,
              Instant.EPOCH,
              FinancialPeriod(periodStart, periodStart.plus(Period.ofDays(100)))
            )
          }

          val financialDetails = pencePeriodStartCombinations.map { case (recordId, pence, periodStart) =>
            createStatement(recordId, user, pence, periodStart)
          }

          val expectedSentDetails = expectedSummarizedStatementCombinations.map { case (recordId, pence, periodStart) =>
            createStatement(recordId, user, pence, periodStart)
          }

          financialDetailsService.getDetails
            .expects(user)
            .returns(Right(financialDetails))

          // Changing values in tests create noise, and we always want to control time as it is one of the more
          // challenging things to work with
          (() => clock.instant)
            .expects()
            .returns(currentInstant)

          summarizationReportingService.sendSummarization
            .expects(
              SummarizedAccountDetails(user, startOfExpectedSummarization, expectedSentDetails)
            )
            .returns(Right(true))

          auditingService.recordSuccessfulSummarization
            .expects(user.userId.value, startOfExpectedSummarization)
            .returns(Right(true))

          val errorOrCount =
            poorSeparationOfConcernsExample.sendSummarization(user.userId.value, operatingMode, maxDaysToProcess)

          // Calculating the count here is a hacky, better to pass it in to be able to summarise easily
          // It is the variance of state and the consequences of it that we are testing
          errorOrCount.remapLeftToClass shouldBe Right(SummarizationCount(expectedSummarizedStatementCombinations.size))
        }
      }

      "when no FinancialPeriodStatements were found after filtering looking for max days to go back" in {
        runExpectedSuccess(
          List((1, Pence(2), currentInstant.minus(Period.ofDays(maxDaysToProcess)).minusNanos(1))),
          List.empty
        )
      }

      "when a FinancialPeriodStatement were found after filtering " in {
        runExpectedSuccess(
          List((1, Pence(2), currentInstant.minus(Period.ofDays(maxDaysToProcess)))),
          List((1, Pence(2), currentInstant.minus(Period.ofDays(maxDaysToProcess))))
        )
      }

      "when a multiple statements were found after filtering" in {
        runExpectedSuccess(
          List(
            (1, Pence(2), currentInstant.minus(Period.ofDays(maxDaysToProcess + 1))),
            (2, Pence(3), currentInstant.minus(Period.ofDays(maxDaysToProcess))),
            (3, Pence(5), currentInstant.minus(Period.ofDays(maxDaysToProcess - 1))),
            (4, Pence(7), currentInstant.minus(Period.ofDays(maxDaysToProcess - 2))),
            (5, Pence(1), currentInstant.minus(Period.ofDays(maxDaysToProcess + 2)))
          ),
          List(
            (2, Pence(3), currentInstant.minus(Period.ofDays(maxDaysToProcess))),
            (3, Pence(5), currentInstant.minus(Period.ofDays(maxDaysToProcess - 1))),
            (4, Pence(7), currentInstant.minus(Period.ofDays(maxDaysToProcess - 2)))
          )
        )
      }

    }

    "should fail" - {

      "for user based retrieval reasons" - {
        "when the user fails attempting retrieval for unknown reasons" in {
          val options = Table("mode", OperatingMode.values*)

          forAll(options) { option =>
            userService.getUser
              .expects(userId.value)
              .returns(Left(new RuntimeException("failed connecting to user service")))

            poorSeparationOfConcernsExample.sendSummarization(userId.value, option, 33).remapLeftToClass shouldBe
              Left(classOf[ServiceFailedInAttemptingUserRetrievalError])
          }
        }

        "when the user is not found" in {
          val options = Table("mode", OperatingMode.values*)

          forAll(options) { option =>
            userService.getUser
              .expects(userId.value)
              .returns(Right(None))

            poorSeparationOfConcernsExample.sendSummarization(userId.value, option, 33).remapLeftToClass shouldBe
              Left(classOf[ExpectedUserNotFound])
          }
        }
      }

      // This is high risk so needs to be CLEAR on how it is operating at a glance.
      // Quite often we spot things when doing other things. This ties into Linus's Law
      // "given enough eyeballs, all bugs are shallow"
      // we do not want bugs in our tests as they can align with quite serious bugs in our code.
      // The code tests the test as well as the test tests the code. The simpler side of the equation is really the test
      // as we use that for the metric of correctness. Writing tests last leads to more complicated tests, until you have
      // actually mastered writing tests. Still test first can be a lot calmer experience if things are kept well-organised.
      // Calm (and fast) does not feel smart though, we link cortisol with being smart too much :P
      // Relationships are bidirectional, thinking like that increases quality.
      "for the following user mode/operating mode combinations" - {
        val superAdminUser = AdminUser(superAdminUserId, isSuperAdmin = true)
        val adminUser = AdminUser(normalAdminUserId, isSuperAdmin = false)

        def createNormalUser(trustLevel: NormalUserTrustLevel) = NormalUser(userId, trustLevel)

        "as a super admin attempting an OperatingMode2 which is only allowed for non super admins" in {
          // we are having to loop due to the bad design of the code
          // Every failure branch in the class has to be recorded. We reduce this stuff by reducing the branches
          // that lead to this. Complex chains of events should be as linear as possible, ideally completely linear.
          // This is still simple at only 2. If the test didn't feel so crowded, we could then skip the looping
          val recordingFailureOptions: TableFor2[Either[RuntimeException, ActionSuccess.type], Boolean] = Table(
            ("result of trying to record failure", "expected value in failure result"),
            (Right(ActionSuccess), false),
            (Left(new RuntimeException("failed recording auditing")), true)
          )

          forAll(recordingFailureOptions) { (recordingFailureResult, expectedHasFailedAuditing) =>
            // we need to see where things are going, wildcard values in mocks are not good
            // unless it is actually noise, such as an ExecutionContext. If it is a value we rely on, then
            // it should not be *. Relationships are bidirectional and * makes it one directional, test wise anyway.
            userService.getUser
              .expects(superAdminUser.userId.value)
              .returns(Right(Some(superAdminUser)))

            auditingService.recordAdminUserAttemptedInvalidAction
              .expects(superAdminUser, InvalidAdminUserInteraction(OperatingMode2))
              .returns(recordingFailureResult)

            val errorOrCount =
              poorSeparationOfConcernsExample.sendSummarization(superAdminUser.userId.value, OperatingMode2, 33)

            assertOperatingModeUserValidationFailure(
              errorOrCount,
              expectedHasFailedAuditing = expectedHasFailedAuditing
            )
          }
        }

        def assertOperatingModeUserValidationFailure(
            errorOrCount: Either[ExampleError, SummarizationCount],
            expectedHasFailedAuditing: Boolean
        )(implicit pos: Position) = {
          prettyWithClue("expected the audit failure state to be") {
            errorOrCount match {
              case Left(exampleError: ExampleError) =>
                exampleError match {
                  case correctFailure: FailedValidatingUserWithOperatingMode =>
                    correctFailure.hasFailedRecordingAudit shouldBe expectedHasFailedAuditing

                  case _ =>
                    // Do this to assert a normal failure message
                    exampleError.getClass shouldBe a[FailedValidatingUserWithOperatingMode]
                }
              case _ =>
                // again just a nicer error
                errorOrCount.remapLeftToClass shouldBe Left(a[FailedValidatingUserWithOperatingMode])
            }
          }
        }

        "as a normal admin is attempting an OperatingMode1 which is only allowed for super admins" in {
          val recordingFailureOptions: TableFor2[Either[RuntimeException, ActionSuccess.type], Boolean] = Table(
            ("result of trying to record failure", "expected value in failure result"),
            (Right(ActionSuccess), false),
            (Left(new RuntimeException("failed recording auditing")), true)
          )

          forAll(recordingFailureOptions) { (recordingFailureResult, expectedHasFailedAuditing) =>
            userService.getUser
              .expects(adminUser.userId.value)
              .returns(Right(Some(adminUser)))

            auditingService.recordAdminUserAttemptedInvalidAction
              .expects(adminUser, InvalidAdminUserInteraction(OperatingMode1))
              .returns(recordingFailureResult)

            val errorOrCount =
              poorSeparationOfConcernsExample.sendSummarization(adminUser.userId.value, OperatingMode1, 33)

            assertOperatingModeUserValidationFailure(
              errorOrCount,
              expectedHasFailedAuditing = expectedHasFailedAuditing
            )
          }
        }

        "as a normal user OperationMode1 does not allow possible spies" in {
          val recordingFailureOptions: TableFor2[Either[RuntimeException, ActionSuccess.type], Boolean] = Table(
            ("result of trying to record failure", "expected value in failure result"),
            (Right(ActionSuccess), false),
            (Left(new RuntimeException("failed recording auditing")), true)
          )

          forAll(recordingFailureOptions) { (recordingFailureResult, expectedHasFailedAuditing) =>
            val normalSpyUser = createNormalUser(PossibleEnemySpy)
            userService.getUser
              .expects(normalSpyUser.userId.value)
              .returns(Right(Some(normalSpyUser)))

            auditingService.recordNormalUserAttemptedInvalidAction
              .expects(normalSpyUser, InvalidNormalUserInteraction())
              .returns(recordingFailureResult)

            val errorOrCount =
              poorSeparationOfConcernsExample.sendSummarization(normalSpyUser.userId.value, OperatingMode1, 33)

            assertOperatingModeUserValidationFailure(
              errorOrCount,
              expectedHasFailedAuditing = expectedHasFailedAuditing
            )
          }
        }

        "as a normal user OperationMode2 fails on anything but FriendlyWetWorkOperative" in {
          val allOptionsButWetWorkOperatives =
            NormalUserTrustLevel.values.filterNot(_ == NormalUserTrustLevel.FriendlyWetWorkOperative).toList

          // We have to do this due to poor code design
          val tableOptions = allOptionsButWetWorkOperatives.flatMap { trustLevel =>
            List(
              (Right(ActionSuccess), false, trustLevel),
              (Left(new RuntimeException("failed recording auditing")), true, trustLevel)
            )
          }

          val recordingFailureOptions = Table(
            ("result of trying to record failure", "expected value in failure result", "trust level"),
            tableOptions*
          )

          forAll(recordingFailureOptions) {
            (recordingFailureResult, expectedHasFailedAuditing, nonWetWorkOperatingMode: NormalUserTrustLevel) =>
              val nonSpyNormalUser = createNormalUser(nonWetWorkOperatingMode)
              userService.getUser
                .expects(nonSpyNormalUser.userId.value)
                .returns(Right(Some(nonSpyNormalUser)))

              auditingService.recordNormalUserAttemptedInvalidAction
                .expects(nonSpyNormalUser, InvalidNormalUserInteraction())
                .returns(recordingFailureResult)

              val errorOrCount =
                poorSeparationOfConcernsExample.sendSummarization(nonSpyNormalUser.userId.value, OperatingMode2, 33)

              assertOperatingModeUserValidationFailure(
                errorOrCount,
                expectedHasFailedAuditing = expectedHasFailedAuditing
              )
          }
        }

      }

      "if getting the financial details fails due to an error" in {
        // this is dirty, we want to validate the validation passes and getting to this point
        // shows we have passed validation. The only way to semi-elegantly test that is at this point :)
        // Still digging ourselves a hole by trying to fit the test around a poor implementation.
        // Horrible to debug a failure, less horrible than dynamic IN, but still VERY horrible.

        forAll(validUserOperatingModeCombinations) { (user: User, operatingMode: OperatingMode) =>
          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          financialDetailsService.getDetails
            .expects(user)
            .returns(Left(UserNotRegisteredOnFinancialSystemError(user)))

          val errorOrCount =
            poorSeparationOfConcernsExample.sendSummarization(user.userId.value, operatingMode, 33)

          errorOrCount.remapLeftToClass shouldBe Left(classOf[FailedSummarisingFinancialData])
        }
      }

      "if the the financial account data has more than one account id returned in the results, we don't trust the data for some reason" in {
        // this is dirty, we want to validate the validation passes and getting to this point
        // shows we have passed validation. The only way to semi-elegantly test that is at this point :)
        // Still digging ourselves a hole by trying to fit the test around a poor implementation.
        // Horrible to debug a failure, less horrible than dynamic IN, but still VERY horrible.

        forAll(validUserOperatingModeCombinations) { (user: User, operatingMode: OperatingMode) =>
          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          def createDetails(recordId: Int, accountId: Int) =
            FinancialPeriodStatement(
              recordId = recordId,
              userId = user.userId.value,
              accountId = accountId,
              totalInBankAtTime = Pence(1),
              createdInstant = Instant.now,
              period = FinancialPeriod(Instant.EPOCH, Instant.EPOCH)
            )

          financialDetailsService.getDetails
            .expects(user)
            .returns(Right(List(createDetails(0, 2), createDetails(0, 3))))

          val errorOrCount =
            poorSeparationOfConcernsExample.sendSummarization(user.userId.value, operatingMode, 33)

          errorOrCount.remapLeftToClass shouldBe Left(classOf[FailedSummarisingFinancialData])
        }
      }

      "if the the financial account data has a differing user id, we do not trust the data" in {
        // This is dirty, we want to validate the validation passes and getting to this point
        // shows we have passed validation. The only way to semi-elegantly test that is at this point :)
        // Still digging ourselves a hole by trying to fit the test around a poor implementation.
        // Horrible to debug a failure, less horrible than dynamic IN, but still VERY horrible.
        forAll(validUserOperatingModeCombinations) { (user: User, operatingMode: OperatingMode) =>
          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          financialDetailsService.getDetails
            .expects(user)
            .returns(
              Right(
                List(
                  createFinancialPeriodStatement(0, userId = user.userId.value),
                  createFinancialPeriodStatement(0, userId = user.userId.value + 1)
                )
              )
            )

          val errorOrCount =
            poorSeparationOfConcernsExample.sendSummarization(user.userId.value, operatingMode, 33)

          errorOrCount.remapLeftToClass shouldBe Left(classOf[FailedSummarisingFinancialData])
        }
      }

      def createFinancialPeriodStatement(
          recordId: Int,
          userId: Int,
          period: FinancialPeriod = FinancialPeriod(Instant.EPOCH, Instant.EPOCH)
      ): FinancialPeriodStatement =
        FinancialPeriodStatement(
          recordId = recordId,
          userId = userId,
          accountId = 1,
          totalInBankAtTime = Pence(1),
          createdInstant = Instant.now,
          period = period
        )

      // Really we should test it with a varying construct of FailedSendingSummarisedFinancialData
      // with summarized data as well as empty, but this implementation makes it boring to do
      // When it is refactored we can just can an answer from the mock and not care about all this detail here.
      "if sending the summarization fails" in {
        forAll(validUserOperatingModeCombinations) { (user: User, operatingMode: OperatingMode) =>
          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          financialDetailsService.getDetails
            .expects(user)
            .returns(
              Right(
                List()
              )
            )

          val maxDaysToProcess = 33

          // Changing values in tests create noise, and we always want to control time as it is one of the more
          // challenging things to work with
          val currentInstant = Instant.EPOCH.plus(Period.ofDays(66))
          (() => clock.instant)
            .expects()
            .returns(currentInstant)

          val startOfExpectedSummarization = currentInstant.minus(Period.ofDays(33))

          summarizationReportingService.sendSummarization
            .expects(
              SummarizedAccountDetails(user, startOfExpectedSummarization, List())
            )
            .returns(Left(new RuntimeException("failed sending summarization")))

          val errorOrCount =
            poorSeparationOfConcernsExample.sendSummarization(user.userId.value, operatingMode, maxDaysToProcess)

          errorOrCount.remapLeftToClass shouldBe Left(classOf[FailedSendingSummarisedFinancialData])

        }
      }
    }
  }
}
