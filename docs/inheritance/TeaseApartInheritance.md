# Tease-Apart Inheritance (what you do when you finally realize you are needlessly getting a headache)
Note ADT's may use the term inheritance to bind themselves together, but they are a low complexity and Scala's approach
to inheritance is different than Java's, this is about Java style inheritance which things like Play Framework promote.
Scala uses TypeClasses in place of inheritance in many places (add hoc polymorphism).

It is worth reading [WhenGoodPatternsGoBad.md](WhenGoodPatternsGoBad.md) to give you an overview of when patterns go
bad. The problems are not immediate but slowly sink productivity over time. Every approach we take we should look for
alternates if things start to degenerate. Different approaches have different sensitivities and handle chasing Jira tickets
by those who may not have the experience to deal with internal design changes.

Unfortunately, most people don't really refactor. They don't know about feature envy (https://refactoring.guru/smells/feature-envy)
and how dealing with it radically alters the internal quality of the system. Part of it is likely because
changing "working" code is scary, they become a tall blade of grass by doing it. There is also the part that people are
taught work should be hard so are not keyed into spotting when things are needlessly hard. This field of work has an infinite
appetite for effort, so it is important we learn how to apply effort effectively.

## Why is inheritance a problem?
I am like a stuck record on this. The problem is it is generally the first thing learnt, versus one of the last things.
This means it is the first thing used by people who don't know how to manage it, so things just get worse and worse
productivity wise. That is the measure of whether an approach is good or bad by how it affects team performance and
happiness. Not all people need to feel performant to be happy, but possibly the most performant do need this and will
get frustrated by things first. It comes down to how people's reward systems work. Performant people set their own 
metrics, whether a manager says something is delivered in a performant enough fashion has little value as they also don't 
really have to inhabit the current solution or feel the stress.

New people in the industry are also at risk of being taught a bad form of normal. There are decades of literature on fixing
these types of headaches, but as senior members often haven't really thought about long-term software ownership, much is 
not even googled, let alone read, why it is a problem is not understood. This sinks teams.

### Composition over inheritance
This is what should be taught from the get-go. People have been saying it for decades due to the headaches that come
from using inheritance badly. It is not for shared functionality, shared functionality often has its domain specifics
and is likely best kept separately and tested separately. Testing logic in a parent class is not fun, and often leads
to really nasty tests to work with. For example, a test that has a parent test that uses the child test to fill in 
parts in a way that none of the singular tests can easily be run leading to being hard to work with.

Tests have design technical debt as well as code, the test magnifies the design technical debt of the code. We then
try and clean the test up when the cleanest thing is to clean up the code.

Inheritance is possibly the hardest thing to refactor out of, often using it is akin to crushing walnuts with a kango hammer.
It seems opportunistically easy, until it isn't. People may look to leave a code base at that point. I know I have. I'll clean
it up, but that often needs some group buy-in and people have different tolerances to time-wasting. I don't mind working hard,
but it needs to be effective.

This is a good writeup on it.
https://www.thoughtworks.com/en-gb/insights/blog/composition-vs-inheritance-how-choose

Inheritance is a bit like refined sugar, feels good for a while, okay in very, very small doses. In summation, it leads to
heavy convolution and heavy convolution leads to the poor ability to estimate, which causes stress to the people who 
have to deliver. 

## Example

## Template method pattern 
https://refactoring.guru/design-patterns/template-method

[templateMethodExample.scala](../../src/main/scala/refactoring_examples/template_method/templateMethodExample.scala)

```scala
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
```

Template method pattern is a type of design is probably the first that people naturally gravitate to, design patterns
occur naturally, and frameworks also may dictate them as a way to easily fill in the gaps. It requires low skill to do, but 
is also one directional and high skill to manage. You have to always work through it to understand it, and it can be really
tricky. **SubChildClass1** extends **ChildClass1** overriding some of it, you have to keep 3 things in your head. This is 
a pretty common style of code, and the source of a lot of the problems. Basically, inheritance breeds inheritance.

For example, it bred to the example test. This is again common.
```scala
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
```

This is not a nice test to work in. Dealing with failures sucks as they are not as easy to run. Any deviation between the types like having 
extra methods in ChildClass1 starts making things become heavier.

## Strategy pattern
https://refactoring.guru/design-patterns/strategy

Instead of having a child class fill-in commonality, we can inject the functionality of the child class into parent. We can
then test the "parent" class easily by passing in a mock, and then we can test the different "child" implementations clearly
and easily. If something is clear, it is easier by nature anyway.

### The "parent" class
Now we have teased apart the inheritance we have gone from 
 
```scala
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
```

to

```scala
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
```

We haven't created any more code than before.

#### The "parent" class test

We have a nice simple linear test. 

```scala
class StrategisedTemplateMethodPatternSpec extends AnyFreeSpecLike with Matchers with MockFactory {

  private val strategy: Strategy = mock[Strategy]
  private val dummyLookupService: DummyLookupService = mock[DummyLookupService]
  private val strategizedTemplateMethodPattern = new StrategisedTemplateMethodPattern(strategy, dummyLookupService)
  
  "StrategisedTemplateMethodPattern" - {
    "getData" - {
      "should get the data using the range calculated by the strategy when found" in {
        (() => strategy.offsetMultiplier)
          .expects()
          .returns(4)

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
```
### Refactored child class to strategy

Using inheritance
```scala
class ChildClass1(dummyLookupService: DummyLookupService) extends TemplateMethodExample(dummyLookupService) {

  override protected def offsetMultiplier: Int = 3
  
  override protected def calculateStartDate(offset: Int): LocalDate = LocalDate.EPOCH.plusYears(offset)
  
  override protected def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3021-10-31").plusYears(offset)
}
```

Using the strategy

```scala
class ChildStrategy1 extends Strategy {
  override def offsetMultiplier: Int = 3

  override def calculateStartDate(offset: Int): LocalDate = FromEpochPlusYearsCalculation.calculate(offset)

  override def calculateEndDate(offset: Int): LocalDate = LocalDate.parse("3021-10-31").plusYears(offset)
}

```
Again, no real code size difference.

#### Test for child class
Nice and simple to test in a clearer fashion than when we did inheritance route. Now we can state
the reasons why the values are calculated that way if we wish. In the real world, things like
offsetMultiplier would have real reasons for being that value and tests are the ideal place to state 
that reason in the assertion. The benefit of this rather than comments in the code is that comments 
drift, code changes as people don't update the comments. If there are comments in code, generally
the good ones are based around an exact implementation detail explaining why changing the approach would
cause problems. 

Requirement-based comments are much better in the test. If the rules change, then 
the test will have to be updated and people will notice the assertion. It is really clear to pick up
in pull requests if the tests are clear. One of the major downsides of just doing high level integration
tests is that they poorly communicate these things. It is "Yay it works" or a real headache. They 
are very "happy outcome" minded leading to much unhappiness when things are not a happy outcome.


```scala
class StrategySpec extends AnyFreeSpecLike with Matchers {

  "ChildStrategy1" - {
    val childStrategy1 = new ChildStrategy1

    "should calculate the start date from the epoch using years for the offset" in {
      childStrategy1.calculateStartDate(offset = 0) shouldBe LocalDate.EPOCH
      childStrategy1.calculateStartDate(offset = -2) shouldBe LocalDate.EPOCH.minusYears(2)
      childStrategy1.calculateStartDate(offset = 1) shouldBe LocalDate.EPOCH.plusYears(1)
    }

    "should calculate the end date from 3021-10-31 using years for the offset" in {
      val baseDate = LocalDate.parse("3021-10-31")

      childStrategy1.calculateEndDate(offset = 0) shouldBe baseDate
      childStrategy1.calculateEndDate(offset = -2) shouldBe baseDate.minusYears(2)
      childStrategy1.calculateEndDate(offset = 1) shouldBe baseDate.plusYears(1)
    }

    "should return the offset multiplier" in {
      childStrategy1.offsetMultiplier shouldBe 3
    }
  }
}
```

This clarity is lacking from **TemplateMethodExampleSpec** exampled above. We just see the outcomes, but
there is no real way to communicate the factors involved in the calculation.

#### Using a factory to create the equivalent of child classes.
Admittedly, this adds more code, but if you want to lock things down into structures, then factory methods are ideal.

```scala
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
```
