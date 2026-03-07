# eTree — Knowledge Base

## Project Description

A generative art system that grows a cherry blossom tree on an e-ink display over months and years. Given a seed, the system produces a tree structure that evolves slowly through time via iterative simulation: `S(t) = Grow(S(t-1), seed, t)`.

### Target Display
- **Resolution**: 800x480 (landscape)
- **Colors**: 3-color e-ink (white background, black branches/leaves, red blossoms). Future: 4/7-color or Spectra 6, not for MVP.
- **Refresh rate**: ~once per day.
- **Rendering**: 2D projection of a 3D structure. Pure 2D worth exploring if 3D doesn't pan out.
- **Aesthetic**: As realistic as resolution allows. Target: `referances/more_likely_referance_photo.jpg` style (sparse black branches, discrete red blossom clusters, bark line-work). Falling petals on the ground are a nice touch.
- **MVP species**: Cherry tree (Prunus) with red cherry blossoms.

---

## Settled Knowledge

### How Real Trees Grow

**Primary growth (elongation):** Happens only at tips via apical meristems. Once a branch segment elongates, its length is permanent. Cherry trees have determinate growth — each shoot does most of its elongation in a short spring flush (weeks), then stops and forms a terminal bud for next year. Growth curve follows a Gompertz function (fast start, long tapering).

**Secondary growth (thickening):** The cambium layer adds a ring of wood each year along the entire length of every living branch. Young branches add thick rings; old branches add thinner ones. Diameter never decreases — wood doesn't un-grow.

**Bud formation and activation:** Buds form along new shoots in a spiral arrangement (golden angle ~137.5 degrees between successive buds). The following spring, some buds activate and extend into new shoots. Terminal buds are strongly favored (apical dominance); lateral buds activate with lower probability, decreasing with distance from the tip. Light availability at the bud's position also affects activation probability.

### The Pipe Model (Diameter Computation)

Shinozaki (1964): the cross-sectional area of a branch equals the total area needed to supply all its downstream leaves. Equivalent to Da Vinci's rule with exponent ~2.0 (area-preserving): at any junction, parent cross-sectional area equals sum of children's areas.

Actionable result: `diameter(branch) = proportional to sqrt(downstream_leaf_area)`. Computed bottom-up from tips to trunk. Exponent is stable at ~2.0 across species and ages (drift of +0.1 to +0.2 over a lifetime, negligible for our purposes).

### Self-Pruning

A branch dies when its carbon balance goes negative: photosynthesis (proportional to light received times leaf area) minus respiration (proportional to wood volume, which scales with length times diameter squared).

Interior branches are shaded by the outer canopy, so they die first. In open-grown ornamental cherry, this process is slow and mainly affects the deep crown interior, not the lower branches (the tree gets light from all sides).

**Junction simplification:** When all children but one at a junction die, the dead segments are removed but the node remains as a pass-through point. This naturally preserves direction changes (kinks) at former junctions, which give branches their organic, irregular shape. Over many years, self-pruning transforms a bushy young tree into a simpler structure where a few winning branches define the silhouette. No explicit branch merging is needed — the graph just retains its history.

**This means: there are no "scaffold branches" at birth.** What we call scaffold branches in a mature tree are simply the winners of decades of competition. Every branch starts the same — as a bud that activated. The tree's large-scale structure is an emergent product of growth, competition, and pruning. This is the central insight of the project: we simulate all branches uniformly and let structure emerge.

### Self-Pruning Heuristics (from research, not yet tested)

Ranked from simplest to most sophisticated:

1. **Crown depth:** Distance from branch tip to nearest crown surface point, normalized by crown radius. Deeper = more likely to die. Cheapest to compute.
2. **Voxel density:** Divide 3D space into a coarse grid, count branch/leaf density per cell. High-density cells are "shaded." Adds spatial awareness within the crown.
3. **Shadow cone:** For each branch, count how much branch/leaf mass is directly above it (between it and the sky). More above = more shading. Adds directional light information.
4. **Light proxy + carbon balance:** Combine height score, surface distance, and density into a single "light score." Compare against respiratory cost (proportional to wood volume). Most biologically accurate heuristic.

Palubicki et al. (SIGGRAPH 2009) found that simplified local light estimation produced visually comparable tree shapes to full Beer-Lambert voxel ray tracing. Approximate light is sufficient for visual realism.

### Collision Avoidance

In real trees, "collision avoidance" is mostly achieved through self-pruning: branches that end up in crowded positions don't get enough light and die. Growth redirection from physical contact (thigmotropism) is a minor effect.

For the simulation, we need to prevent literal geometric overlap. Options:
- **Proximity check during growth:** When a shoot extends, check distance to nearby branches. If too close, redirect growth direction. Simple but may produce unnatural veering.
- **Voxel occupancy grid:** Mark occupied cells. Shoots can't grow into occupied cells. More robust.
- **Rely mainly on pruning:** Let branches grow freely, then prune crowded ones. Most biologically accurate but allows transient overlaps.

The voxel grid serves double duty: collision avoidance during growth AND density estimation for pruning.

### Tropisms

- **Gravitropism:** Branch direction is pulled downward proportional to accumulated length and weight. Cherry branches characteristically arch outward then droop slightly.
- **Phototropism:** Growth biased toward "open space" or upward. In the simulation, bias toward the outward normal of the crown surface, or toward voxels with low density.
- **Apical dominance:** The terminal bud suppresses lateral buds below it. Stronger in young trees (producing a central leader), weakening with age (producing the spreading decurrent form of mature cherry trees).

### Cherry Tree (Prunus) Specifics

**Physical form:**
- Mature height: 8-12m, trunk diameter 30-60cm
- Crown shape: wide, spreading, vase-shaped. Width:height ratio ~1.3 to 1.8
- Trunk forks low (~25% of height) into 3-5 scaffold branches (emergent, not pre-designed)
- Scaffold angles: 45-70 degrees from vertical (wide spreading)
- Branching pattern: alternate, golden angle spiral
- Length ratio (child:parent): ~0.65-0.75
- Max branching order: 5-7
- Taper exponent (beta): ~0.65
- Da Vinci exponent: ~2.0

**Growth pattern over lifetime:**
- Young (1-10 years): Strong upright central leader, narrow pyramidal crown, branches ascending at 30-45 degrees. Dense branching.
- Maturing (10-25 years): Crown broadens, central leader weakens (decurrent habit), scaffold branches arch outward. Self-pruning thins interior.
- Mature (25+ years): Broad spreading crown, long clean scaffolds with kinks from former junctions, foliage concentrated at periphery. Interior sparse.

**Seasonal cycle:**
- Winter dormancy (Nov-Feb): Bare branches. Full architecture visible. Ink-drawing aesthetic.
- Bud swell (late Feb-mid Mar): Pink-tinged flower buds swell on spurs along 2+ year old wood.
- Bloom (mid Mar-early Apr): Blossoms BEFORE leaves. The hero moment. Red on bare black branches. Flowers on short spurs on 2+ year old wood, orders 2-4. Not on trunk, not on current-year growth.
- Petal fall (Apr): Hanafubuki. Red dots thin on branches, scatter on ground.
- Leaf emergence (Apr-May): Overlaps late petal fall. Bronze/red-tinged new leaves turn green. Current-year shoots elongate (only growth period).
- Summer canopy (Jun-Aug): Dense dark foliage hides branch structure.
- Autumn color (Oct-Nov): Yellow to orange-bronze. Interior/lower crown first.
- Leaf drop (Nov-Dec): Reveals bare branch architecture again.

### Rendering (E-Ink Specific)

- At 800x480: trunk ~8-15px wide, primary scaffolds ~4-8px, secondary ~2-4px, twigs ~1-2px. Max useful depth ~4-6 orders.
- No anti-aliasing with 3 colors. Smooth branch paths via Catmull-Rom splines.
- Diameter tapering along segments (trapezoid, not uniform rectangle).
- Black: branches, bark, summer foliage. Red: blossoms, autumn color, petal fall. White: background.
- Painter's algorithm (back-to-front by depth) for overlapping branches.
- Dithered lines for sub-pixel branches during early growth.
