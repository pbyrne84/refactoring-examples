package refactoring_examples.extract_class.refactored_components

import refactoring_examples.ActionSuccess
import refactoring_examples.extract_class.dependencies.*
import refactoring_examples.extract_class.errors.{
  InvalidAdminUserOperationModeError,
  InvalidNormalUserOperationModeError,
  InvalidUserOperationModeError
}

/**
 * Extracting this here removes 5 paths that can easily be tested, so we have removed 30 paths of complexity
 * from the main class so it only has to deal with the success and failure of this.
 *
 * @param userOperationValidation
 * @param auditingService
 */
class FailureRecordingUserValidation(
    userOperationValidation: UserOperationValidation,
    auditingService: AuditingService
) {

  def validateUserAgainstOperatingMode(
      operatingMode: OperatingMode,
      user: User
  ): Either[InvalidUserOperationModeError, ActionSuccess.type] = {
    val userIsValidCombination = userOperationValidation.validateUserAgainstOperatingMode(operatingMode, user)

    if (!userIsValidCombination) {
      user match {
        case adminUser: AdminUser =>
          recordFailedAdminUserAttempt(operatingMode, adminUser)

        case normalUser: NormalUser =>
          recordFailedNormalUserAttempt(operatingMode, normalUser)
      }
    } else {
      Right(ActionSuccess)
    }
  }

  private def recordFailedAdminUserAttempt(operatingMode: OperatingMode, adminUser: AdminUser) = {
    val hasFailedRecordingAudit = auditingService
      .recordAdminUserAttemptedInvalidAction(adminUser, InvalidAdminUserInteraction(operatingMode))
      .isLeft

    Left(
      InvalidAdminUserOperationModeError(
        adminUser,
        operatingMode,
        hasFailedRecordingAudit = hasFailedRecordingAudit
      )
    )
  }

  private def recordFailedNormalUserAttempt(operatingMode: OperatingMode, normalUser: NormalUser) = {
    val hasFailedRecordingAudit = auditingService
      .recordNormalUserAttemptedInvalidAction(normalUser, InvalidNormalUserInteraction(operatingMode))
      .isLeft

    Left(
      InvalidNormalUserOperationModeError(
        normalUser,
        operatingMode,
        hasFailedRecordingAudit = hasFailedRecordingAudit
      )
    )
  }
}
