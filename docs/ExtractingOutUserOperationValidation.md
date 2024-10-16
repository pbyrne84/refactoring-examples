# Extracting Out User Operation Validation Logic

This has 15 branches, more if you count it recording the invalid action in the **auditingService** as that causes a
boolean flag to be set doubling the branches at that point.

Something like this probably would have started small, each story adding a small bit with people taking the wrong course of action
refactoring the test in a way that complicates the test. Often getting to the point, the test is more complicated than
the code, the inverse of what it should be. Complexity in a test is how easy it is to understand the detail of the actual
operation, we should not feel like trust is a major factor. Trust and automation are a horrendous combination.

```scala
  /** This is an area of high complexity, prime extraction candidacy to its own class It does branch off into private
    * methods to help our monkey brains grasp it. But the cyclomatic complexity is high, and this complexity needs to be
    * taken into account when testing boundaries in calls after this one. Whenever we start thinking about looping in a
    * test to cover boundaries, we should ask why we are doing it, looping in tests is complexity and could be a
    * reaction to poor organization.
   *
   *  This has two logical branches with then split into fifteen success cases and five failure cases between them.
    * @param operatingMode
    * @param user
    * @return
    */
  private def validateUserAgainstOperatingMode(
      operatingMode: OperatingMode,
      user: User
  ): Either[InvalidUserOperationModeError, true] = {
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
   * @param operatingMode
   * @param adminUser
   * @return
   */
  private def validateAdminUserAgainstOperationMode(
      operatingMode: OperatingMode,
      adminUser: AdminUser
  ): Either[InvalidUserOperationModeError, true] = {
    operatingMode match {
      case OperatingMode1 if adminUser.isSuperAdmin =>
        Right(true)
      case OperatingMode2 if !adminUser.isSuperAdmin =>
        Right(true)
      case OperatingMode3 => // 2 success cases, any admin mode
        Right(true)
      case _ =>
        // Usually you would log the error and not just discard it, unless you want to be mean that is.
        val hasFailedRecordingAudit =
          auditingService
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
  }

  /**
   * We have 9 combinations (I think)
   * 6 success cases - trust level has 3 combinations
   * 3 failure cases
   * Counting is fun!
   * @param operatingMode
   * @param normalUser
   * @return
   */
  private def validateNormalUserAgainstOperatingMode(
      operatingMode: OperatingMode,
      normalUser: NormalUser
  ): Either[InvalidNormalUserOperationModeError, true] = {
    operatingMode match {
      case OperatingMode1 if normalUser.trustLevel != PossibleEnemySpy => // 2 success cases
        Right(true)
      case OperatingMode2 if normalUser.trustLevel == FriendlyWetWorkOperative => // 1 success case
        Right(true)
      case OperatingMode3 => // this actually covers 3 cases as any trust level is accepted
        Right(true)
      case _ =>
        // Usually you would log the error and not just discard it, unless you want to be mean that is
        val hasFailedRecordingAudit =
          auditingService
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
```

This invites people to have to do looping needlessly in a complicated test. This leads to people getting very inventive
in not necessarily good ways by writing massive complication in the test to handle it. We need to extract it out,
write its own test, which will be much simpler than the sludge we have in the original test, and then
remove the complication in the original test. If you look at tests first, you can estimate the time you will waste.
Things like recording failures are possibly the responsibility of the caller, etc.

We are left with an easily testable calculation separated from actions that do any side effect. Separating calculation
from side effect makes things easier. Doing this by extracting private methods is not really doing this due to
the call chain is still polluted by the misdirection.


#### The needless added complexity of the test is highlighted by the looping element
This pattern can be very common, but we should question why we are doing this and whether there is a better way to expend effort.


```scala
  private val validUserOperatingModeCombinations = Table(
    ("user returned", "operation"),
    (AdminUser(normalAdminUserId, false), OperatingMode2),
    (AdminUser(superAdminUserId, true), OperatingMode1),
    (AdminUser(normalAdminUserId, false), OperatingMode3),
    (AdminUser(superAdminUserId, true), OperatingMode3),
    // now for the 6 normal user cases
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode1),
    (NormalUser(normalAdminUserId, NonCombatant), OperatingMode1),
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode2),
    (NormalUser(normalAdminUserId, FriendlyWetWorkOperative), OperatingMode3),
    (NormalUser(normalAdminUserId, NonCombatant), OperatingMode3),
    (NormalUser(normalAdminUserId, PossibleEnemySpy), OperatingMode3)
  )
```
etc.

<https://github.com/pbyrne84/refactoring-examples/blob/main/src/test/scala/refactoring_examples/extract_class/PoorSeparationOfConcernsExampleSpec.scala#L62>

Not nice to have to debug a failure with. Especially if running a single test easily in the IDE has been broken.
This is usually a sign the tests are being written last, trying to write tests first in this situation can be a
major headache most people will avoid.

#### How this was dealt with

1. I create an empty new class, now the naming at this point isn't so important to get hung up on it at the start.
   You can rename it later, naming it can be the harder thing to do but can come easier as you get a feel for it.
   Trying to name it can actually trigger analysis paralysis, so we are looking to keep momentum until I brain wakes
   up to the naming task. It can be fully awake for programming logic but not naming.

   It is the reason I think more extract class is not done, naming the class and creating the test seem like the biggest
   challenges. Creating the test is easy though if you know shortcuts.

2. Copy the code to the new class, the ide will complain about any required constructor dependencies. It should look red
   depending on your color scheme. I made mine make every warning very visible, so it can be really overwhelming in
   some code bases. Do not alter the code at this point too much, you can alter it when you have a test that actually
   covers the new code.

3. Inject the new dependency in the current class and delete the old code, update the test to inject the new dependency.
   Existing test should still run. If the test is an obscure test, then we have to rely on faith as we cannot really guarantee
   its correctness. We have a hard time spotting any holes in it as too much is out of sight.

4. Refactor out any dependencies/logic that hinder testing.
   [UserOperationValidation.scala](../src/main/scala/refactoring_examples/extract_class/refactored_components/UserOperationValidation.scala)<br/>
```scala
 // note - making this static blocks dependencies being injected as it grows. Or it has to have the knowledge to instantiate the
 // dependencies. One of the reasons we do dependency injection is to limit the number of things that need to be known to use something.
 // Every time something's dependencies' change, then everything that instantiates it needs to know about the details to deal with that
 // change causing a cascade through the call chain. 
 // Static requires the caller to be able to provide what is needed to instantiate the new dependency. This is one of the issues procedural
 // programming faces. Changes can have much larger effects on the whole call chain as things need to be passed down (or global variables ICK).
 class UserOperationValidation {
 
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
```
5. Create a new test. This can simply be achieved by using the "Go to Test" (CTRL/CMD)+Shift+T. CMD for OSX and CTRL for Windows. This will ask<br/>
   whether you want to create a test. It will create any packages it needs to, as long as you have the scala test root created. Else it will
   create it in the same folder as the code, which you do not want to happen.

6. Write a lovely stress-free test that feels like it gives you room to breathe, the bias of complexity in very firmly on the production code, we can easily see it is correct. 
   We need space to add further complexity later. We can also count on the fact any approach we
   take will be taken to the point of destruction as refactoring seems to be based on how much suffering someone can take before doing it. Headaches
   should not be normal, but we are brought up to work hard at the expense of smart. Smart people cause problems and ask questions that are tiring :)

   The Test can be found at [UserOperationValidationSpec.scala](../src/test/scala/refactoring_examples/extract_class/refactored_components/UserOperationValidationSpec.scala).
   NOTE: There is LIMITED looping, every test can ran and debugged easily. That is a goal, it is about the experience we leave each other, not
   just tick boxing things because we are told to do them.

7. I wrote a wrapper for the recording of failures as that is a separate responsibility, it is hard to know what a responsibility is and people use 
   absolute language like there should be 1 reason a class is changed. Really, I look at what causes change and the likelihood of change. Some logic is fairly immortal compared to other logic,
   for example, whether it is a leap year calculation versus a business requirement.

   The first one we will probably be dead way before the logic changes, seeming immortal.
   The business logic may need to change 5 minutes from now.</br>
   [FailureRecordingUserValidation.scala](../src/main/scala/refactoring_examples/extract_class/refactored_components/FailureRecordingUserValidation.scala)
```scala
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
```
8. The test for this is found at [FailureRecordingUserValidationSpec.scala](../src/test/scala/refactoring_examples/extract_class/refactored_components/FailureRecordingUserValidationSpec.scala)
