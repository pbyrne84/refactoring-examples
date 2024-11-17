package refactoring_examples.template_method

import java.time.LocalDate

sealed trait Strategy {

  // What were protected methods are now public, we are not using inheritance any more
  // we have seperated calculation from the other complexity
  def offsetMultiplier: Int

  def calculateStartDate(offset: Int): LocalDate

  def calculateEndDate(offset: Int): LocalDate
}

object FromEpochPlusYearsCalculation {
  def calculate(offset: Int): LocalDate = LocalDate.EPOCH.plusYears(offset)
}

class ChildStrategy1 extends Strategy {
  override def offsetMultiplier: Int = 3

  override def calculateStartDate(offset: Int): LocalDate = FromEpochPlusYearsCalculation.calculate(offset)

  override def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3021-10-31").plusYears(offset)
}

/*
 * We could inherit off of ChildStrategy1, but people will again just copy the pattern to destruction.
 * Inheritance gets unwieldy very quickly. Unwieldy means no-one really knows what is going on. Von Neumann maybe able to work it out,
 * but I doubt it is a challenge he would enjoy. Because we can work something out with effort, does not mean we should have to
 * apply effort.
 */
class SubChildStrategy1 extends Strategy {
  override def offsetMultiplier: Int = 4

  /*
   * Beforehand, this value was achieved using inheritance (SubChildClass1 extended ChildClass1). Inheritance is a heavy relationship, the relationship
   * can also be determined by which came first. With humans, that type of relationship makes sense as a child cannot give birth to their parent,
   * but in code it is not really good design. Shared things can be shared without it being parasitic, inheriting something because you want to
   * co-opt something else is parasitic. We shouldn't teach people to co-opt like this, it becomes the only tool in the toolbox.
   * Hence, why I am doing all of this.
   * ChildStrategy1 and SubChildStrategy1 are now equal in level mental modelling wise
   */
  override def calculateStartDate(offset: Int): LocalDate = FromEpochPlusYearsCalculation.calculate(offset)

  override def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("2500-10-31").plusYears(offset)
}

class ChildStrategy2 extends Strategy {
  override def offsetMultiplier: Int = 2

  override def calculateStartDate(offset: Int): LocalDate = LocalDate.parse("2024-01-18").plusDays(offset)

  override def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3024-03-18").plusDays(offset)
}

// If we wanted to inject DummyLookupService into the constructor
object StrategisedTemplateMethodPattern {
  def createChildStrategy1StrategisedTemplateMethodPattern(
      dummyLookupService: DummyLookupService
  ): StrategisedTemplateMethodPattern =
    new StrategisedTemplateMethodPattern(new ChildStrategy1, dummyLookupService)

  def createSubChildStrategy1StrategisedTemplateMethodPattern(
      dummyLookupService: DummyLookupService
  ): StrategisedTemplateMethodPattern =
    new StrategisedTemplateMethodPattern(new SubChildStrategy1, dummyLookupService)

  def createChildStrategy2StrategisedTemplateMethodPattern(
      dummyLookupService: DummyLookupService
  ): StrategisedTemplateMethodPattern =
    new StrategisedTemplateMethodPattern(new ChildStrategy2, dummyLookupService)
}

class StrategisedTemplateMethodPattern(strategy: Strategy, dummyLookupService: DummyLookupService) {
  def getData: Either[GetDataError, Option[Data]] = {
    val offset = strategy.offsetMultiplier * 2
    val startDate = strategy.calculateStartDate(offset)
    val endDate = strategy.calculateEndDate(offset)

    val exampleDateRange = ExampleDateRange(startDate, endDate)
    dummyLookupService
      .getData(exampleDateRange)
      .left
      .map(error => GetDataError(s"Failed getting the data for $exampleDateRange", error))
  }
}
