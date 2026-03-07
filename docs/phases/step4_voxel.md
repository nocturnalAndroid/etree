# Step 4: Add Voxel Grid + Collision Avoidance

## Goal

Branches are spatially aware. Growth is influenced by local crowding. Collision avoidance prevents gross overlaps.

## What to Implement

- 3D voxel grid (resolution: ~5cm per voxel initially, tune later)
- Phase D: populate grid after each season
- Upgrade Phase A: bud activation influenced by local density (light estimate from voxel grid)
- Upgrade Phase A: soft repulsion during shoot extension (Level 2 collision avoidance) — or skip and rely on pruning (Level 1), depending on Step 3 results
- Upgrade Phase C: pruning heuristic to Level 2 if needed (crown depth + voxel density)

## Evaluation Criteria

- Does the crown fill space more evenly?
- Are buds in crowded regions suppressed?
- If using repulsion: do branches curve around each other naturally?
- Compare 2D projection: are there obvious overlaps?

## Contingency

- Voxel resolution too coarse (branches pass through each other): increase resolution.
- Too fine (branches can't grow anywhere): decrease resolution.
- Repulsion creates artifacts: dial back, rely more on pruning.
- Tree looks good without explicit collision avoidance: skip it — simpler is better.

---

## TODO

- [ ] Implement 3D voxel grid data structure
- [ ] Implement Phase D: populate grid from current tree state
- [ ] Upgrade bud activation to use local voxel density as light proxy
- [ ] Evaluate: is collision avoidance needed beyond pruning?
- [ ] If needed: implement soft repulsion during shoot extension
- [ ] If needed: upgrade pruning heuristic to Level 2 (crown depth + density)
- [ ] Tune voxel resolution for balance between accuracy and growth freedom
