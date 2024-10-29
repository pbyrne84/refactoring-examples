package refactoring_examples.template_method

import java.time.LocalDate

case class ExampleDateRange(startDate: LocalDate, endDate: LocalDate)

/**
 * Template method pattern is possibly one of the most naturally occurring patterns
 * This does not mean it is good, it lays down the pattern that inheritance is the solution
 * to all further changes.
 *
 * One of the harder things to do is a tease-apart inheritance refactoring. There is a hard skill barrier
 * to do it. It relies on the ability of someone to visually model what is happening and what it needs to turn into.
 * That is not a skill in high abundance, meaning the implementation will get worse and worse.
 *
 * Someone in the parent class expects the child to fill in, and you run it by calling the method in the parent.
 * Most examples look quite cute, but people tend to do things like create child classes that inherit off the child class
 * then override a bit of the parent child class. Trying to work that out gets impossible.
 *
 * You can end up with many children as well. They can look similar to ADT's, but they are used for interchangeability
 * versus using matching for some decision. ADT's are about communicating symmetry. Where the template method pattern
 * is really using inheritance for functionality
 *
 * https://refactoring.guru/design-patterns/template-method
 * Strategy is the safer, far less destructive option
 * https://refactoring.guru/design-patterns/strategy
 * You inject difference, instead of inheriting it. Parents calling children are just really hard to debug.
 * And it can go "parent child, parent child". Ctrl/CMD clicking an abstract method breaks easy navigation
 */

case class Data(text: String)

trait DummyLookupService {
  def getData(exampleDateRange: ExampleDateRange): Either[Throwable, Option[Data]]
}

case class GetDataError(message: String, cause: Throwable) extends RuntimeException(message, cause)

sealed abstract class TemplateMethodExample(dummyLookupService: DummyLookupService) {

  def getData: Either[GetDataError, Option[Data]] = {
    val offset = offsetMultiplier * 2
    val startDate = calculateStartDate(offset)
    val endDate = calculateEndDate(offset)

    val exampleDateRange = ExampleDateRange(startDate, endDate)
    dummyLookupService
      .getData(exampleDateRange)
      .left
      .map(error => GetDataError(s"Failed getting the data for $exampleDateRange", error))
  }

  protected def offsetMultiplier: Int
  protected def calculateStartDate(offset: Int): LocalDate
  protected def calculateEndDate(offset: Int): LocalDate
}

class ChildClass1(dummyLookupService: DummyLookupService) extends TemplateMethodExample(dummyLookupService) {

  override protected def offsetMultiplier: Int = 3
  override protected def calculateStartDate(offset: Int): LocalDate = LocalDate.EPOCH.plusYears(offset)
  override protected def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3021-10-31").plusYears(offset)
}

// Someone sees ChildClass1, thinks DRY and looks to just override bits of that.
// It makes debugging really hard, as it involves 3 classes, and the classes can get very large as well
// Intellij can create UML diagrams, and looking at the diagrams can be mind mending.
// You set Template Method Pattern as a way of doing something, you will likely end up with a fur ball
// of logic
class SubChildClass1(dummyLookupService: DummyLookupService) extends ChildClass1(dummyLookupService) {
  override protected def offsetMultiplier: Int = 4
  override protected def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("2500-10-31").plusYears(offset)
}

class ChildClass2(dummyLookupService: DummyLookupService) extends TemplateMethodExample(dummyLookupService) {
  override protected def offsetMultiplier: Int = 2

  // using plusDays here as I am trying to communicate chaotic complexity. We cannot predict what we will be expected to do
  override protected def calculateStartDate(offset: Int): LocalDate = LocalDate.parse("2024-01-18").plusDays(offset)
  override protected def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3024-03-18").plusDays(offset)
}
