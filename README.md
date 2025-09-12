Revisiting a project that I first attempted many years ago now that I have more programming experience under my belt - a solver for [Numberlink](https://en.wikipedia.org/wiki/Numberlink) puzzles like [these here](https://numberlinks.puzzlebaron.com/).

The specific objective of this project was to write a solver that can search through the solution-space in an intelligent way. It still doesn't quite capture the full depth of human heuristics of what seem like "good" moves to someone who has solved a great deal of these puzzles, but it does have a very sophisticated system to fully account for all the possible "trivial" consequences of a given move.

The code is reasonably efficient, but I've focused more on implementing a sophisticated search algorithm than hardcore bit-fiddling.
