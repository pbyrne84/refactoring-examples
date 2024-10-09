package refactoring_examples.extract_class.refactored_components

import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.{FriendlyWetWorkOperative, PossibleEnemySpy}
import refactoring_examples.extract_class.dependencies.OperatingMode.{OperatingMode1, OperatingMode2, OperatingMode3}
import refactoring_examples.extract_class.dependencies.{AdminUser, NormalUser, OperatingMode, User}

class UserOperationValidation {

  /**
   * We have moved the auditing out and left this as pure calculation as it simplifies things a lot
   * The test in the caller now only cares whether true or false are returned. We have gone from 15 combinations to 2
   * in that test.
   *
   * @param operatingMode
   * @param user
   * @return
   */
  def validateUserAgainstOperatingMode(
      operatingMode: OperatingMode,
      user: User
  ): Boolean = {
    user match {
      case adminUser: AdminUser =>
        validateAdminUserAgainstOperationMode(operatingMode, adminUser)

      case normalUser: NormalUser =>
        validateNormalUserAgainstOperatingMode(operatingMode, normalUser)
    }
  }

  /**
   * We have 6 combinations
   * 4 success cases (2 for OperatingMode3 as any type of admin is allowed)
   * 2 failure cases
   * So five test cases
   *
   * @param operatingMode
   * @param adminUser
   * @return
   */
  private def validateAdminUserAgainstOperationMode(
      operatingMode: OperatingMode,
      adminUser: AdminUser
  ): Boolean = {
    operatingMode match {
      case OperatingMode1 if adminUser.isSuperAdmin =>
        true
      case OperatingMode2 if !adminUser.isSuperAdmin =>
        true
      case OperatingMode3 => // 2 success cases, any admin mode
        true
      case _ =>
        false
    }
  }

  /**
   * We have 9 combinations (I think)
   * 6 success cases - trust level has 3 combinations
   * 3 failure cases
   * Counting is fun!
   *
   * @param operatingMode
   * @param normalUser
   * @return
   */
  private def validateNormalUserAgainstOperatingMode(
      operatingMode: OperatingMode,
      normalUser: NormalUser
  ): Boolean = {
    operatingMode match {
      case OperatingMode1 if normalUser.trustLevel != PossibleEnemySpy => // 2 success cases
        true
      case OperatingMode2 if normalUser.trustLevel == FriendlyWetWorkOperative => // 1 success case
        true
      case OperatingMode3 => // this actually covers 3 cases as any trust level is accepted
        true
      case _ =>
        false
    }
  }
}
