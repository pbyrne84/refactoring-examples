package refactoring_examples.extract_class.dependencies

import refactoring_examples.extract_class.dependencies.User.UserId

sealed trait User {
  val userId: UserId
}

object User {
  // opaque types are equivalent to tagged types, one of my favourite concepts in scala. Can use value classes/case classes,
  // but these come with lower overhead for times when you want that
  // type UserId = Int
  // is just a misdirecting way of saying int as it offers no checks. Makes me think we have added stringency.
  // Until I look at the implementation, then I am sad.
  opaque type UserId = Int

  object UserId {
    def apply(int: Int): UserId = int
  }

  extension (userId: UserId) {
    @inline
    def value: Int = userId
  }

}

case class AdminUser(userId: UserId, isSuperAdmin: Boolean) extends User

enum NormalUserTrustLevel {
  case PossibleEnemySpy,
    FriendlyWetWorkOperative,
    NonCombatant

}

case class NormalUser(userId: UserId, trustLevel: NormalUserTrustLevel) extends User

class UserService {

  def getUser(id: Int): Either[Throwable, Option[User]] = ???

}
