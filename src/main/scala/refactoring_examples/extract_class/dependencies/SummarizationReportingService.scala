package refactoring_examples.extract_class.dependencies

import refactoring_examples.ActionSuccess

class SummarizationReportingService {

  def sendSummarization(summarizedAccountDetails: SummarizedAccountDetails): Either[Throwable, ActionSuccess.type] = ???
}
