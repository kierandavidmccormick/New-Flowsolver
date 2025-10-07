Revisiting a project that I first attempted many years ago now that I have more programming experience under my belt - a solver for [Numberlink](https://en.wikipedia.org/wiki/Numberlink) puzzles like [these here](https://numberlinks.puzzlebaron.com/).

The specific objective of this project was to write a solver that can search through the solution-space in an intelligent way. It still doesn't quite capture the full depth of human heuristics of what seem like "good" moves to someone who has solved a great deal of these puzzles, but it does have a very sophisticated system to fully account for all the possible "trivial" consequences of a given move.

The core of the algorithm is Iterative Deepening Search with very aggressive pruning to reduce the size of the search space as much as possible. The key insight that really makes this solver possible is a technique of "proof by counterexample", proving that invalid states are such as soon as possible to prune all the daughter branches of the game tree, amounting to a huge amount of the search space.

All the puzzles in boards/ are tractable to this solver - most notably those in imported.txt, which serve as the meat of the demonstration. The largest, those around 20x30 size, can take a few minutes on my (decent) machine.

The code is reasonably efficient, but I've focused more on implementing a sophisticated search algorithm than hardcore bit-fiddling, and all the solver code is still running single-threaded.