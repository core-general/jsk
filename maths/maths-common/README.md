# Maths Common

Mathematical data structures and function estimation — provides data set modeling, least-squares curve fitting with Levenberg-Marquardt optimization, and standard function prototypes.

## What It Solves

- `MDataSet` / `MDataSets` — multi-dimensional data point containers with validation, limits calculation, and color support
- `MLeastSquareFuncEstimator` — fits parameterized mathematical functions to data using Levenberg-Marquardt optimization with global random parameter search
- Standard function prototypes: polynomial (`MX1Linear0`–`MX1Linear3`), logarithmic (`MX1Log1`), exponential (`MX1Ex1`) — each with value and Jacobian implementations

## Key Details

- Uses Apache Commons Math 3 for the Levenberg-Marquardt optimizer
- Functions are JSON-serializable (via `JsonPolymorph`)
- `funcParallelRandomSearch()` tries 10,000 random starting points with 100,000 iterations per point
