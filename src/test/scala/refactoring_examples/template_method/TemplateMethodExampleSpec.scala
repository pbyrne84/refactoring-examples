package refactoring_examples.template_method

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

// As we are testing inheritance, quite often people write the test using inheritance
// I have done this. It was really not fun to debug.
// You could use a context, but it is the same level of misdirection. Just a different way to do bad things.
// Tests are about communication, not looks, unless the looks aid communication :)
//
// test cases in the parent method also means you cannot run them one by one easily on failure :P
// I am going to add all the tests in one file, you can imagine the fun across multiple files
abstract class TemplateMethodExampleSpec extends AnyFreeSpecLike with Matchers with MockFactory {

  protected val dummyLookupService: DummyLookupService

  "getData" - {

    // The actual meaning of these test cases is obscured by the fact we can't actually communicate the specifics of
    // each child thing
    "should remap an error" in {
      val runtimeException = new RuntimeException("I had a problem")

      dummyLookupService.getData
        .expects(getExpectedDateRange)
        .returning(Left(runtimeException))

      val templateMethodExample = getInstance

      val errorOrData = templateMethodExample.getData
      errorOrData.left.map(_.getClass) shouldBe Left(classOf[GetDataError])
      errorOrData.left.map(_.cause) shouldBe Left(runtimeException)
    }

    "should handle no data found" in {
      dummyLookupService.getData
        .expects(getExpectedDateRange)
        .returning(Right(None))

      val templateMethodExample = getInstance
      templateMethodExample.getData shouldBe Right(None)
    }

    "should return found data" in {
      val someData = Some(Data("data"))
      dummyLookupService.getData
        .expects(getExpectedDateRange)
        .returning(Right(someData))

      val templateMethodExample = getInstance
      templateMethodExample.getData shouldBe Right(someData)
    }

  }

  def getInstance: TemplateMethodExample
  def getExpectedDateRange: ExampleDateRange
}

//Run here, fail at a distance. Why putting tests in contexts is also bad. The failure will just be in the context :(
// I am also going to cheat as these tests are no fun to write, meaning I will just bung in the values ScalaMock complains about.
// ScalaMock driven development
class ChildClass1Spec extends TemplateMethodExampleSpec {
  override protected val dummyLookupService: DummyLookupService = mock[DummyLookupService]

  override def getInstance: ChildClass1 = new ChildClass1(dummyLookupService)

  override def getExpectedDateRange: ExampleDateRange =
    ExampleDateRange(LocalDate.parse("1976-01-01"), LocalDate.parse("3027-10-31"))
}

class SubChildClass1Spec extends TemplateMethodExampleSpec {
  override protected val dummyLookupService: DummyLookupService = mock[DummyLookupService]

  override def getInstance: SubChildClass1 = new SubChildClass1(dummyLookupService)

  override def getExpectedDateRange: ExampleDateRange =
    ExampleDateRange(LocalDate.parse("1978-01-01"), LocalDate.parse("2508-10-31"))
}

class ChildClass2Spec extends TemplateMethodExampleSpec {
  override protected val dummyLookupService: DummyLookupService = mock[DummyLookupService]

  override def getInstance: ChildClass2 = new ChildClass2(dummyLookupService)

  override def getExpectedDateRange: ExampleDateRange =
    ExampleDateRange(LocalDate.parse("2024-01-22"), LocalDate.parse("3024-03-22"))
}
