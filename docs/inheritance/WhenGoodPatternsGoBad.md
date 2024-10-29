# When good patterns go bad

So, you may have heard about design patterns, but not necessarily the history of them. Design patterns are a formal
way we can communicate concepts. This is why it is food to know them, knowing them also helps us understand the problems
we have with doing them. They are solutions that naturally occur, but people don't necessarily know they are doing them
do come up with more 'interesting' variations. Unfortunately, people took design patterns as a form of recipe to build
programs, and as they often did not think in terms of communication but checklists, made things worse.

The term for this is Patternitis, you have a solution, and you are looking for a problem. Pretending they don't exist
just means you may not be aware of what you are actually doing. There was a pushback against patterns because of this
instead of people really trying to understand the issue. Naturally occurring things naturally occur, they don't need
us to be conscious of it for it to happen. It does help if we are conscious, so we can spot problems that other people
have had before we have the same.

## A quick overview of design patterns.

### Ones that are now viewed as bad

Whether something is good or bad is based on how it scales with complexity and how hard it is to move to a better
solution. It can feel impossible to climb your way out, so people will keep using it until destruction. The most
problematic ones also seem the easiest sometimes, but really they are akin to using a bulldozer to go shopping. Inheritance
is also like that, it is something people reach for to share internal functionality where composition causes far fewer headaches.

#### Singleton

Now, Scala confuses things with its concept of a singleton object. Really, it just is somewhere to put your static/constant
values. When people refer to the Singleton Pattern, they mean a class that determines it can be its only instance.

I will use java as it follows a similar pattern to all languages that merge static and non-static within the same entity.
```java

public class ExampleSingleton {

    //This is effectively a global state to use it like a global variable (you should twinge at the term "global variable")
    private static ExampleSingleton instance = new ExampleSingleton();

    //We have blocked instantiation
    private ExampleSingleton() {

    }

    // We get the instance
    static ExampleSingleton getInstance() {
        return instance;
    }
}
```

This was possibly peoples' first naturally occurring pattern. They think that there can only be one instance of something,
they want to access it easily so use a global mechanism, which they get using the static **getInstance()**. Unfortunately,
this design is very inflexible, for example, only allow a single db connection, and can be arduous to untangle. The refactor
to get out if this is called **inline singleton**. More reading here https://www.informit.com/articles/article.aspx?p=1398606&seqNum=6.

Scala by nature of being mutation avoidant makes marking things as singleton safer due to its thread safety, but the rule
is that the class cannot be that opinionated about itself. It is up to the thing controlling all the instantiations, and
we inject it down. Think of how someone may annotate in Guice (@Singleton). We do that to save instantiation costs,
not because we want to do global variables, etc.


#### Service Locator
I see this one pop up with people not really knowing they are doing it. Fundamentally, it is a container that you ask
for dependencies, it has an unclear interface, so you won't know if it will fail except for runtime, including when running tests.
This was often used as a step to going to constructor injection as they may not have control of how things are constructed, such as being
in an opinionated framework that has no concept of flexible construction.

```scala
import scala.reflect.ClassTag

//Could be an instance or object. Instances are usually just more flexible as you can give it things, whereas static
//always has to be able to find things. Finding things can make things harder as static can only rely on static, making
//lots of things static. Except for very simple things, like simple creation/factory methods, I would stick to instances.
class DummyService

class ServiceLocator {

  def locate[A](implicit classTag: ClassTag[A]) = {
    if (classTag.runtimeClass == classOf[DummyService]) {
      new DummyService()
    } else {
      // Not really nice
      throw new NotImplementedError(s"${classTag.runtimeClass} has not been configured")
    }
  }
}

val serviceLocator = new ServiceLocator
serviceLocator.locate[DummyService]

```
The issues with using this approach is it completely occludes what a class actually needs. It makes the class
act like it is in the pick and mix sweet section trying to decide what it needs, and never knowing whether it can get it.
People sometimes pass a map of dependencies down in duck typed languages, but this has the same issue of occlusion.

##### Registry
This is another pick and mix solution based on hope. I have dealt with a system that relied on this for state. It makes
it impossible to now what will be there at any point in time.

The premise is shared mutation with things held in a key. It basically just is a form of hashmap that often used the singleton
pattern. So, a global mutating hashmap.

```scala
import scala.collection.mutable

object Registry {
  val instance: Registry = new Registry()
}

class Registry {
  private val map = new mutable.HashMap[String, String]

  def set(key: String, value: String): Unit = {
    map.put(key, value)
  }

  def get(key: String): Option[String] = {
    map.get(key)
  }
}

Registry.instance.set("key", "value")
Registry.instance.get("key")
```

This makes testing really hard without doing everything by integration tests as one class may set something another
class needs, changing the call chain means you cannot tell what will be affected. It kills momentum when you stop
being able to make decisions effectively. Lack of thread safety always makes it fun.

### Template Method Pattern is now a pattern I view as likely to go bad very easily
Every time I teach someone this pattern, they don't know when to refactor out of it leading to a mess. I have been awed
by the mess. The issue lies in the fact that people view inheritance as building some kind of engine, when really inheritance
is a way of conforming to something else's expectation. It is not a mechanical concept.

Overriding an implementation of a method really muddies the water, leading to call chains you cannot follow. The child
calls the parent, the parent calls a method, has that method been overridden by the child you come from? It can tangle
very, very fast, leading us to a state of operating on guess work. We should not be operating on guess work as it is a
main indicator of poor design. We should always ask ourselves "what would I change, so I can understand it better?" also
questioning the answer with "What new problems we will face, and how quickly?". We are exceptional at running into new
problems quickly for not thinking things through.

In a template method pattern, the parent will call the implemented methods in the child. The child class fills in the template.
Unfortunately, this is very opinionated, meaning it gets tangled with complexity when the children do not simply align. The
call chains can get very confusing when people start chaotically overriding things. Chaotic is determined by its
sense of being unintelligible, smart applied in the wrong way.

```scala
object ExampleTangledInheritance {
  def main(args: Array[String]): Unit = {
    
    // What do you think these output, and should it be that hard to work out. Hint - no it should not be that hard
    // These call chains actually become very common and a reason why people say composition over inheritance.
    // Composition is about thinking in smaller concepts, inheritance leads to thinking in blobs.
    println(new B().run())
    println(new C().run())
  }
}

abstract class A {

  def run(): String = {
    callC(callB(callA))
  }

  protected def callA: String
  protected def callB(text: String): String
  protected def callC(text: String): String
  protected def callD(text: String) = s"cat $text"
}

class B extends A {
  protected def callA: String = "squirrel"
  protected def callB(text: String): String = callD(s"ape $text")
  protected def callC(text: String): String = s"orangutan $text"
}

class C extends B {

  override protected def callC(text: String) = s"monkey $text"
  override protected def callD(text: String) = s"dog $text"
}


```
