# JFree Charts

JFreeChart wrapper — high-level API for creating line charts, scatter plots, bar charts, and function visualization from JSK data structures.

## What It Solves

- Static methods for chart creation: `lineChartX1()`, `scatterPlotX1()`, `barChartX1()`, `uniPlotX1()` (mixed lines+dots)
- `functionAndDataSet()` overlays fitted mathematical functions on raw data scatter plots with error display
- Chart output to PNG file, `BufferedImage`, or Swing `JFrame`
- Quick debug chart output to `/tmp/jfree_debug/`

## Key Details

- Single-class module (~290 lines)
- Depends on `maths-common` for data structures (`MDataSet`, `MDataSets`) and on JFreeChart
- Used by `g-cluster-checker` for deployment health visualization
