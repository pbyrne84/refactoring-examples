package refactoring_examples.extract_class.errors

import refactoring_examples.extract_class.dependencies.{OperatingMode, User}

sealed abstract class ExampleError(message: String, maybeCause: Option[Throwable] = None)
    extends RuntimeException(message, maybeCause.orNull)

case class ServiceFailedInAttemptingUserRetrievalError(userId: Int, cause: Throwable)
    extends ExampleError(s"unexpected error when retrieving user id $userId", Some(cause))

case class ExpectedUserNotFound(userId: Int) extends ExampleError(s"user id $userId was not found")

case class FailedValidatingUserWithOperatingMode(
    user: User,
    operatingMode: OperatingMode,
    cause: InvalidUserOperationModeError
) extends ExampleError(s"failed validating user $user with mode $operatingMode", Some(cause)) {
  val hasFailedRecordingAudit: Boolean = cause.hasFailedRecordingAudit
}

case class FailedSummarisingFinancialData(user: User, cause: Throwable)
    extends ExampleError(s"failed summarising data for $user", Some(cause))

case class FailedSendingSummarisedFinancialData(user: User, cause: Throwable)
    extends ExampleError(s"failed sending summarised data for $user", Some(cause))
