package refactoring_examples.extract_class

import refactoring_examples.extract_class.dependencies.*
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.{FriendlyWetWorkOperative, PossibleEnemySpy}
import refactoring_examples.extract_class.dependencies.OperatingMode.*
import refactoring_examples.extract_class.errors.*

import java.time.{Clock, Instant, Period}

//The number of constructor params should be a warning if there is conditional logic
class PoorSeparationOfConcernsExample(
    clock: Clock, // adding time is always fun, controlling time in a test is controlling the universe
    userService: UserService,
    auditingService: AuditingService,
    financialDetailsService: FinancialDetailsService,
    summarizationReportingService: SummarizationReportingService
) {
  def sendSummarization(
      userId: Int,
      operatingMode: OperatingMode,
      maxDaysToProcess: Int
  ): Either[ExampleError, SummarizedAccountDetails] = {

    for {
      maybeUser <- userService
        .getUser(userId)
        .left
        .map(error => ServiceFailedInAttemptingUserRetrievalError(userId, error))
      // getUser should not fail when it does not find one, that is up to the caller to handle on a case by case basis
      user <- maybeUser.map(Right.apply).getOrElse(Left(ExpectedUserNotFound(userId)))
      _ <- validateUserAgainstOperatingMode(operatingMode, user).left.map((error: InvalidUserOperationModeError) =>
        FailedValidatingUserWithOperatingMode(user, operatingMode, error)
      )
      summarisedAccountDetails <- getCurrentFinancialDetailsSummarization(user, maxDaysToProcess).left.map(error =>
        FailedSummarisingFinancialData(user, error)
      )
      _ <- sendPossibleSummarizedAccountDetails(user, summarisedAccountDetails).left.map(error =>
        FailedSendingSummarisedFinancialData(user, error)
      )
    } yield summarisedAccountDetails
  }

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

  /** This is an area of high complexity, prime extraction candidacy to its own class
    * @param user
    * @param maxDaysToProcess
    * @return
    */
  private def getCurrentFinancialDetailsSummarization(
      user: User,
      maxDaysToProcess: Int
  ): Either[Exception, SummarizedAccountDetails] = {
    // Personally, I would put these in a private method underneath a lot of the time.
    // Unfortunately, as a class gets more and more busy, people start doing things that are less than
    // ideal, as they are mentally fighting for space with the other code.
    // One is okay, but when they start referring to things outside their scope (being a closure), it gets very confusing
    // as it really becomes a disguised class with all the complexities of a class in method.
    // We also have the risk of having variables with the same name in scope, not nice as gets confusing.
    // We don't necessarily think very verbally when we do stuff, so awareness of reader difficulty can be problematic.
    //
    // A cleaner way to do something that is getting complex is to create a curried private def
    // This uses the params, bit it gets harder and harder to spot.
    // so
    // private def summariseFinancialDetails(user: User, maxDaysToProcess: Int)(financialDetails: List[FinancialDetails]): Either[Throwable, Any]
    //
    // would isolate its exact needs. I am trying to be dirty now.
    // Think of a curried function as a single method class, with the first param set the constructor.
    // If we did not move it outside the scope, it doesn't achieve as much by mental naming conflicts, etc.
    def summariseFinancialStatements(
        financialDetails: List[FinancialPeriodStatement]
    ): Either[SummarizationDataError, SummarizedAccountDetails] = {
      val uniqueAccountIds = financialDetails.map(_.accountId).distinct
      val accountIdUniqueCount = uniqueAccountIds.size
      val userIdsThatDoNotMatchCurrent = financialDetails.map(_.userId).filter(_ != user.userId.value)

      if (userIdsThatDoNotMatchCurrent.nonEmpty) {
        Left(
          InvalidUserIdsReturnedInAccountDetailsError(
            userId = user.userId,
            invalidUserIds = userIdsThatDoNotMatchCurrent
          )
        )
      } else if (accountIdUniqueCount > 1) {
        Left(InvalidTooManyAccountIdsAccountDetailsError(userId = user.userId, accountIds = uniqueAccountIds))
      } else {
        val now = Instant.now(clock)
        val startDateOfDesiredRecords = now.minus(Period.ofDays(maxDaysToProcess))

        val dateFilteredFinancialRecords: List[FinancialPeriodStatement] =
          financialDetails.filterNot(_.period.start.isBefore(startDateOfDesiredRecords))

        Right(
          SummarizedAccountDetails(user, startDateOfDesiredRecords, dateFilteredFinancialRecords)
        )
      }

    }

    // We should actually audit the failure, but the test for this is getting very tiring. Code like this
    // makes writing the test a chore, when really it should be allowing us to go consistently at a good pace producing
    // an ideal output. The more effort something is, the more people will find excuses not to do it or just deem
    // it unimportant. This can actually be for important things like logging/telemetry that allow us to support it
    // easily in production. Penny wise, pound foolish.
    val errorOrStatements = financialDetailsService.getDetails(user)
    errorOrStatements.flatMap(statements => summariseFinancialStatements(statements))

  }

  private def sendPossibleSummarizedAccountDetails(
      user: User,
      summarisedAccountDetails: SummarizedAccountDetails
  ): Either[Throwable, SummarizedAccountDetails] = {

    summarizationReportingService
      .sendSummarization(summarisedAccountDetails)
      .flatMap { _ =>
        auditingService.recordSuccessfulSummarization(user.userId.value, summarisedAccountDetails.startingInstant)
      }
      .map(_ => summarisedAccountDetails)

  }
}
