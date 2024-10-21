# Refactoring Examples

Tests are a cost, but they should be helping us go fast. If they are not, we are probably in a cargo cult.
A cargo cult is when people copy something without understanding the nuance, so it loses effectiveness. 
Testing from a developer's perspective often follows a very similar pattern to the old American Fire Marshal, people
effectively "hand me down" sometimes poor understanding to each other. This caused the pseudo-science of "pour patterns" being
very harmfully spread.<br/>
https://www.lb7.uscourts.gov/documents/13c6098.pdf

Of course, this is not a good way to do things as relying on direct experience means we are just repeating 
the same problems over and over again, wasting each other's time.

## The purpose of writing tests first

### We view things top down
We are thinking about contracts as we are thinking about the entrance point we are calling. We should not be thinking
in method calls but in agreements. Designing an application interface is designing an agreement. Clear agreements make 
life easy. We should not have to look at the source code to use something. Whenever we are forced to look at an implementation, 
it distracts. Good design, including the tests themselves, should all feel fluid to use. The unfortunate fact is that as
design rot comes in over a period of time, we don't notice how much time we end up wasting. 

People will leave if they feel like they are wasting too much time and effort.

### We start with a failure
Starting with a failure allows us to experience how it feels when the test fails. How much we have to needlessly 
fight the test when we create the solution. We should not feel like we are fighting the test, it is there to reduce
stress and help us conserve energy. Conversed energy can be saved for more fun things.

### Helping us understand if the code quality is actually good

Proving the code is good enough quality that this can be done fairly easily, complexity usually should be from
intentional complexity, not from our own accidental complexity from poor organization. How we organize things aids
clear understanding, clear understanding allows pace and the ability to easily keep a model of what you are working
on in your brain. Poor organization leaves you tired with your head spinning.

Procedural/OO/Functional are organizational approaches that allow things to be communicated in different ways. Instead
of arguing about languages, it is better to learn about the short falls of each paradigm. Some short falls can be dealt
with by better practices. A lot of people hate how people use inheritance as it is hard to get right, hence it is 
not in some languages. Inheritance is not about bunging things in a parent structure such as a class of trait for convenience’s
sake, it is about the identity of the class. This gets really bad in tests as all the organization tends to favour DRY
versus being comprehensible on the detail of what is happening. There are testing anti-patterns and squirrelling things
all over the place making you bounce around is a major sign of http://xunitpatterns.com/Obscure%20Test.html, it is worth
reading if you want to save a lot of headaches. 

#### Example: General Fixture
Scalatest contexts also come under general fixtures <br/>
https://www.scalatest.org/user_guide/sharing_fixtures <br/>

```
There seems to be a lot of test fixture being built; much more than it would appear to be necessary 
for any particular test. It is hard to understand the "cause and effect" relationship between the fixture, 
the part of the SUT being  exercised and the expected outcome of a test.
```
A test shouldn't feel like we are building an engine, it should feel human. It should be a valuable tool for understanding
when someone comes into the team. This helps to onboard people as they can easily work things out like business 
rules and reasons for themselves.

The less understandable the test is, it is likely due to the code has poor separation of concerns. The test should NOT read
like a puzzle. Everything we need to know and call should be available at an EASY glance. Of course, this can go to the extreme
where people forbid any reusable things in the tests. Software developers/engineers are superb at throwing the bath
out with the baby and the bathwater. This can lead to jumping out of the frying pan into the fire. Creating a new sometimes 
larger headache while running away from the old one. Before changing the direction, we need to really understand the problems
of the current one.

### We ideally use the critical part of our brain

Writing a test should be about working out negative outcomes of decisions. We should be trying to defeat our understanding
as this leads to better more detailed understanding. When writing code often we are in a flow state, this is fantastic for 
some things but not so good at spotting problems down the road. We become transfixed on the ticket versus the whole picture.
When writing the test, it is about thinking the bigger picture. What problems this solution may have in the future, etc.
It should exercise the more human parts of the brain that deal with communication, our code is communication, communication
with a machine and communication with each other.

Things like implicit contracts can be much easier to spot when thinking about entrance point down in a test. An implicit 
contract is when we allow or disallow something without clarifying we are. An example I would use is if you have an api
that accepts a JSON payload without validation and stores it in a DB. The implicit contract is max http post-size the
server accepts and what the database will accept (characters/fields size etc.). People will assume the unintentional
may be intentional, rely on it, and then you cannot tighten it easily if you have a problem. Understanding published
interface (code or api are both interfaces) can be very helpful https://martinfowler.com/ieeeSoftware/published.pdf.

### We avoid ending up to poor design due to the implementation being a sunk cost fallacy

Here is an unfortunately widespread scenario that reinforces itself as it creates a hole we keep digging in, and why 
I am doing all this. We spend a chunk of time adding code to where we think it is easiest to add it. We don't pay
attention to any of the tests, we go to the relevant test, and it is already a major headache. We have already spent time,
we do not want to 'feel' like we have wasted time so we keep barrelling along the current path.

We have just added to the mess, and also probably wasted a lot of time in a way that forces the next person to waste time.
The rule is we spend time to save other people time as there are more of them than us usually, if everyone does this, 
then we have a fast happy team. Fast is not based on per ticket, but how well we adapt and perform over a period. Not doing
this and fixating on each ticket is "penny wise pound foolish".

## So what should we do?

Audit the tests you will have to touch beforehand. Locate the complexity in the test and then locate the part of the code
causing complexity.

In this code 
[PoorSeparationOfConcernsExample.scala](src/main/scala/refactoring_examples/extract_class/PoorSeparationOfConcernsExample.scala)

The complexity is being caused by 2 places, these are their own concerns and should be classes. But people tend to not 
think like that until they get skilled, perhaps they will never as they get promoted beforehand :(. Instead, they 
will keep adding stuff to an existing class as it is the most opportunistic thing to do. Most design tends to end up
opportunistic unless you proactively learn design. Seniors should know about design, but a lot of it is just practices
people pass to each other, so people are at the mercy of the people doing the passing. 

There is a lot of literature on doing it more effectively as opportunistic design is not a new problem. 

### The parts that are causing the complication and should be refactored out

#### Validating the user against the mode
This is detailed here <br/>
[ExtractingOutUserOperationValidation.md](docs/ExtractingOutUserOperationValidation.md)

#### Extracting Out The Calculation For The Financial Details Summarization
This is detailed here <br/>
[ExtractingOutFinancialDetailsSummarization.md](docs/ExtractingOutFinancialDetailsSummarization.md)


### Final refactored main code and tests

#### Code and tests before refactor
This is detailed here <br/>
[ProductionCodeWithTestBefore.md](docs/ProductionCodeWithTestBefore.md)

#### Code and tests after refactor
This is detailed here <br/>
[ProductionCodeWithTestBefore.md](docs/ProductionCodeWithTestBefore.md)
