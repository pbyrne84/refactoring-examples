package refactoring_examples.extract_class.dependencies

class SummarizationReportingService {

  def sendSummarization(summarizedAccountDetails: SummarizedAccountDetails): Either[Throwable, true] = ???
}
