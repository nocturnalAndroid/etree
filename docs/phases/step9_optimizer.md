# Step 9: Parameter Optimizer for Lifelike Cherry Trees

## Goal

Build an optimizer that takes observed/measured properties of real cherry trees and searches the GrowthParams parameter space to find values that produce the most realistic simulated trees.

## Research Questions

- What measurable properties of real cherry trees can we use as optimization targets? (crown aspect ratio, branching angles, branch length distributions, fractal dimension, trunk taper, height-to-diameter allometry, branch order statistics)
- What cherry tree measurement datasets or published allometric tables exist?
- What optimization approach fits best given ~19 continuous parameters and a stochastic simulator? (Bayesian optimization, CMA-ES, differential evolution, etc.)
- What objective/loss function compares simulated vs real tree properties? (weighted sum of normalized errors across measurable traits)
- How fast is one simulation evaluation, and how many evaluations can we budget?

## What to Implement

- **Target specification**: a data structure defining desired tree properties (e.g. height-to-width ratio at maturity, mean branching angle, branch length decay per order)
- **Feature extraction**: functions that measure the same properties from a simulated TreeState (crown width/height, mean/std of branching angles, segment length distributions, branch order depths)
- **Objective function**: compare extracted features against target spec, return a scalar loss
- **Optimizer harness**: run an optimization algorithm (e.g. scipy.optimize, Optuna, or CMA-ES) over the parameter space, calling simulate → extract → score in a loop
- **Cherry tree target values**: research and populate realistic target values from literature or photo analysis
- **Integration**: either a standalone CLI script (`python -m etree.optimize`) or a web UI panel, or both

## Evaluation Criteria

- Does the optimizer converge to parameter sets that visually resemble cherry trees?
- Are the optimized parameters botanically plausible?
- Does the optimizer run in reasonable time (minutes, not hours)?
- Can we define multiple target profiles (young cherry, mature cherry, weeping cherry)?
- Do optimized trees look better than hand-tuned defaults?

## Contingency

- If the parameter space is too rugged for gradient-free optimization: reduce dimensionality by fixing less-important params
- If simulation is too slow per evaluation: reduce num_years or use a fast approximate fitness
- If no good cherry measurement data: use reference photos + manual measurement of proportions

---

## TODO

- [ ] Research cherry tree allometric data (branch angles, crown ratios, growth rates)
- [ ] Define target feature vector for a mature cherry tree
- [ ] Implement feature extraction from TreeState (crown geometry, branching stats)
- [ ] Implement objective/loss function
- [ ] Choose and integrate optimizer (Optuna or CMA-ES recommended)
- [ ] Build CLI entry point for running optimization
- [ ] Validate: compare optimized trees against reference photos
- [ ] Optionally expose in web UI (run optimization, show progress, apply best params)
