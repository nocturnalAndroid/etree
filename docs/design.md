# eTree — High-Level Design

## Core Design Principle

**All branches are born equal. Structure emerges.**

There are no "scaffold branches" and no "detail branches." A bud activates, a shoot extends, and the branch either survives to become part of the tree's defining structure, or it dies and is absorbed. The trunk forks because one junction's competitors were pruned. A major branch has a kink because there used to be a branching node there. The entire large-scale architecture — how many scaffolds, at what angles, how long — is an emergent product of three forces:

1. **Growth** — tips elongate, buds activate, driven by apical dominance and available resources
2. **Self-pruning** — shaded/crowded branches die, simplifying the structure
3. **Collision avoidance** — branches can't occupy the same space, redirecting or suppressing growth

The simulation runs in 3D model space (continuous coordinates, units in centimeters). Rendering to the display is a separate, later concern. Performance is not a constraint for now — use as much compute as needed to get the tree right.

---

## Data Structure

The tree is a mutable directed graph. Each node represents a point in 3D space where something interesting happens (a branch tip, a junction, a bud site).

**Branch segment** (edge between two nodes):
- Start node, end node
- Length (cm), diameter (cm)
- Direction vector (3D unit vector)
- Age (years since this segment was created)
- Status: growing / mature / dying / dead
- Curvature: optional, for smooth rendering (could be stored as a control point)

**Node:**
- 3D position
- Parent segment (toward root) and child segments (toward tips)
- Buds: list of dormant buds at this node, each with a phyllotactic angle and a vigor score

**Tree state:**
- Root node (ground level)
- Flat list of all segments (for iteration)
- 3D voxel grid (for collision avoidance and density estimation)
- Current simulation year
- PRNG state (for determinism)

When all children but one at a node die, the dead segments are removed but the node and the surviving child remain as-is. The tree graph retains the full history of junction points, which naturally produces the kinks and direction changes that give branches their organic character. No merging is needed — trivial pass-through nodes are cheap to keep and preserve structural information.

---

## The Simulation Loop

Each iteration represents one growing season (approximately one year). Within a season, four phases execute in order.

### Phase A: Bud Activation and Shoot Extension (Spring)

For every living branch tip that has dormant buds: score each bud for activation probability (apical dominance, light estimate, vigor, random noise), activate probabilistically, extend new shoots with direction modifiers (gravitropism, phototropism, collision avoidance), and form new buds along each new shoot.

### Phase B: Secondary Growth (Diameter Update)

Pipe model (bottom-up): traverse tips to root, sum downstream leaf areas, compute `d = scaling_constant * sqrt(downstream_leaf_area)`. Add annual ring width to every living segment. Non-shrinking rule ensures existing wood is preserved.

### Phase C: Self-Pruning

Estimate light for each branch segment using a heuristic. Compare photosynthetic income against respiratory cost. Branches in deficit accumulate stress; after N consecutive deficit years, they die. Dead branches persist as stubs briefly, then are removed.

### Phase D: Update Spatial Grid

Rebuild the 3D voxel grid from the current tree state. Mark voxels with branch presence, cumulative volume, and leaf area. Used by next season's Phase A and Phase C.

---

## Pruning Strategy — Escalation Plan

Start with the simplest heuristic. Evaluate visually. Escalate only if needed.

### Level 1: Crown Depth (start here)

Distance from branch to nearest crown surface point, normalized by crown radius. Deeper inside = higher death probability.

### Level 2: Crown Depth + Voxel Density

Add local density awareness from voxel grid. High local density = more competition. Low density = branch survives even if somewhat interior.

### Level 3: Shadow Cone (Directional Light Proxy)

Cast a cone upward, count branch/leaf mass above. Cheap via voxel grid column summation.

### Level 4: Voxel Ray Light Estimation (Last Resort)

Multiple rays toward sky hemisphere with Beer-Lambert extinction. Most accurate, most expensive. Only if Levels 1-3 are inadequate.

---

## Collision Avoidance Strategy — Escalation Plan

### Level 1: Pruning-Based (start here)

No explicit collision avoidance. Rely on self-pruning to resolve crowded positions.

### Level 2: Soft Repulsion During Growth

Query nearby voxels during shoot extension. Add repulsive bias away from occupied space.

### Level 3: Hard Voxel Occupancy

Mark voxels as solid. New shoots cannot extend into solid voxels.

---

## Open Design Questions

1. **Voxel grid resolution:** Start at 5cm. Tune based on tree scale and collision behavior.
2. **Apical dominance decay with age:** How fast should the central leader weaken? Controls pyramidal sapling → spreading mature tree transition.
3. **Pruning stress threshold:** How many consecutive deficit years before death? Start at 2-3.
4. **Stub persistence time:** How long do dead stubs remain visible?
5. **2D vs 3D:** If 3D projection produces a confusing tangle at 800x480, fallback is 2D simulation in a plane.
6. **Growth-to-display time mapping:** 1 tree year = 1 real year (atmospheric) vs 1 tree year = 1 real month (faster). Could be configurable.

---

## Implementation Steps

| Step | Focus | Details |
|------|-------|---------|
| 1 | [Dev UI + Minimal Viable Growth](phases/step1_growth.md) | Data structures, Phase A (apical dominance only), Phase B (pipe model), dev UI grid |
| 2 | [Tropisms](phases/step2_tropisms.md) | Gravitropism, simplified phototropism |
| 3 | [Self-Pruning](phases/step3_pruning.md) | Crown depth heuristic, carbon balance, death process |
| 4 | [Voxel Grid + Collision](phases/step4_voxel.md) | 3D voxel grid, density-aware buds, soft repulsion |
| 5 | [Cherry Tuning](phases/step5_cherry.md) | Cherry-specific parameters, 30-50 year lifecycle |
| 6 | [3D Rendering](phases/step6_rendering.md) | Projection, tapered segments, splines, depth sorting |
| 7 | [Seasonal Cycle](phases/step7_seasons.md) | Blossoms, leaves, petal fall, bare winter |
| 8 | [Time Interpolation](phases/step8_interpolation.md) | Smooth daily frames between annual snapshots |
