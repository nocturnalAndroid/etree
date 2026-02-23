# eTree — Execution Plan

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

Each iteration represents one growing season (approximately one year). Within a season, five phases execute in order.

### Phase A: Bud Activation and Shoot Extension (Spring)

For every living branch tip that has dormant buds:

1. **Score each bud** for activation probability:
   - **Apical dominance:** Terminal bud (at the very tip) gets the highest score. Lateral buds are suppressed proportional to their distance from the tip. Dominance strength is a parameter that decreases with tree age (young cherry = strong central leader, mature cherry = weak leader, spreading form).
   - **Light estimate:** Query the voxel grid at the bud's position. Higher light → higher activation probability. This naturally suppresses buds in the interior.
   - **Vigor:** The parent branch's resource flux (approximated by its diameter) affects how many buds can activate. Thick, well-fed branches activate more buds.
   - **Random noise** from seeded PRNG for variation.

2. **Activate** buds probabilistically. Each activated bud becomes a new shoot.

3. **Extend each new shoot:**
   - **Initial direction:** Parent's direction, rotated by the branching angle (species parameter, ~45-65 degrees for cherry) around the parent axis, at the bud's phyllotactic angle.
   - **Direction modifiers** (applied as additive biases to the direction vector):
     - *Gravitropism:* Slight upward bias for young branches, transitioning to horizontal/drooping for older heavy branches. Magnitude proportional to segment length and accumulated weight downstream.
     - *Phototropism:* Bias toward low-density voxels (open space). Computed as the gradient of the density field at the tip — grow away from crowded regions.
     - *Collision avoidance:* If target voxel is occupied, redirect away from the occupied region. (See collision avoidance section below.)
   - **Shoot length:** Determined by vigor (parent's resource flux × species growth rate × random noise). Cherry trees: vigorous shoots ~15-40cm, weak shoots ~2-10cm. Gompertz curve within the season, but since we simulate per-year, the shoot reaches its final length in one step.
   - **Segmentation:** A single shoot may be broken into 2-3 segments if it is long, to allow curvature and collision checks at intermediate points.

4. **Form new buds** along each new shoot. Buds are placed at phyllotactic intervals (golden angle spiral). Approximately one bud per 3-8cm of shoot length (species parameter). These buds are dormant until the next season.

### Phase B: Secondary Growth (Diameter Update)

1. **Pipe model (bottom-up):** Traverse the tree from tips to root. Each terminal branch contributes a base leaf area. At each junction, sum children's downstream leaf areas. Compute structural diameter: `d = scaling_constant * sqrt(downstream_leaf_area)`.

2. **Cambial thickening:** Add an annual ring width to every living segment. Ring width decreases with branch age (fast thickening when young, slowing). `ring_width = base_ring * exp(-decay * age)`.

3. **Non-shrinking rule:** `segment.diameter = max(pipe_model_diameter, segment.diameter + ring_width)`. When pruning removes downstream branches, the pipe model gives a lower number, but the existing wood remains — the branch just doesn't get the growth "boost" anymore.

### Phase C: Self-Pruning

1. **Estimate light** for each branch segment using a heuristic (see "Pruning Strategy" section below for which heuristic and the escalation plan).

2. **Carbon balance:** For each branch, compare estimated photosynthetic income against respiratory cost.
   - Income: `light_score * leaf_area * photosynthetic_rate`
   - Cost: `length * diameter^2 * respiration_rate` (proportional to wood volume)
   - A branch in deficit accumulates "stress." After N consecutive years of deficit (N = 2-3, tunable), the branch dies.

3. **Death process:**
   - Dying branches lose leaf area over 1-2 seasons (gradual, not instant, to avoid sudden diameter changes in ancestors via pipe model).
   - Dead branches persist as stubs for 1-3 years (visual realism: real dead branches don't vanish instantly).
   - After the stub period, the dead segment is removed from the graph.

### Phase D: Update Spatial Grid

Rebuild the 3D voxel grid from the current tree state:
- Clear all voxels.
- For each living segment, mark the voxels it passes through with: branch presence flag, cumulative branch volume, cumulative leaf area.
- This grid is used in the next season's Phase A (bud activation, growth direction) and Phase C (pruning heuristic).

---

## Pruning Strategy — Escalation Plan

Start with the simplest heuristic. Evaluate the result visually. Escalate only if needed.

### Level 1: Crown Depth (start here)

For each branch, compute its distance from the nearest point on the crown surface (convex hull or fitted ellipsoid of all branch tips). Normalize by crown radius. Branches deeper inside → higher death probability.

**Evaluation criteria:**
- Does the interior thin over time? (should see hollow crown developing)
- Does the trunk/lower region clear over time? (natural pruning / bole cleaning)
- Do the surviving branches form a plausible cherry silhouette?

**When to escalate:** If pruning is too uniform (all interior branches die at the same rate, no spatial variation), or if branches in locally sparse regions die when they shouldn't.

### Level 2: Crown Depth + Voxel Density

Add local density awareness. Query the voxel grid at each branch's position: count occupied voxels in a neighborhood. High local density → more competition → higher death probability, even if crown depth is moderate. Low local density → branch survives even if somewhat interior (it found a gap).

**Evaluation criteria:**
- Do sparse regions within the crown retain branches?
- Do dense clusters thin unevenly (some branches win, some lose)?
- More natural asymmetry than Level 1?

**When to escalate:** If the vertical structure is wrong (e.g., lower branches die too fast or too slow relative to upper ones), suggesting that a directional light component is needed.

### Level 3: Shadow Cone (Directional Light Proxy)

For each branch, cast a cone upward (toward the sky) and count how much branch/leaf mass lies within the cone. This estimates how much light is blocked from above. Branches under heavy canopy above → shaded → die. Branches with open sky above → survive.

Can be implemented cheaply using the voxel grid: sum occupied voxels in a column above the branch, weighted by vertical distance (closer canopy shades more).

**Evaluation criteria:**
- Does the tree develop the correct asymmetry (top of crown thriving, interior shaded)?
- Does an open-grown tree (light from all sides) retain lower branches appropriately?

**When to escalate:** Unlikely to need Level 4 for visual purposes. But if results are still unsatisfactory...

### Level 4: Voxel Ray Light Estimation (Last Resort)

Cast multiple rays from each branch toward the sky hemisphere. For each ray, walk through the voxel grid and accumulate occlusion (Beer-Lambert-style exponential extinction). Sum over all rays for total light estimate.

This is the most accurate heuristic short of full ray tracing. Should produce excellent results but is the most expensive. Only use if Levels 1-3 produce visually inadequate pruning.

---

## Collision Avoidance Strategy — Escalation Plan

### Level 1: Pruning-Based (start here, no explicit collision avoidance)

Don't prevent collisions during growth. Instead, rely on self-pruning: branches that end up in crowded positions will have low light scores and die. Transient overlaps are allowed during growth; they resolve within a few seasons.

**Evaluation criteria:**
- Do branches visually overlap in the 2D projection?
- If overlaps occur, are they transient (resolve via pruning within 2-3 seasons)?
- Does the crown fill space reasonably without clumping?

**When to escalate:** If persistent overlaps remain after pruning, or if the 2D rendering shows obvious branch-through-branch intersections that look wrong.

### Level 2: Soft Repulsion During Growth

When extending a new shoot, query nearby voxels. If there are occupied voxels close to the growth path, add a repulsive bias to the direction vector (push away from occupied space). The repulsion is soft — it bends growth, doesn't block it.

This naturally combines with phototropism (grow toward open space) and can use the same voxel grid density gradient.

**Evaluation criteria:**
- Do branches curve around each other gracefully?
- Does the repulsion produce natural-looking curves, or weird artifacts (branches veering unnaturally)?

**When to escalate:** If soft repulsion produces visible artifacts (unnatural S-curves, branches that look like they're dodging something). In that case, reduce repulsion strength and lean more on pruning (Level 1).

### Level 3: Hard Voxel Occupancy

Mark voxels as "solid" when a branch occupies them. New shoots cannot extend into solid voxels. If the target voxel is solid, the shoot stops growing in that direction or picks an alternate direction.

This is the nuclear option — guaranteed no overlap, but may produce blunt branch terminations or unnatural redirection. Use only if needed.

---

## Implementation Steps

Each step is independently testable. Evaluate visually at each step before proceeding.

### Step 1: Dev UI + Minimal Viable Growth

**Goal:** A single trunk grows upward. Buds form along it. Some buds activate and produce lateral branches. Lateral branches produce their own buds and sub-branches. Runs for ~5 simulated years. Results are visible in a development UI.

**Implement:**
- Tree data structure (nodes, segments, buds)
- Phase A: bud activation (apical dominance only, no light/density yet) + shoot extension (fixed direction, no tropisms yet)
- Phase B: pipe model diameter computation (bottom-up)
- **Development UI**: a grid view where each column is a season/phase (winter bare, spring bloom, summer canopy, autumn) and each row is a year. The simulation runs for N years and populates the full grid. Parameter sliders on the side allow tuning values (branching angle, apical dominance, growth rate, etc.) and re-running the simulation to see the grid update. This UI is the primary tool for all subsequent steps.

**Evaluate:**
- Does it look like a young sapling? Dominant central leader with smaller laterals?
- Do proportions look right? (trunk thickest, laterals thinner, sub-laterals thinnest)
- Does the exponential branching produce reasonable branch counts after 5 years?

**Contingency:** If branching is too dense or too sparse, tune: bud spacing, activation probability, shoot length parameters. If the trunk doesn't dominate, increase apical dominance strength.

### Step 2: Add Tropisms

**Goal:** Branches exhibit gravitropism (droop with weight) and grow outward/upward, producing a natural-looking crown shape.

**Implement:**
- Gravitropism: direction bias toward gravity, proportional to segment length and weight. Cherry-specific: branches arch outward then droop slightly.
- Phototropism (simplified): upward + outward bias. No density grid yet — just a preference for growing away from the trunk center.

**Evaluate:**
- Do branches arch outward in a cherry-like spreading pattern?
- Does the young tree transition from upright leader to spreading form over ~10 years?
- Does gravitropism produce natural-looking droop without branches pointing straight down?

**Contingency:** If branches droop too much, reduce gravitropism strength. If the tree is too columnar, increase branching angles and/or reduce apical dominance with age.

### Step 3: Add Self-Pruning (Level 1)

**Goal:** Interior branches die over time. The tree develops a hollow crown with living branches at the periphery. Branch merging simplifies the structure.

**Implement:**
- Crown surface estimation (convex hull or ellipsoid fit to branch tips)
- Crown depth pruning heuristic (Level 1)
- Carbon balance: stress accumulation over consecutive deficit years
- Death process: gradual leaf loss, stub persistence, removal from graph (dead stubs left as pass-through nodes)

**Evaluate:**
- Does the interior thin over time? Check at year 5, 10, 20, 30.
- Do clear scaffold branches emerge as competitors are pruned?
- Do the surviving branches have characterful kinks at former junction points (naturally, from the retained pass-through nodes)?
- Does the overall silhouette resemble a cherry tree at maturity?
- **Critical test:** Compare young tree (bushy, dense) → middle-aged (emerging scaffolds) → mature (clean structure) progression against reference photos.

**Contingency:** If pruning is too aggressive (tree goes bald), reduce death rate or increase stress tolerance. If pruning is too weak (tree stays bushy), increase sensitivity to crown depth. If the wrong branches survive (e.g., interior branches persist while outer ones die), escalate to Level 2 (add voxel density).

### Step 4: Add Voxel Grid + Collision Avoidance

**Goal:** Branches are spatially aware. Growth is influenced by local crowding. Collision avoidance prevents gross overlaps.

**Implement:**
- 3D voxel grid (resolution: ~5cm per voxel initially, tune later)
- Phase D: populate grid after each season
- Upgrade Phase A: bud activation influenced by local density (light estimate from voxel grid)
- Upgrade Phase A: soft repulsion during shoot extension (Level 2 collision avoidance) — or skip this and just use pruning (Level 1), depending on Step 3 results
- Upgrade Phase C: pruning heuristic to Level 2 if needed (crown depth + voxel density)

**Evaluate:**
- Does the crown fill space more evenly?
- Are buds in crowded regions suppressed?
- If using repulsion: do branches curve around each other naturally?
- Compare 2D projection: are there obvious overlaps?

**Contingency:** If voxel resolution is too coarse (branches pass through each other between cells), increase resolution. If too fine (branches can't grow anywhere), decrease. If repulsion creates artifacts, dial it back and rely more on pruning. If the tree looks good without explicit collision avoidance (just pruning), skip it — simpler is better.

### Step 5: Tune for Cherry Tree

**Goal:** The simulation produces trees that look specifically like ornamental cherry, not generic broadleaf.

**Implement:**
- Cherry-specific parameter set: branching angles (45-65 deg), length ratio (~0.7), apical dominance schedule (strong young → weak mature), crown width:height target (~1.4)
- Age-dependent apical dominance: young tree has strong central leader, mature tree has spreading decurrent form
- Run simulation for 30-50 tree-years to see full lifecycle

**Evaluate:**
- Compare silhouettes against reference photos of young, middle-aged, and mature cherry trees
- Check: does the trunk fork low? Do 3-5 main scaffolds emerge? Are they widely spreading?
- Check: is the crown wider than tall at maturity?

**Contingency:** This step is pure parameter tuning. Expect many iterations. If the structure is fundamentally wrong (not just parameter-level wrong), revisit Steps 3-4.

### Step 6: 3D Rendering and Camera

**Goal:** Render the 3D tree as a 2D image suitable for the display.

**Implement:**
- 3D → 2D projection (perspective or orthographic, fixed camera, side view)
- Branch rendering as tapered line segments (thickness = projected diameter)
- Catmull-Rom spline interpolation through waypoints (former junction kinks) for smooth curves
- Painter's algorithm (depth sorting) for overlap
- Output as image (PNG or similar)

**Evaluate:**
- Does the 2D rendering look like the reference images?
- Are branch widths reasonable at display resolution?
- Does depth sorting produce correct occlusion?
- Does spline interpolation produce natural-looking curves at kinks?

**Contingency:** If 3D projection produces a confusing tangle (too much depth complexity), try: narrower field of view, slight rotation to separate overlapping branches, or even explore 2D simulation as a fallback (growth in a plane, no depth).

### Step 7: Seasonal Cycle

**Goal:** The tree displays seasonal visual changes (bare winter → blossoms → leaves → autumn → bare).

**Implement:**
- Seasonal modulation layer (separate from structural growth):
  - Blossom rendering: red dots/clusters on spurs along branches with age >= 2 years. Peak in spring. Blossom density as a Gaussian in day-of-year.
  - Leaf rendering: dark canopy (black stipple or solid mass around crown) in summer. Sigmoid up in spring, sigmoid down in autumn.
  - Petal fall: scattered red dots drifting to ground during post-bloom period.
  - Bare branches: the default winter state, and the most visually striking.
- Map simulation years to display time (e.g., 1 tree year = 1 real year, or faster for testing)
- Within a tree year, map daily updates to seasonal state

**Evaluate:**
- Is the bloom moment (red on bare black branches) visually striking?
- Does the seasonal cycle feel natural in timing?
- Does summer foliage appropriately hide interior branches?

**Contingency:** Seasonal rendering is largely cosmetic — if something looks wrong, adjust timing/density parameters. The structural simulation is unaffected.

### Step 8: Time Interpolation for Daily Display Updates

**Goal:** Between annual simulation steps, produce smooth daily frames showing gradual growth and seasonal change.

**Implement:**
- Option A: Simulate at sub-annual resolution (e.g., monthly steps instead of annual). More accurate but more compute.
- Option B: Interpolate between annual snapshots. Shoots extend gradually over spring days (linear interpolation of length from 0 to final). Diameters interpolate smoothly. Seasonal overlays change daily.
- The choice depends on how the annual simulation looks — if it's smooth enough as-is, interpolation is fine.

**Evaluate:**
- Are day-to-day changes small enough to be imperceptible as individual jumps?
- Does the spring growth period show shoots gradually extending?
- Is any frame jarring relative to the previous day?

**Contingency:** If annual-step + interpolation looks jerky (especially during spring growth when topology changes), switch to sub-annual simulation steps (monthly or even weekly during spring, annual during winter dormancy).

---

## Open Design Questions (to be resolved during implementation)

1. **Voxel grid resolution:** Start at 5cm. Tune based on tree scale and collision behavior.
2. **Apical dominance decay with age:** How fast should the central leader weaken? This controls the transition from pyramidal sapling to spreading mature tree. Cherry-specific timing needed.
3. **Pruning stress threshold:** How many consecutive deficit years before death? Start at 2-3. Tune based on how fast/slow the interior thins.
4. **Stub persistence time:** How long do dead stubs remain visible? Affects visual realism during the messy middle-age phase.
5. **2D vs 3D:** If 3D projection produces a confusing tangle at 800x480, the fallback is 2D simulation in a plane. This sacrifices depth but may produce cleaner images. Evaluate at Step 6.
6. **Growth-to-display time mapping:** 1 tree year = 1 real year is the most atmospheric (watch it grow over actual years), but 1 tree year = 1 real month allows seeing maturity in 2-3 years. Could be configurable.
