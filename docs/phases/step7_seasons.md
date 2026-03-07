# Step 7: Seasonal Cycle

## Goal

The tree displays seasonal visual changes (bare winter -> blossoms -> leaves -> autumn -> bare).

## What to Implement

- Seasonal modulation layer (separate from structural growth):
  - Blossom rendering: red dots/clusters on spurs along branches with age >= 2 years. Peak in spring. Density as Gaussian in day-of-year.
  - Leaf rendering: dark canopy (black stipple or solid mass) in summer. Sigmoid up in spring, sigmoid down in autumn.
  - Petal fall: scattered red dots drifting to ground during post-bloom period.
  - Bare branches: the default winter state.
- Map simulation years to display time (e.g., 1 tree year = 1 real year, or faster)
- Within a tree year, map daily updates to seasonal state

## Evaluation Criteria

- Is the bloom moment (red on bare black branches) visually striking?
- Does the seasonal cycle feel natural in timing?
- Does summer foliage appropriately hide interior branches?

## Contingency

Seasonal rendering is largely cosmetic — if something looks wrong, adjust timing/density parameters. The structural simulation is unaffected.

---

## TODO

- [ ] Implement blossom rendering on age >= 2 year spurs
- [ ] Implement blossom density as Gaussian function of day-of-year
- [ ] Implement leaf canopy rendering (black stipple/solid mass)
- [ ] Implement leaf sigmoid spring emergence and autumn drop
- [ ] Implement petal fall (scattered red dots to ground)
- [ ] Implement year-to-display time mapping
- [ ] Implement daily seasonal state within a tree year
- [ ] Validate: bloom moment is visually striking
