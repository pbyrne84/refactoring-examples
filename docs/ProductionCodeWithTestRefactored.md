# Production Code With Test Refactored 

## Production code after refactor

[PoorSeparationOfConcernsExampleRefactored.scala](../src/main/scala/refactoring_examples/extract_class/PoorSeparationOfConcernsExampleRefactored.scala)

Note the call chain is linear, the only operations we are doing is a call and then an error remapping. We no longer 
need any real complexity in the test. The complexity has been split up and refactored out into clear responsibilities
that have some room to grow. Though they will likely turn to mess as well over time, so we always need to keep pruning 
and keep things looking healthy :) 

```scala
class PoorSeparationOfConcernsExampleRefactored(
    userService: UserService,
    auditingService: AuditingService,
    failureRecordingUserValidation: FailureRecordingUserValidation,
    financialDetailsSummarization: FinancialDetailsSummarization,
    summarizationReportingService: SummarizationReportingService
) {

  def sendSummarization(
      userId: Int,
      operatingMode: OperatingMode,
      maxDaysToProcess: Int
  ): Either[ExampleError, SummarizedAccountDetails] = {
    for {
      // all calls are very high level (compose method)
      user <- attemptRetrievingUser(userId)
      _ <- attemptUserValidation(operatingMode, user)
      summarisedAccountDetails <- attemptFinancialSummarization(maxDaysToProcess, user)
      _ <- attemptSendingSummarization(user, summarisedAccountDetails)
    } yield summarisedAccountDetails
  }

  private def attemptRetrievingUser(userId: Int): Either[ExampleError, User] = {
    for {
      maybeUser <- userService
        .getUser(userId)
        .left
        .map(error => ServiceFailedInAttemptingUserRetrievalError(userId, error))

      user <- maybeUser.map(Right.apply).getOrElse(Left(ExpectedUserNotFound(userId)))
    } yield user
  }

  private def attemptUserValidation(
      operatingMode: OperatingMode,
      user: User
  ): Either[FailedValidatingUserWithOperatingMode, ActionSuccess.type] = {
    failureRecordingUserValidation
      .validateUserAgainstOperatingMode(operatingMode, user)
      .left
      .map((error: InvalidUserOperationModeError) => FailedValidatingUserWithOperatingMode(user, operatingMode, error))
  }

  private def attemptFinancialSummarization(
      maxDaysToProcess: Int,
      user: User
  ): Either[FailedSummarisingFinancialData, SummarizedAccountDetails] = {
    financialDetailsSummarization
      .calculateCurrentFinancialDetailsSummarization(user, maxDaysToProcess)
      .left
      .map(error => FailedSummarisingFinancialData(user, error))
  }

  private def attemptSendingSummarization(
      user: User,
      summarisedAccountDetails: SummarizedAccountDetails
  ): Either[FailedSendingSummarisedFinancialData, ActionSuccess.type] = {
    summarizationReportingService
      .sendSummarization(summarisedAccountDetails)
      .flatMap { _ =>
        auditingService.recordSuccessfulSummarization(user.userId.value, summarisedAccountDetails.startingInstant)
      }
      .map(_ => ActionSuccess)
      .left
      .map(error => FailedSendingSummarisedFinancialData(user, error))
  }
}
```

## Test code after refactor

[PoorSeparationOfConcernsExampleRefactoredSpec.scala](../src/test/scala/refactoring_examples/extract_class/PoorSeparationOfConcernsExampleRefactoredSpec.scala)

Again, simple enough not to require any complicated contexts, I personally never use them as they seem too much of a crutch for bad design
and end up hiding everything in them making things much worse.

```scala
class PoorSeparationOfConcernsExampleRefactoredSpec
    extends AnyFreeSpecLike
    with TableDrivenPropertyChecks
    with MockFactory
    with Matchers {

  // Keeping this at the top means it is easily referred to. We need to refer to things in the test easily
  // else we cannot spot problems. The more ability you gain, the more problems you can spot.
  // So more senior members need to keep an eye on things as we should spot things others cannot.
  private val userService: UserService = mock[UserService]
  private val auditingService: AuditingService = mock[AuditingService]
  private val financialDetailsSummarization: FinancialDetailsSummarization = mock[FinancialDetailsSummarization]
  private val summarizationReportingService: SummarizationReportingService =
    mock[SummarizationReportingService]
  private val failureRecordingUserValidation = mock[FailureRecordingUserValidation]

  private val separationOfConcernsExampleRefactored = new PoorSeparationOfConcernsExampleRefactored(
    userService,
    auditingService,
    failureRecordingUserValidation,
    financialDetailsSummarization,
    summarizationReportingService
  )

  def prettyWithClue[A](message: String)(a: => A): A = {
    withClue(message + " ->\n")(a)
  }

  private val userId = UserId(1)
  private val user = NormalUser(userId, NonCombatant)

  private val financialStatements = List(
    createFinancialStatement(1, 200, Instant.EPOCH),
    createFinancialStatement(2, 200, Instant.EPOCH),
    createFinancialStatement(3, 200, Instant.EPOCH)
  )

  private def createFinancialStatement(recordId: Int, pence: Int, createdInstant: Instant) =
    FinancialPeriodStatement(
      recordId = recordId,
      userId = user.userId.value,
      accountId = 1,
      totalInBankAtTime = Pence(pence),
      createdInstant = createdInstant,
      period = FinancialPeriod(createdInstant.plusSeconds(200), createdInstant.plusSeconds(400))
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

      /**
       * No looping, yay.
       * We have moved the complexity by extracting user validation and summary calculation
       * The code is fairly linear, so the test is fairly linear and easier to understand.
       * I did intend to do the summary calculation in another example, but it is adding to much noise
       * and it is tiresome. Easier and faster to refactor
       */
      "and return the summarised accounting details" in {
        val user = NormalUser(userId, NonCombatant)
        val maxDaysToProcess = 3

        userService.getUser
          .expects(user.userId.value)
          .returns(Right(Some(user)))

        failureRecordingUserValidation.validateUserAgainstOperatingMode
          .expects(OperatingMode1, user)
          .returns(Right(ActionSuccess))

        val summarizationStartDate = Instant.EPOCH.plus(Period.ofDays(2222))

        val financialStatements = List(
          createFinancialStatement(1, 200, Instant.EPOCH),
          createFinancialStatement(2, 200, Instant.EPOCH),
          createFinancialStatement(3, 200, Instant.EPOCH)
        )

        val calculatedSummarisedDetails = SummarizedAccountDetails(user, summarizationStartDate, financialStatements)

        financialDetailsSummarization.calculateCurrentFinancialDetailsSummarization
          .expects(user, maxDaysToProcess)
          .returns(Right(calculatedSummarisedDetails))

        summarizationReportingService.sendSummarization
          .expects(calculatedSummarisedDetails)
          .returns(Right(ActionSuccess))

        auditingService.recordSuccessfulSummarization
          .expects(user.userId.value, summarizationStartDate)
          .returns(Right(ActionSuccess))

        separationOfConcernsExampleRefactored.sendSummarization(
          user.userId.value,
          OperatingMode1,
          maxDaysToProcess = maxDaysToProcess
        ) shouldBe Right(
          calculatedSummarisedDetails
        )
      }
    }

    "should fail" - {

      "when the user is not found" - {
        // Operating mode actually has no logical action within the class now
        // You could pass a random one down, but random can cause random failures if clumsy.
        // Someone could hard code OperatingMode3 in the implementation, but you need to have some faith in ability.
        "when the user fails attempting retrieval for unknown reasons" in {
          userService.getUser
            .expects(userId.value)
            .returns(Left(new RuntimeException("failed connecting to user service")))

          separationOfConcernsExampleRefactored
            .sendSummarization(userId.value, OperatingMode3, 33)
            .remapLeftToClass shouldBe
            Left(classOf[ServiceFailedInAttemptingUserRetrievalError])
        }

        "when the user is not found" in {
          userService.getUser
            .expects(userId.value)
            .returns(Right(None))

          separationOfConcernsExampleRefactored
            .sendSummarization(userId.value, OperatingMode3, 33)
            .remapLeftToClass shouldBe
            Left(classOf[ExpectedUserNotFound])
        }

        "when the user fails validation" in {
          val user = NormalUser(userId, NonCombatant)
          val maxDaysToProcess = 3

          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          failureRecordingUserValidation.validateUserAgainstOperatingMode
            .expects(OperatingMode3, user)
            .returns(Left(InvalidNormalUserOperationModeError(user, OperatingMode3, true)))

          separationOfConcernsExampleRefactored
            .sendSummarization(userId.value, OperatingMode3, maxDaysToProcess)
            .remapLeftToClass shouldBe
            Left(classOf[FailedValidatingUserWithOperatingMode])
        }

        "when the calculateCurrentFinancialDetailsSummarization fails" in {
          val user = NormalUser(userId, NonCombatant)
          val maxDaysToProcess = 3

          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          failureRecordingUserValidation.validateUserAgainstOperatingMode
            .expects(OperatingMode3, user)
            .returns(Right(ActionSuccess))

          financialDetailsSummarization.calculateCurrentFinancialDetailsSummarization
            .expects(user, maxDaysToProcess)
            .returns(Left(new RuntimeException("failed summarization")))

          separationOfConcernsExampleRefactored
            .sendSummarization(userId.value, OperatingMode3, maxDaysToProcess)
            .remapLeftToClass shouldBe
            Left(classOf[FailedSummarisingFinancialData])
        }

        "when the sendSummarization fails" in {
          val user = NormalUser(userId, NonCombatant)
          val maxDaysToProcess = 3
          val summarisedDetails = SummarizedAccountDetails(
            user,
            Instant.EPOCH,
            financialStatements
          )

          userService.getUser
            .expects(user.userId.value)
            .returns(Right(Some(user)))

          failureRecordingUserValidation.validateUserAgainstOperatingMode
            .expects(OperatingMode3, user)
            .returns(Right(ActionSuccess))

          financialDetailsSummarization.calculateCurrentFinancialDetailsSummarization
            .expects(user, maxDaysToProcess)
            .returns(Right(summarisedDetails))

          summarizationReportingService.sendSummarization
            .expects(summarisedDetails)
            .returns(Left(new RuntimeException("failed sending summarization")))

          separationOfConcernsExampleRefactored
            .sendSummarization(userId.value, OperatingMode3, maxDaysToProcess)
            .remapLeftToClass shouldBe
            Left(classOf[FailedSendingSummarisedFinancialData])
        }
      }
    }
  }
}

```
