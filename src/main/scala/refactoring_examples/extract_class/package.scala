package refactoring_examples

package object extract_class

//I don't really like returning unit, it also can unflattened side affecting futures.
// Future[Unit] hides Future[Future[Int]] and it will run in the background, potentially finishing after the call
// responds hiding its failure.
case object ActionSuccess

opaque type SummarizationCount = Int
object SummarizationCount {
  def apply(count: Int): SummarizationCount = count
  extension (summarizationCount: SummarizationCount) {
    def value: Int = summarizationCount
  }
}
