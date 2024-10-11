package refactoring_examples.extract_class.dependencies

import cats.data.NonEmptyList

import java.time.Instant

opaque type Pence = Int
object Pence {
  def apply(pence: Int): Pence = pence

  extension (pence: Pence) {
    @inline
    def value: Int = pence
  }
}

case class FinancialPeriod(start: Instant, end: Instant)
case class FinancialPeriodStatement(
    recordId: Int,
    userId: Int,
    accountId: Int,
    totalInBankAtTime: Pence,
    createdInstant: Instant,
    period: FinancialPeriod
)

sealed abstract class FinancialSystemsError(message: String) extends RuntimeException(message)

case class UserNotRegisteredOnFinancialSystemError(user: User)
    extends FinancialSystemsError(s"User ${user.userId} was not found on the financial system")

class FinancialDetailsService {

  /**
   * We could do Option[List] to denote a user not registered. But we are going to say if something is calling this
   * and it not being there, then there is a major data issue. You can tell I am making stuff up.
   * @param userId
   * @return
   */
  def getDetails(userId: User): Either[FinancialSystemsError, List[FinancialPeriodStatement]] = ???
}
