package refactoring_examples.extract_class.errors

import cats.kernel.instances.BooleanEnumerable
import refactoring_examples.extract_class.dependencies.{AdminUser, NormalUser, OperatingMode}

sealed abstract class InvalidUserOperationModeError(message: String) extends RuntimeException(message) {
  val hasFailedRecordingAudit: Boolean
}

case class InvalidNormalUserOperationModeError(
    user: NormalUser,
    operatingMode: OperatingMode,
    hasFailedRecordingAudit: Boolean
) extends InvalidUserOperationModeError(
      s"normal user ${user.userId} was not valid for mode $operatingMode with trust level of ${user.trustLevel}"
    )

case class InvalidAdminUserOperationModeError(
    user: AdminUser,
    operatingMode: OperatingMode,
    hasFailedRecordingAudit: Boolean
) extends InvalidUserOperationModeError(s"admin user ${user.userId} was not valid for mode $operatingMode")
