package refactoring_examples.extract_class.errors

import refactoring_examples.extract_class.dependencies.User.UserId

sealed abstract class SummarizationDataError(message: String) extends RuntimeException(message)

case class InvalidTooManyAccountIdsAccountDetailsError(userId: UserId, accountIds: List[Int])
    extends SummarizationDataError(s"For user $userId there should only be one account id found, received $accountIds")

case class InvalidUserIdsReturnedInAccountDetailsError(userId: UserId, invalidUserIds: List[Int])
    extends SummarizationDataError(
      s"For user $userId only its account details should be returned, received them for the following ids $invalidUserIds"
    )
