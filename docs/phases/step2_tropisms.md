# Step 2: Add Tropisms

## Goal

Branches exhibit gravitropism (droop with weight) and grow outward/upward, producing a natural-looking crown shape.

## What to Implement

- **Gravitropism**: Direction bias toward gravity, proportional to segment length and weight. Cherry-specific: branches arch outward then droop slightly.
- **Phototropism (simplified)**: Upward + outward bias. No density grid yet — just a preference for growing away from the trunk center.

## Evaluation Criteria

- Do branches arch outward in a cherry-like spreading pattern?
- Does the young tree transition from upright leader to spreading form over ~10 years?
- Does gravitropism produce natural-looking droop without branches pointing straight down?

## Contingency

If branches droop too much, reduce gravitropism strength. If the tree is too columnar, increase branching angles and/or reduce apical dominance with age.

---

## TODO

- [ ] Add gravitropism direction bias to shoot extension
- [ ] Weight gravitropism by segment length and accumulated downstream weight
- [ ] Add simplified phototropism (upward + outward bias)
- [ ] Add age-dependent apical dominance (strong young, weak mature)
- [ ] Tune parameters for cherry-like spreading pattern
- [ ] Validate: young tree upright, mature tree spreading
