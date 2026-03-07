# Step 5: Tune for Cherry Tree

## Goal

The simulation produces trees that look specifically like ornamental cherry, not generic broadleaf.

## What to Implement

- Cherry-specific parameter set: branching angles (45-65 deg), length ratio (~0.7), apical dominance schedule (strong young -> weak mature), crown width:height target (~1.4)
- Age-dependent apical dominance: young tree has strong central leader, mature tree has spreading decurrent form
- Run simulation for 30-50 tree-years to see full lifecycle

## Evaluation Criteria

- Compare silhouettes against reference photos of young, middle-aged, and mature cherry trees
- Check: does the trunk fork low? Do 3-5 main scaffolds emerge? Are they widely spreading?
- Check: is the crown wider than tall at maturity?

## Contingency

This step is pure parameter tuning. Expect many iterations. If the structure is fundamentally wrong (not just parameter-level wrong), revisit Steps 3-4.

---

## TODO

- [ ] Define cherry-specific parameter preset
- [ ] Implement age-dependent apical dominance schedule
- [ ] Run 30-50 year simulations
- [ ] Compare silhouettes against reference photos
- [ ] Tune branching angles for wide spreading pattern
- [ ] Tune crown width:height ratio toward ~1.4
- [ ] Verify trunk forks low with 3-5 emergent scaffolds
- [ ] Iterate parameter tuning until cherry-like
