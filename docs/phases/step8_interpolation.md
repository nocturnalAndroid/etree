# Step 8: Time Interpolation for Daily Display Updates

## Goal

Between annual simulation steps, produce smooth daily frames showing gradual growth and seasonal change.

## What to Implement

- **Option A**: Simulate at sub-annual resolution (e.g., monthly steps). More accurate but more compute.
- **Option B**: Interpolate between annual snapshots. Shoots extend gradually over spring days (linear interpolation of length from 0 to final). Diameters interpolate smoothly. Seasonal overlays change daily.
- The choice depends on how the annual simulation looks — if it's smooth enough, interpolation is fine.

## Evaluation Criteria

- Are day-to-day changes small enough to be imperceptible as individual jumps?
- Does the spring growth period show shoots gradually extending?
- Is any frame jarring relative to the previous day?

## Contingency

If annual-step + interpolation looks jerky (especially during spring growth when topology changes), switch to sub-annual simulation steps (monthly or even weekly during spring, annual during winter dormancy).

---

## TODO

- [ ] Decide: sub-annual simulation vs interpolation (based on annual results)
- [ ] If interpolation: implement shoot length linear interpolation over spring days
- [ ] If interpolation: implement diameter smooth interpolation
- [ ] If sub-annual: implement monthly/weekly simulation stepping
- [ ] Implement daily seasonal overlay changes
- [ ] Validate: no jarring day-to-day jumps
- [ ] Validate: spring growth shows gradual shoot extension
