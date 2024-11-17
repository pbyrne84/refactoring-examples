package refactoring_examples.template_method

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers

import java.time.LocalDate

// Now the logic/boundary conditions can be made clear
// Each thing can grow at their own rate (have extra methods etc.)
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

  "SubChildStrategy1" - {
    val subChildStrategy1 = new SubChildStrategy1

    "should calculate the start date from the epoch using years for the offset" in {
      subChildStrategy1.calculateStartDate(offset = 0) shouldBe LocalDate.EPOCH
      subChildStrategy1.calculateStartDate(offset = -2) shouldBe LocalDate.EPOCH.minusYears(2)
      subChildStrategy1.calculateStartDate(offset = 1) shouldBe LocalDate.EPOCH.plusYears(1)
    }

    "should calculate the end date from 2500-10-31 using years for the offset" in {
      val baseDate = LocalDate.parse("2500-10-31")

      subChildStrategy1.calculateEndDate(offset = 0) shouldBe baseDate
      subChildStrategy1.calculateEndDate(offset = -2) shouldBe baseDate.minusYears(2)
      subChildStrategy1.calculateEndDate(offset = 1) shouldBe baseDate.plusYears(1)
    }

    "should return the offset multiplier" in {
      subChildStrategy1.offsetMultiplier shouldBe 4
    }
  }

  "childStrategy2" - {
    val childStrategy2 = new ChildStrategy2

    "should calculate the start date from 2024-01-18 using days for the offset" in {
      val baseDate = LocalDate.parse("2024-01-18")

      childStrategy2.calculateStartDate(offset = 0) shouldBe baseDate
      childStrategy2.calculateStartDate(offset = -2) shouldBe baseDate.minusDays(2)
      childStrategy2.calculateStartDate(offset = 1) shouldBe baseDate.plusDays(1)
    }

    "should calculate the end date from 3024-03-18 using days for the offset" in {
      val baseDate = LocalDate.parse("3024-03-18")

      childStrategy2.calculateEndDate(offset = 0) shouldBe baseDate
      childStrategy2.calculateEndDate(offset = -2) shouldBe baseDate.minusDays(2)
      childStrategy2.calculateEndDate(offset = 1) shouldBe baseDate.plusDays(1)
    }

    "should return the offset multiplier" in {
      childStrategy2.offsetMultiplier shouldBe 2
    }

  }
}
