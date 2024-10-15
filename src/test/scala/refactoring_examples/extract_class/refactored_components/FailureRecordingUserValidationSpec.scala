package refactoring_examples.extract_class.refactored_components

import org.scalactic.source.Position
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import refactoring_examples.ActionSuccess
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.NonCombatant
import refactoring_examples.extract_class.dependencies.OperatingMode.{OperatingMode1, OperatingMode2, OperatingMode3}
import refactoring_examples.extract_class.dependencies.User.UserId
import refactoring_examples.extract_class.dependencies.*
import refactoring_examples.extract_class.errors.{
  InvalidAdminUserOperationModeError,
  InvalidNormalUserOperationModeError,
  InvalidUserOperationModeError
}

class FailureRecordingUserValidationSpec
    extends AnyFreeSpecLike
    with TableDrivenPropertyChecks
    with MockFactory
    with Matchers {

  private val userOperationModeValidation = mock[UserOperationValidation]
  private val auditingService: AuditingService & reflect.Selectable = mock[AuditingService]

  private val failureRecordingUserValidation =
    new FailureRecordingUserValidation(userOperationModeValidation, auditingService)

  private val adminUser: AdminUser =
    AdminUser(UserId(233), true) // the values of the internals are not used for either in the implementation

  private val normalUser: NormalUser = NormalUser(UserId(233), NonCombatant)

  "validateUserAgainstOperatingMode" - {
    "should pass for an an admin user not recording the attempt" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, adminUser)
        .returns(true)

      failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, adminUser) shouldBe Right(
        ActionSuccess
      )
    }

    "should pass for an a normal user not recording the attempt" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, normalUser)
        .returns(true)

      failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, normalUser) shouldBe Right(
        ActionSuccess
      )
    }

    // As InvalidAdminUserOperationModeError extends throwable we cannot compare easily due to the equals implementation, fun
    "should fail for an admin user returning that it had failed in the error response" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, adminUser)
        .returns(false)

      auditingService.recordAdminUserAttemptedInvalidAction
        .expects(adminUser, InvalidAdminUserInteraction(OperatingMode1))
        .returns(Left(new RuntimeException("failed recording")))

      val errorOrSuccess: Either[InvalidUserOperationModeError, ActionSuccess.type] =
        failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, adminUser)

      assertAdminErrorExpectation(errorOrSuccess, true, adminUser, OperatingMode1)
    }

    // Everything is kept close and below the first time it called to aid linear reading.
    def assertAdminErrorExpectation(
        errorOrSuccess: Either[InvalidUserOperationModeError, ActionSuccess.type],
        expectedAuditingFailure: Boolean,
        expectedAdminUser: AdminUser,
        operatingMode: OperatingMode
    )(implicit position: Position) = {
      errorOrSuccess match {
        case Left(adminError: InvalidAdminUserOperationModeError) =>
          adminError.user shouldBe expectedAdminUser
          adminError.operatingMode shouldBe operatingMode
          adminError.hasFailedRecordingAudit shouldBe expectedAuditingFailure

        case _ =>
          // this will actually show the unexpected success clearly in the error
          errorOrSuccess.left.map(_.getClass) shouldBe Left(a[InvalidUserOperationModeError])
      }
    }

    "should fail for an admin user returning that it had NOT failed in the error response" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, adminUser)
        .returns(false)

      auditingService.recordAdminUserAttemptedInvalidAction
        .expects(adminUser, InvalidAdminUserInteraction(OperatingMode1))
        .returns(Right(ActionSuccess))

      val errorOrSuccess: Either[InvalidUserOperationModeError, ActionSuccess.type] =
        failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, adminUser)

      assertAdminErrorExpectation(errorOrSuccess, false, adminUser, OperatingMode1)
    }

    "should fail for a normal user returning that it had failed in the error response" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, normalUser)
        .returns(false)

      auditingService.recordNormalUserAttemptedInvalidAction
        .expects(normalUser, InvalidNormalUserInteraction(OperatingMode1))
        .returns(Left(new RuntimeException("failed recording")))

      val errorOrBoolean: Either[InvalidUserOperationModeError, ActionSuccess.type] =
        failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, normalUser)

      assertNormalUserErrorExpectation(errorOrBoolean, true, normalUser, OperatingMode1)
    }

    // Everything is kept close and below the first time it called to aid linear reading.
    def assertNormalUserErrorExpectation(
        errorOrSuccess: Either[InvalidUserOperationModeError, ActionSuccess.type],
        expectedAuditingFailure: Boolean,
        expectedAdminUser: NormalUser,
        operatingMode: OperatingMode
    )(implicit position: Position) = {
      errorOrSuccess match {
        case Left(adminError: InvalidNormalUserOperationModeError) =>
          adminError.user shouldBe expectedAdminUser
          adminError.operatingMode shouldBe operatingMode
          adminError.hasFailedRecordingAudit shouldBe expectedAuditingFailure

        case _ =>
          // this will actually show the unexpected success clearly in the error
          errorOrSuccess.left.map(_.getClass) shouldBe Left(a[InvalidNormalUserOperationModeError])
      }
    }

    "should fail for a normal user returning that it has not failed in the error response" in {
      userOperationModeValidation.validateUserAgainstOperatingMode
        .expects(OperatingMode1, normalUser)
        .returns(false)

      auditingService.recordNormalUserAttemptedInvalidAction
        .expects(normalUser, InvalidNormalUserInteraction(OperatingMode1))
        .returns(Right(ActionSuccess))

      val errorOrSuccess: Either[InvalidUserOperationModeError, ActionSuccess.type] =
        failureRecordingUserValidation.validateUserAgainstOperatingMode(OperatingMode1, normalUser)

      assertNormalUserErrorExpectation(errorOrSuccess, false, normalUser, OperatingMode1)
    }

  }

}
