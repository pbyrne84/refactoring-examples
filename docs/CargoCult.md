# How to know if you are behaving like you are in a cargo cult when programming

This is a bit of a tangent, and an old man shouting up at the clouds scenario. Lots of good ideas descend into a cargo
cult when they hit the general populace. This, I think, is because we have been brought up too much to blindly follow,
not question. Things evolve from problems, so we need to understand the problems that caused the thing to happen, we need
to question the reasoning why people expect us to do things. We need to do this without causing people too many headaches 
or else they become less open to new avenues of reasoning, taking into account this may not be a new avenue of reasoning
for them. They just may not have the capacity to verbalise at the moment and need time to unpack a good response.

https://en.wikipedia.org/wiki/Cargo_cult_programming

```
Cargo cult programming can also refer to the practice of applying a design pattern or coding style blindly 
without understanding the reasons behind that design principle. Some examples are adding unnecessary comments 
to self-explanatory code, overzealous adherence to the conventions of a programming paradigm, or adding deletion code 
for objects that garbage collection automatically collects.
```

1. Everyone is copying each other, relying on their sense of innate capability to wing it. It is akin to not
   reading the instructions when you buy something. Which does add to the fun in a personal setting, but not a team one.
2. Your only source of understanding comes from near coworkers. Seniority can often be based on tenure, not understanding,
   this can lead to less than ideal practices. So it is important the whole team questions why (respectfully), else their learning
   becomes stunted.
3. You are doing something because you are told to do it. Unless people can find personal benefit, they will cut the wrong corners when under pressure.
   You need to learn the history of why things are done certain ways. Google it, find arguments, importantly for and against.
   You can learn the nuance of using something from "the against" arguments. This helps prep the conversation.
4. Velocity sucks more and more, and the quality of the project gets worse.
5. People jump from approach to approach without working out why the original approach was actually problematic.
   A change of approach doesn't just have to handle current complexity, it also needs to be able to not collapse
   under new complexity. Quite often we just kick the can a bit further down the road, leaving a much bigger problem for someone
   else.

All approaches we do should take into account how they affect the cognitive load of any person who works on it.
https://en.wikipedia.org/wiki/Cognitive_load
We have to take into account that we need to train people to understand seemingly complex approaches, as sometimes communicating
simply actually requires things people may find complex at first. Generics are a good example of this. Without generics,
we end up with accidental complexity due to having to work around the language. Don't have an enforced type system, we
need to remember a lot more making it harder for new people to ramp up to speed.

Bad design/organization leads to the destruction of us being able to rely on working memory easily
https://en.wikipedia.org/wiki/Working_memory. As that is a primary thing we rely on, this is NOT a good place to be.

A good example of "loss of working memory" is being in the middle of something, getting distracted by a meeting, and then spending
20 minutes, maybe an hour, trying to rebuild it so you can effectively work again.
