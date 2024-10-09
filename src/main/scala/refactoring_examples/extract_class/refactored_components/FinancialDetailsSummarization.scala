package refactoring_examples.extract_class.refactored_components

import refactoring_examples.ActionSuccess
import refactoring_examples.extract_class.dependencies.{
  FinancialDetailsService,
  FinancialPeriodStatement,
  SummarizedAccountDetails,
  User
}
import refactoring_examples.extract_class.errors.{
  InvalidTooManyAccountIdsAccountDetailsError,
  InvalidUserIdsReturnedInAccountDetailsError,
  SummarizationDataError
}

import java.time.{Clock, Instant, Period}

/**
 * Extracting this out simplifies things as we have limited failure paths and misdirection so we can
 * concentrate on the looping logical elements clearer
 *
 * @param financialDetailsService
 * @param clock
 */
class FinancialDetailsSummarization(financialDetailsService: FinancialDetailsService, clock: Clock) {

  def calculateCurrentFinancialDetailsSummarization(
      user: User,
      maxDaysToProcess: Int
  ): Either[Exception, SummarizedAccountDetails] = {
    // instead of something using a closure obscuring what is actually happening, we can split it into a linear set of operations
    // for easy reading
    for {
      statements <- financialDetailsService.getDetails(user)
      _ <- validateFinancialDetails(user, statements)
      results <- filterResultsToWithinPeriod(user, statements, maxDaysToProcess)
    } yield results
  }

  private def validateFinancialDetails(
      user: User,
      financialDetails: List[FinancialPeriodStatement]
  ): Either[SummarizationDataError, ActionSuccess.type] = {
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
      Right(ActionSuccess)
    }
  }

  private def filterResultsToWithinPeriod(
      user: User,
      financialDetails: List[FinancialPeriodStatement],
      maxDaysToProcess: Int
  ) = {
    val now = Instant.now(clock)
    val startDateOfDesiredRecords = now.minus(Period.ofDays(maxDaysToProcess))

    val dateFilteredFinancialRecords: List[FinancialPeriodStatement] =
      financialDetails.filterNot(_.period.start.isBefore(startDateOfDesiredRecords))

    Right(
      SummarizedAccountDetails(user, startDateOfDesiredRecords, dateFilteredFinancialRecords)
    )
  }
}
