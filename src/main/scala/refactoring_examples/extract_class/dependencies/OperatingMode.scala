package refactoring_examples.extract_class.dependencies

// We are using this to simulate branching triggers, usually a flag value in a payload, making stuff up is hard :)
// We just want to show how cyclomatic complexity increases test complexity by acting as a multiplier.
// One line of code may start off at 2 lines of test code, but this can up at 10 lines or test code etc.
// We should monitor this value as it is a key factor in maintaining velocity.
//sealed trait OperatingMode

enum OperatingMode {
  case OperatingMode1,
    OperatingMode2,
    OperatingMode3
}
