# Step 3: Add Self-Pruning (Level 1)

## Goal

Interior branches die over time. The tree develops a hollow crown with living branches at the periphery. Junction simplification emerges naturally.

## What to Implement

- Crown surface estimation (convex hull or ellipsoid fit to branch tips)
- Crown depth pruning heuristic (Level 1)
- Carbon balance: stress accumulation over consecutive deficit years
- Death process: gradual leaf loss, stub persistence, removal from graph (dead stubs left as pass-through nodes)

## Evaluation Criteria

- Does the interior thin over time? Check at year 5, 10, 20, 30.
- Do clear scaffold branches emerge as competitors are pruned?
- Do surviving branches have characterful kinks at former junction points?
- Does the overall silhouette resemble a cherry tree at maturity?
- **Critical test**: Compare young (bushy, dense) -> middle-aged (emerging scaffolds) -> mature (clean structure) progression against reference photos.

## Contingency

- Pruning too aggressive (tree goes bald): reduce death rate or increase stress tolerance.
- Pruning too weak (tree stays bushy): increase sensitivity to crown depth.
- Wrong branches survive: escalate to Level 2 (add voxel density).

---

## TODO

- [ ] Implement crown surface estimation (convex hull or ellipsoid)
- [ ] Implement crown depth computation for each branch
- [ ] Implement carbon balance: income vs respiratory cost
- [ ] Implement stress accumulation over consecutive deficit years
- [ ] Implement death process (gradual leaf loss over 1-2 seasons)
- [ ] Implement stub persistence (1-3 years) then removal
- [ ] Implement junction simplification (pass-through nodes preserved)
- [ ] Tune pruning parameters for cherry-like thinning
- [ ] Validate progression: bushy -> emerging scaffolds -> clean structure
