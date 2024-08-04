# MCTS-based Planning for Grand Strategy Games

## Overview

This repository contains the implementation of planning systems based on Monte Carlo Tree Search (MCTS) and evolutionary algorithms for use in grand strategy video games. The primary focus is on the game TripleA, an open-source grand strategy game engine. The implemented planning systems aim to address the challenges of large search spaces and limited computational budgets inherent in these games.

## Thesis Summary

Grand strategy games, like Hearts of Iron and Europa Universalis, require managing a nation’s resources and military strategy to achieve long-term goals. The complexity and size of the search space, combined with limited computational resources, make AI planning in these games particularly challenging.

This thesis explores several state-of-the-art planning algorithms, including:

- **Bridge Burning MCTS**
- **Non-Exploring MCTS**
- **Online Evolutionary Planning (OEP)**

These algorithms were tested against each other and the existing AI solutions in TripleA. The results demonstrate that our agents consistently outperform the game’s current AI.

## Key Features

- **MCTS Variants**: Implementations of Bridge Burning MCTS and Non-Exploring MCTS tailored for grand strategy games.
- **Evolutionary Algorithm**: Implementation of Online Evolutionary Planning for dynamic decision making.
- **Domain-Specific Pruning**: Pruning strategies to reduce the search space effectively.
- **TripleA Integration**: Custom scenarios and maps designed for testing within the TripleA engine.
