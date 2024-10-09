package refactoring_examples.extract_class.refactored_components

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.{
  FriendlyWetWorkOperative,
  NonCombatant,
  PossibleEnemySpy
}
import refactoring_examples.extract_class.dependencies.OperatingMode.{OperatingMode1, OperatingMode2, OperatingMode3}
import refactoring_examples.extract_class.dependencies.User.UserId
import refactoring_examples.extract_class.dependencies.{AdminUser, NormalUser, NormalUserTrustLevel}

class UserOperationValidationSpec
    extends AnyFreeSpecLike
    with TableDrivenPropertyChecks
    with MockFactory
    with Matchers {

  // This test looks big, but it is linear and reads like a script you could rewrite the code from
  // tests communicate to us the business goals (think comments that self validate).
  // So they need to read human versus having to decipher machine complexity, the code is machine complexity.
  // It is really important as projects change hands and this is the communication to the next person
  "validateUserAgainstOperatingMode" - {
    val userOperationValidation = new UserOperationValidation
    val adminUser = AdminUser(UserId(11), false)
    val superAdminUser = AdminUser(UserId(11), true)

    "when validating an admin user" - {
      "should pass when it is a normal admin and the operating mode is OperatingMode2" in {
        userOperationValidation.validateUserAgainstOperatingMode(OperatingMode2, adminUser) shouldBe true
      }

      "should fail when it is a super admin and the operating mode is OperatingMode2" in {
        userOperationValidation.validateUserAgainstOperatingMode(OperatingMode2, superAdminUser) shouldBe false
      }

      "should pass when it is a super admin and the operating mode is OperatingMode1" in {
        userOperationValidation.validateUserAgainstOperatingMode(OperatingMode1, superAdminUser) shouldBe true
      }

      "should fail when it is a normal admin and the operating mode is OperatingMode1" in {
        userOperationValidation.validateUserAgainstOperatingMode(OperatingMode1, adminUser) shouldBe false
      }

      "should pass for any type of admin on OperatingMode3" - {
        "normal admin" in {
          userOperationValidation.validateUserAgainstOperatingMode(OperatingMode3, adminUser) shouldBe true
        }

        "super admin" in {
          userOperationValidation.validateUserAgainstOperatingMode(OperatingMode3, superAdminUser) shouldBe true
        }
      }
    }

    "when validating a normal user" - {
      val possibleSpyUser = NormalUser(UserId(11), PossibleEnemySpy)
      val nonCombatantUser = NormalUser(UserId(11), NonCombatant)
      val friendlyWetWorkOperativeUser = NormalUser(UserId(11), FriendlyWetWorkOperative)

      "should fail" - {
        "when we are OperatingMode1 and the the person is a PossibleEnemySpy" in {
          userOperationValidation.validateUserAgainstOperatingMode(OperatingMode1, possibleSpyUser) shouldBe false
        }

        "when we are OperatingMode2 and the the person is a PossibleEnemySpy" in {
          userOperationValidation.validateUserAgainstOperatingMode(OperatingMode2, possibleSpyUser) shouldBe false
        }

        "when we are OperatingMode2 and the the person is a NonCombatant" in {
          userOperationValidation.validateUserAgainstOperatingMode(OperatingMode2, nonCombatantUser) shouldBe false
        }
      }

      "should pass" - {

        // two cases
        "when it is OperatingMode1 and the the user is not a PossibleEnemySpy" in {
          // Tables add complication when failing, so I would try and not use any looping with any complicated
          // In this case we actually want to guarantee that if any are added to the enum, this test still covers it.
          // It is not done for less typing/DRY, but safety reasons.
          // Using a dynamic in will break the ability to run that test easily, which I don't think is a very social thing.
          // It can add a lot of needless stress if you have low tolerance for faffing about. It can make stories much more
          // of a time sink which really adds to the pressure.

          val nonPossibleEnemySpyOptions: TableFor1[NormalUserTrustLevel] = Table(
            "normal user trust level",
            NormalUserTrustLevel.values.filterNot(
              _ == PossibleEnemySpy
            )* // * extracts it from a list, similar to extracting into a vararg
          )

          forAll(nonPossibleEnemySpyOptions) { nonPossibleEnemySpyOption =>
            userOperationValidation.validateUserAgainstOperatingMode(
              OperatingMode1,
              NormalUser(UserId(11), nonPossibleEnemySpyOption)
            ) shouldBe true
          }
        }

        "when it is OperatingMode2 and the the user is a FriendlyWetWorkOperative" in {
          userOperationValidation.validateUserAgainstOperatingMode(
            OperatingMode2,
            friendlyWetWorkOperativeUser
          ) shouldBe true
        }

        // 3 cases
        "when it is OperatingMode3 and any user level is allowed" in {
          // Using withClue can be nicer than table-driven property checks as it doesn't mess with catching and hiding exceptions
          // Again we are using looping because we want to guarantee all the options are always covered, it is about safety.
          NormalUserTrustLevel.values.foreach { userTrustLevel =>
            withClue(s"User trust level $userTrustLevel \n\n") {
              userOperationValidation.validateUserAgainstOperatingMode(
                OperatingMode3,
                NormalUser(UserId(11), userTrustLevel)
              ) shouldBe true
            }
          }
        }

        "should fail" - {
          "when it is OperatingMode1 and the the user is a PossibleEnemySpy" in {
            userOperationValidation.validateUserAgainstOperatingMode(
              OperatingMode1,
              possibleSpyUser
            ) shouldBe false
          }

          // 2 cases
          "when it is OperatingMode2 and the the user is not a FriendlyWetWorkOperative" in {
            val nonPossibleEnemySpyOptions: TableFor1[NormalUserTrustLevel] = Table(
              "normal user trust level",
              NormalUserTrustLevel.values.filterNot(
                _ == FriendlyWetWorkOperative
              )* // * extracts it from a list, similar to extracting into a vararg
            )

            forAll(nonPossibleEnemySpyOptions) { nonFriendlyWetWorkOperativeOption =>
              userOperationValidation.validateUserAgainstOperatingMode(
                OperatingMode2,
                NormalUser(UserId(11), nonFriendlyWetWorkOperativeOption)
              ) shouldBe false
            }
          }
        }
      }
    }

  }
}
