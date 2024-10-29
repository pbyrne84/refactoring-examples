package refactoring_examples.template_method

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

class StrategisedTemplateMethodPatternSpec extends AnyFreeSpecLike with Matchers with MockFactory {

  private val strategy: Strategy = mock[Strategy]
  private val dummyLookupService: DummyLookupService = mock[DummyLookupService]
  private val strategizedTemplateMethodPattern = new StrategisedTemplateMethodPattern(strategy, dummyLookupService)

  // now we have no inheritance, we can test via the separate the parts
  // We are also safe from the issues that parts may diverge
  // The tests for the StrategisedTemplateMethodPattern are linear
  "StrategisedTemplateMethodPattern" - {
    "getData" - {
      "should get the data using the range calculated by the strategy when found" in {
        (() => strategy.offsetMultiplier)
          .expects()
          .returns(4)

        // What we care about in our line of site is
        // what is called
        // what is it called with
        // what it returns
        // If we start hiding that then we are actually breaking the tests' ability to communicate with us.
        // We need to be able to look at the test, before any code is changed, and tell what changes we
        // need to do accurately.
        // I tend to lay out mocks like this vertically so the "expects" and "return" always line up so we
        // can read the variance of the values across tests at speed.
        // What variance causes what outcome is the heart of a test.
        // When we hide things, it is like creating a science experiment you cannot really look at, which is frustrating
        // DAMP versus DRY, in tests we do not abstract the duplication away if the duplication is communicative.
        // People just do code first test last when the test becomes a headache as it probably is not a nice kind of self-flagellation
        strategy.calculateStartDate
          .expects(8)
          .returns(LocalDate.EPOCH.plusYears(500))

        strategy.calculateEndDate
          .expects(8)
          .returns(LocalDate.EPOCH.plusYears(1000))

        val data = Data("retrieved data")

        dummyLookupService.getData
          .expects(ExampleDateRange(LocalDate.EPOCH.plusYears(500), LocalDate.EPOCH.plusYears(1000)))
          .returning(Right(Some(data)))

        strategizedTemplateMethodPattern.getData shouldBe Right(Some(data))
      }

      "should return empty using the range calculated by the strategy when none is found" in {
        (() => strategy.offsetMultiplier)
          .expects()
          .returns(2)

        strategy.calculateStartDate
          .expects(4)
          .returns(LocalDate.EPOCH.plusYears(100))

        strategy.calculateEndDate
          .expects(4)
          .returns(LocalDate.EPOCH.plusYears(2000))

        dummyLookupService.getData
          .expects(ExampleDateRange(LocalDate.EPOCH.plusYears(100), LocalDate.EPOCH.plusYears(2000)))
          .returning(Right(None))

        strategizedTemplateMethodPattern.getData shouldBe Right(None)
      }

      "should remap the error" in {
        (() => strategy.offsetMultiplier)
          .expects()
          .returns(3)

        strategy.calculateStartDate
          .expects(6)
          .returns(LocalDate.EPOCH.plusYears(200))

        strategy.calculateEndDate
          .expects(6)
          .returns(LocalDate.EPOCH.plusYears(400))

        val returnedError = new RuntimeException("I had a problem")
        dummyLookupService.getData
          .expects(ExampleDateRange(LocalDate.EPOCH.plusYears(200), LocalDate.EPOCH.plusYears(400)))
          .returning(Left(returnedError))

        val errorOrMaybeData = strategizedTemplateMethodPattern.getData
        // this would be easier to debug on failure with separate assertions, but I am being bad and lazy
        errorOrMaybeData.left.map(error => (error.getClass, error.cause)) shouldBe
          Left((classOf[GetDataError], returnedError))
      }

    }
  }
}
