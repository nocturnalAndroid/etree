# eTree Research Plan

## Project Description

A generative art system that grows a cherry blossom tree on an e-ink display over months and years. Given a seed, the system produces a deterministic tree structure that evolves slowly through time. The growth function must be continuous (or a sufficiently fine discrete approximation of a continuous function), so that the tree's visual state at any moment `t` is a smooth, natural-looking progression from the previous state. Formally: the tree structure `S(t)` is either computed directly as `S(t) = Structure(seed, t)`, or iteratively as `S(t) = Grow(S(t-1), seed, t)`, where in either case the sequence `S(0), S(1), ... S(n)` traces a smooth, visually coherent growth trajectory for a single tree from seedling to maturity.

### Target Display
- **Resolution**: 800x480 (landscape)
- **Colors**: 3-color e-ink (white background, black branches/leaves, red blossoms). Future exploration: 4/7-color or Spectra 6, but not for MVP.
- **Refresh rate**: ~once per day. The tree grows slowly.
- **Rendering**: 2D projection of a 3D structure. Pure 2D may not look good enough but worth exploting if 3d does not pannout.
- **Aesthetic**: As realistic as the resolution allows. See reference images in `referances/`. The "more likely" reference (sparse black branches, discrete red blossom dots, visible bark line-work) is the target for MVP. Falling petals on the ground are a nice touch.
- **MVP species**: Cherry tree (Prunus) with red cherry blossoms. Other species may require implementation changes and are not a priority.

---

## Knowledge Summary (from prior research)

### 1. Branching Geometry
- **Da Vinci / Leonardo's Rule**: Cross-sectional area is conserved at branch junctions: `d_parent^n = sum(d_child_i^n)`, with `n ~ 2` for external structure (mechanical optimum).
- **Murray's Law**: `n ~ 3` governs internal vasculature (hydraulic optimum). External geometry stays close to `n = 2`; the 2-to-3 gradient is an internal phenomenon.
- **Species-tunable exponent**: Empirical range for external branching is roughly 1.8-2.3. A single species-specific constant is sufficient; no need for order-dependent gradients. **Open question**: does the exponent shift as the tree matures over its lifetime? (see Future Research below)
- **WBE Model**: Predicts metabolic scaling (M^3/4) from fractal network geometry. Also predicts the resource flux through any branch as a function of the total downstream mass it supports — could be useful for modeling growth allocation.

### 2. Core Structural Equations

**Variable Glossary:**

| Symbol | Name | Unit | Definition |
|--------|------|------|------------|
| `d` | diameter | cm | diameter of a branch cross-section (`d = 2r`) |
| `r` | radius | cm | radius of a branch cross-section |
| `L` | branch length | cm | length of a branch segment |
| `H` | tree height | m | total height from ground to crown top |
| `D` | trunk DBH | cm | trunk diameter at breast height (1.3m); `D = 2R` where `R` is trunk radius at that height |
| `R_c` | crown radius | m | horizontal radius of the crown envelope |
| `h` | height along trunk | m | vertical position on the trunk (0 = base, H = top) |
| `k` | branching order | - | 0 = trunk, 1 = primary branch, 2 = secondary, etc. |
| `t` | time | years | time since germination |
| `alpha` | allometric exponent | - | relates radius to length in allometric scaling |
| `beta` | taper exponent | - | controls trunk taper shape |
| `lambda` | length ratio | - | ratio of child branch length to parent (0 < lambda < 1) |
| `theta` | branching angle | degrees | angle between parent and child branch |
| `n_b` | branching number | - | number of child branches per node |

**Equations:**
- **Allometric scaling**: `r = r0 * (L/L0)^alpha` — relates branch radius to its length
- **Branch length decay**: `L_k = L0 * lambda^k` — each branching order is shorter by factor `lambda`
- **Branching angle**: `theta_k = theta_0 + delta_theta * k` — angle increases with order
- **Trunk taper**: `d(h) = d_base * (1 - h/H)^beta` — diameter decreases with height
- **Height-diameter**: `H = a * D^b` — tree height from trunk diameter
- **Crown radius**: `R_c = c * D^e` — crown spread from trunk diameter
- **Phyllotaxis**: Golden angle (137.5 deg), Vogel's model for spiral arrangements
- **Branches per order**: `N_k = N0 * n_b^k` — exponential branching

**Note on inputs vs. emergent properties**: Some of these equations should be used as direct model inputs (e.g., branching angle, length ratio, Da Vinci exponent), while others should *emerge* from the simulation rather than being imposed (e.g., crown radius, height-diameter relationship, overall branch count). Determining which is which will require experimentation — see Phase 1 research.

### 3. Growth Dynamics
- **Primary growth** (length): Occurs only at tips via apical meristems. Once a branch segment elongates, it stops lengthening.
- **Secondary growth** (diameter): Occurs along the entire branch via cambium. Continues every year.
- **Diameter model**: `d(t) = d0 + 2*g(t)`, where `g(t)` is cumulative ring width (linear or saturating).
- **Pipe model**: Diameter of a branch is proportional to the leaf area it supports downstream.
- **Self-pruning**: Branches die when their carbon balance (photosynthesis - respiration) goes negative, i.e. shaded interior branches are shed over time. **Open question**: can this be modeled simply (e.g., heuristic based on branch depth / local density) without full shading calculations? Or is a proper light model necessary for visual realism? (see Future Research)
- **Da Vinci ratio drift**: The area-preserving ratio degrades slightly as branches thicken uniformly; trees compensate by allocating more growth to the trunk. **Open question**: is self-pruning the mechanism by which branches effectively "lengthen"? If all but one child at a node are pruned, the node disappears and parent+child merge into one longer branch. This could be how mature trees develop their characteristic long, clean lower trunks. Needs research.

### 4. Environmental Interactions
- **Tropisms**: Gravitropism (droop), phototropism (growth toward light). Modeled as direction perturbations: `d_new = d + gamma*g_hat + tau*s_hat`.
- **Self-shading**: Beer-Lambert extinction: `I = I0 * e^(-k*LAI)`.
- **Collision avoidance**: Options include voxel grids, bounding volumes, and space colonization algorithm.

### 5. Species Parameters
A table of representative values exists for Oak, Pine, Birch, Palm, and Willow covering: taper exponent, Da Vinci exponent, length ratio, branch angle, max branching order, height-diameter coefficients, crown ratio, branches per node, divergence multiplier, and gravitropism. Cherry-specific values will need to be sourced or calibrated (see Future Research).

### 6. Modeling Approaches Identified (not yet evaluated for this project)
- **L-systems** (grammar-based recursive rewriting) — may be too simplistic on its own for the visual quality we want, but could serve as a component.
- **Pipe model** (diameter from downstream leaf area) — promising core, especially if combined with self-collision and self-shading to produce realistic crown shapes.
- **Carbon-balance model** (ecological simulation) — possibly overkill, but interesting. If a light sensor is added to the hardware, the tree could grow toward actual ambient light.
- **Full FSPMs** (GroIMP, OpenAlea) — heavyweight research tools, not suitable for this project directly.
- **Space colonization algorithm** (Runions et al. 2007) — grows a tree by placing "attractor points" in a volume representing the available growing space, then iteratively extending branches toward the nearest attractors. Each iteration: (1) each attractor is associated with its nearest branch tip, (2) tips grow a step toward their attractors, (3) attractors that are reached are consumed. This naturally produces realistic branching patterns, crown filling, and collision avoidance because branches compete for space. The resulting structure depends on the shape of the attractor volume (the crown envelope).

---

## Gaps and Open Questions

### A. Rendering and Display (Resolved)
1. **2D or 3D?** — 2D projection of a 3D structure. Pure 2D likely won't look good enough.
2. **E-ink constraints** — 3-color (white/black/red), 800x480, refresh ~1/day.
3. **Aesthetic** — As realistic as resolution allows. Target: second reference image style (sparse visible branch structure, discrete red blossom clusters, bark line-work texture).

### B. Continuity and Time Mapping (Critical)
4. **Smooth interpolation** — **High priority.** How to ensure visual continuity between daily growth steps. The existing research describes equilibrium states, not transitions. This is the central technical challenge.
5. **Time scale mapping** — Seasonal variation is desired (dormancy in winter, leafing in spring, blossoming, leaf drop in autumn). How to map calendar months to biological seasons.
6. **Direct vs. iterative computation** — Iterative is acceptable. Direct time access is not required.

### C. Algorithmic Architecture
7. **Deterministic seeding** — Nice to have, not a strict requirement. Should be straightforward unless external sensors (light, temperature) are introduced.
8. **Growth event ordering** — What is the correct order of operations per time step (elongation, branching, thickening, pruning), and does it affect visual quality?
9. **Computational cost** — Not a major concern for MVP. Computation happens once per day. Worst case: pre-simulate the full growth sequence and encode it. The algorithm should still be efficient, but this is not a blocker.

### D. Biological Fidelity vs. Artistic License
10. **Which biological details matter visually?** Self-pruning, phyllotaxis, tropisms — which subset gives the best visual payoff at 800x480?
11. **Level of detail over time** — Managing branch count explosion as the tree matures.
12. **Species** — Cherry tree (Prunus) for MVP. Multi-species support is not a priority.

---

## Future Research Topics

These are specific questions identified during the knowledge review that need targeted investigation:

1. **Exponent drift with age**: Does the Da Vinci exponent (n ~ 2) shift as a tree matures from sapling to old growth? If so, how and by how much?
2. **WBE flux model for growth allocation**: Can the WBE model's prediction of resource flux (based on downstream mass) be used directly to allocate growth in our simulation?
3. **Simple self-pruning heuristics**: What are practical, computationally cheap ways to model self-pruning without full light simulation? (e.g., branch depth threshold, local density measure, random pruning weighted by shading proxy)
4. **Branch merging via pruning**: When interior branches are pruned at a node, does the visual/structural effect simulate branch lengthening? Research how mature trees develop long unbranched trunk sections.
5. **Input vs. emergent equations**: Which structural equations should be imposed as model parameters vs. emerge naturally from simulation? Requires experimentation.
6. **Cherry tree (Prunus) specific parameters**: Branching angles, length ratios, Da Vinci exponent, crown shape, blossom timing and distribution for Prunus species.
7. **Seasonal cycle modeling for cherry**: Timing and visual appearance of: bare winter branches, spring bud break, blossom period (and petal fall), leaf-out, summer canopy, autumn leaf color/drop.

---

## Research Plan

The goal is to produce 3-4 concise, concrete modeling options, each with enough detail to evaluate and build an execution plan from.

### Phase 1: Evaluate Candidate Algorithms

Research and compare the following approaches, focusing on: smooth visual transitions, biological plausibility, and implementation complexity.

#### 1.1 L-System with Continuous Diameter Growth
- How to extend classic L-systems with smooth time-parameterized diameter growth rather than discrete generation steps.
- Can L-system derivation steps be interpolated to produce smooth visual transitions?
- How to handle the "popping" problem (sudden appearance of new branches at a generation boundary).
- Relevant work: parametric L-systems, differential L-systems.
- Concern: may be too simplistic alone. Evaluate whether it can produce the organic irregularity seen in the reference images.

#### 1.2 Space Colonization Algorithm
- Runions et al. (2007) "Modeling Trees with a Space Colonization Algorithm."
- Can the algorithm be run incrementally (a few attractor points consumed per time step) to produce gradual growth?
- How to combine with secondary growth (diameter thickening)?
- Does it naturally produce smooth visual evolution, or does it need interpolation?
- Advantage: naturally handles collision avoidance and crown filling.

#### 1.3 Direct Parametric Model (No Simulation)
- Define the tree's topology and geometry as closed-form functions of time: `branch_count(t)`, `branch_i_length(t)`, `branch_i_diameter(t)`, `branch_i_angle(t)`.
- Each function is smooth (e.g., logistic curves for length, linear/saturating for diameter).
- Topology changes (new branches appearing) handled by having branches "exist" from time 0 but with length 0, smoothly growing in.
- Tradeoff: easy to compute `S(t)` directly, but harder to capture emergent structure.

#### 1.4 Hybrid: Parametric Skeleton + Rule-Based Detail
- Use a parametric model for the main trunk and primary branches (deterministic, smooth, cheap to compute).
- Use L-system or stochastic rules for higher-order branching, applied as detail layers.
- Pipe model for diameter computation (downstream leaf area determines thickness).
- Self-shading or density heuristics for pruning to produce realistic crown thinning.
- Two-phase rendering: structural skeleton is always smooth; detail branches appear gradually.

### Phase 2: Prototype and Compare (after Phase 1 review)

For the 1-2 approaches selected from Phase 1, build minimal working prototypes that demonstrate:
- Growth from seedling to mature cherry tree
- Smooth visual transition across at least 20 time steps
- Output as a sequence of images (black branches + red blossoms on white, 800x480)

### Phase 3: Refinement Research (as needed)

Based on prototype results, research specific aspects as needed:
- Cherry blossom rendering (petal clusters, falling petals)
- Seasonal cycle visual effects
- Cherry-specific parameter calibration
- Bark texture within e-ink constraints

---

## Proposed Deliverable from Phase 1

A document comparing the 4 candidate approaches across these criteria:

| Criterion | Why it matters |
|-----------|---------------|
| Visual smoothness | Core requirement: no jarring jumps between daily updates |
| Biological plausibility | Does it look like a real cherry tree? |
| Implementation complexity | Development effort and maintainability |
| Artistic control | Can the tree's character be tuned via parameters? |
| Seasonal support | How naturally does the approach accommodate seasonal cycles? |

Each approach will include: a description, pseudocode, pros/cons, and a recommendation for which to prototype.
