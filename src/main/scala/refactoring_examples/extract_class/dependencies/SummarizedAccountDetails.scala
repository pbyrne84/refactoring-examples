package refactoring_examples.extract_class.dependencies

import java.time.Instant

case class SummarizedAccountDetails(
    user: User,
    startingInstant: Instant,
    financialDetails: List[FinancialPeriodStatement]
)
