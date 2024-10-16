package refactoring_examples.extract_class

import refactoring_examples.ActionSuccess
import refactoring_examples.extract_class.dependencies.*
import refactoring_examples.extract_class.errors.*
import refactoring_examples.extract_class.refactored_components.{
  FailureRecordingUserValidation,
  FinancialDetailsSummarization
}

/**
 * Everything is refactored out, all tests are easy to work with.
 * There is now room to split out further for readability.
 * Follow compose method/respecting levels of abstraction much clearer
 * sendSummarization gives a clear overview of what operations will happen, and if you are looking for a certain
 * operation, it is much clearer.
 * We spend a lot of our time looking for things, and in the process, it is important
 * that things we don't want to find are easily skimmable so we don't waste time and get tired.
 *
 * @param userService
 * @param auditingService
 * @param failureRecordingUserValidation
 * @param financialDetailsSummarization
 * @param summarizationReportingService
 */
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
