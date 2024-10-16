package refactoring_examples.extract_class

import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import refactoring_examples.ActionSuccess
import refactoring_examples.extract_class.dependencies.*
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.*
import refactoring_examples.extract_class.dependencies.OperatingMode.{OperatingMode1, OperatingMode3}
import refactoring_examples.extract_class.dependencies.User.UserId
import refactoring_examples.extract_class.errors.*
import refactoring_examples.extract_class.refactored_components.{
  FailureRecordingUserValidation,
  FinancialDetailsSummarization
}

import java.time.{Clock, Instant, Period}

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
