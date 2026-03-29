# JX Probability Algorithms

Probabilistic data structures library — provides space-efficient approximate algorithms for set membership, frequency estimation, and cardinality counting.

## What It Solves

- `BloomFilterImpl` — probabilistic set membership testing (implements `ICountSetExistence`)
- `CountMinSketchCounterImpl` — frequency estimation for element occurrence counting (implements `ICountElementsInGroups`)
- `HyperLogLogCounterImpl` — approximate cardinality estimation (implements `ICountAllElements`)
- `MurMurHash3` — hash function used by all implementations

## Key Details

- Three distinct probabilistic data structures, each implementing a separate abstract interface — pluggable into any system that needs approximate counting
