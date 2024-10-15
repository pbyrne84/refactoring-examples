package refactoring_examples.extract_class.refactored_components

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import refactoring_examples.extract_class.dependencies.NormalUserTrustLevel.PossibleEnemySpy
import refactoring_examples.extract_class.dependencies.User.UserId
import refactoring_examples.extract_class.dependencies._
import refactoring_examples.extract_class.errors.{
  InvalidTooManyAccountIdsAccountDetailsError,
  InvalidUserIdsReturnedInAccountDetailsError
}

import java.time.{Clock, Instant, Period}

class FinancialDetailsSummarizationSpec
    extends AnyFreeSpecLike
    with TableDrivenPropertyChecks
    with MockFactory
    with Matchers {

  // test is nice and small, keeping things here keeps them easily referable. Code changes hands and this makes it easy
  // to pick up
  private val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]
  private val clock: Clock = mock[Clock]

  private val financialDetailsSummarization = new FinancialDetailsSummarization(financialDetailsService, clock)
  private val possibleSpyUser = NormalUser(UserId(11), PossibleEnemySpy)

  private val daysToSummarize = 33
  private val currentInstant = Instant.EPOCH.plus(Period.ofDays(1000))
  private val startingDate = currentInstant.minus(Period.ofDays(daysToSummarize))

  // Usually I would put private methods nearest first call to follow https://www.baeldung.com/cs/clean-code-formatting>
  // as we read vertically. We should also think about the fact that code is scanned. If you read something from the
  // bottom, do you have to bounce around to get context that could be made clearer.
  // We are going to have different levels of nesting making it harder to share like that
  private def createStatement(recordId: Int, accountId: Int, pence: Int, startOfPeriod: Instant) = {
    FinancialPeriodStatement(
      recordId,
      possibleSpyUser.userId.value,
      accountId,
      Pence(pence),
      // we only care about the start so that is the only varying value
      // variance is communication
      currentInstant,
      FinancialPeriod(start = startOfPeriod, end = currentInstant)
    )
  }

  "calculateCurrentFinancialDetailsSummarization" - {
    "should calculate " - {
      "summarized details for no found entries" in {
        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(Right(List.empty))

        (() => clock.instant)
          .expects()
          .returns(currentInstant)

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe Right(
          SummarizedAccountDetails(possibleSpyUser, startingDate, List.empty)
        )
      }

      "summarized details leaving none as they are all to old" in {
        val daysToSummarize = 33
        val currentInstant = Instant.EPOCH.plus(Period.ofDays(1000))
        val startingDate = currentInstant.minus(Period.ofDays(daysToSummarize))

        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(
            Right(
              List(
                createStatement(1, 1, 22, startingDate.minusMillis(1)),
                createStatement(0, 1, 32, startingDate.minusMillis(2))
              )
            )
          )

        (() => clock.instant)
          .expects()
          .returns(currentInstant)

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe Right(
          SummarizedAccountDetails(possibleSpyUser, startingDate, List.empty)
        )
      }

      "summarized details trimming off the too old ones" in {
        val daysToSummarize = 33
        val currentInstant = Instant.EPOCH.plus(Period.ofDays(1000))
        val startingDate = currentInstant.minus(Period.ofDays(daysToSummarize))
        val newestExpectedSummary = createStatement(3, 1, 42, startingDate.plusMillis(1))
        val expectedSummaryOnBoundary = createStatement(2, 1, 32, startingDate)

        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(
            Right(
              List(
                newestExpectedSummary,
                expectedSummaryOnBoundary,
                createStatement(1, 1, 22, startingDate.minusMillis(1)),
                createStatement(0, 1, 32, startingDate.minusMillis(2))
              )
            )
          )

        (() => clock.instant)
          .expects()
          .returns(currentInstant)

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe Right(
          SummarizedAccountDetails(
            possibleSpyUser,
            startingDate,
            List(newestExpectedSummary, expectedSummaryOnBoundary)
          )
        )
      }
    }

    "should fail" - {
      "when getting the financial details errors returning the error" in {
        val userNotRegisteredOnFinancialSystemError = UserNotRegisteredOnFinancialSystemError(possibleSpyUser)
        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(Left(userNotRegisteredOnFinancialSystemError))

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe
          Left(userNotRegisteredOnFinancialSystemError)
      }

      "when a user id does not match returning those user ids" in {
        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(
            Right(
              List(
                createStatement(2, 1, 22, startingDate.minusMillis(1)).copy(userId = 44),
                createStatement(1, 1, 22, startingDate.minusMillis(1)).copy(userId = 55),
                createStatement(0, 1, 22, startingDate)
              )
            )
          )

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe
          Left(InvalidUserIdsReturnedInAccountDetailsError(possibleSpyUser.userId, List(44, 55)))
      }

      "when the accounts ids mismatch indicating a weird error" in {
        financialDetailsService.getDetails
          .expects(possibleSpyUser)
          .returns(
            Right(
              List(
                createStatement(2, accountId = 10, pence = 22, startOfPeriod = startingDate.minusMillis(1)),
                createStatement(1, accountId = 20, pence = 22, startOfPeriod = startingDate.minusMillis(1))
              )
            )
          )

        financialDetailsSummarization
          .calculateCurrentFinancialDetailsSummarization(possibleSpyUser, daysToSummarize) shouldBe
          Left(InvalidTooManyAccountIdsAccountDetailsError(possibleSpyUser.userId, List(10, 20)))
      }

    }
  }

}
