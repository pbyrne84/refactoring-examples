package refactoring_examples.extract_class.dependencies

import refactoring_examples.extract_class.{ActionSuccess, SummarizationDataError}

import java.time.Instant

case class InvalidAdminUserInteraction(operatingMode: OperatingMode)

case class InvalidNormalUserInteraction()

class AuditingService {

  def recordAdminUserAttemptedInvalidAction(
      adminUser: AdminUser,
      invalidAdminUserInteraction: InvalidAdminUserInteraction
  ): Either[Throwable, ActionSuccess.type] = ???

  def recordNormalUserAttemptedInvalidAction(
      normalUser: NormalUser,
      invalidAdminUserInteraction: InvalidNormalUserInteraction
  ): Either[Throwable, ActionSuccess.type] = ???

  def recordSuccessfulSummarization(userId: Int, startingInstant: Instant): Either[Throwable, true] = ???
}
