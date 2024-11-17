# Extracting Out Financial Details Summarization

Anything highly logical is a prime candidate for being extracted. It makes testing very easy, it also gives room
to make the logic clearer. In a busy class, people tend to find less than ideal ways to organize things. For example, they 
may start using complicated closures (method in a method that depend on things from outside the inner method). They sometimes
do aid simplicity, but unfortunately can easily fall apart under "monkey see monkey do". 

Sometimes I open some code, and someone has literally written a class in a method because they don't understand refactoring 
at all, Everyone just added to the existing pattern without thought. This is also the problem with inheritance, it has a very high chance
of mess so should be used very sparingly, including in tests (traits are inheritance). We need to think "What will the next person likely do". 
It is unlikely they will clean it up, that can take skill the next person does not have. It is not an innate skill, though
pattern recognition does come into it, but something that has to be proactively mastered (read, question and be generally curious) 
or is taught by someone.

## Original code 

```scala
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
  }
```
## Code once refactored

[FinancialDetailsSummarization.scala](../../src/main/scala/refactoring_examples/extract_class/refactored_components/FinancialDetailsSummarization.scala)

Once the original code was extracted out, and we start to feel like we have room to work on it, 
we should take into account why things make us feel as our biology communicates to us, we can clean it up 
and make it a lot easier to linearly read/skim. Bugs slip through if we cannot skim. 

```scala
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
```

## Test for refactored code
[FinancialDetailsSummarizationSpec.scala](../../src/test/scala/refactoring_examples/extract_class/refactored_components/FinancialDetailsSummarizationSpec.scala)

Easily readable and linear, all things are very easily referencable. Not complex contexts needed. As soon as a context 
is added ( "should x" in new Test{} ), then we are inviting complexity. It is probably worth dropping them until 
everyone is up to a high level of fundamental software techniques like managing complexity. 

Adding a context in a test could be the equivalent of using a folate instead of an antifolate when trying to cure cancer. We are reacting in the 
wrong way.

```scala
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

```
