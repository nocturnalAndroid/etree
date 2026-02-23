# eTree — Complete Knowledge Base and Execution Plan

This document combines all settled research and the concrete execution plan into a single file formatted for text-to-speech.

---

# Part One: The Project

We are building a generative art system that grows a cherry blossom tree on an e-ink display. The display is 800 by 480 pixels with three colors: white for the background, black for branches and foliage, and red for blossoms. The display updates about once per day.

The tree grows slowly over months and years, from a seedling to a mature cherry tree. Each day the viewer sees a tiny bit of change. The target aesthetic is visible in the project's reference images: sparse black branches with visible bark texture, discrete clusters of red blossoms, and a clean white background. Think Japanese ink drawing with a splash of red.

The MVP species is cherry, specifically ornamental Prunus like Yoshino or Japanese flowering cherry. Other species may come later but are not a priority.

---

# Part Two: How Real Trees Grow

## Two Kinds of Growth

Trees grow in two fundamentally different ways.

The first is called primary growth, or elongation. This happens only at the tips of branches, in structures called apical meristems. When a branch segment finishes elongating, it stops getting longer permanently. A twig that grew to ten centimeters last spring will still be ten centimeters long a decade from now. It does not stretch. Cherry trees have what is called determinate growth, meaning each shoot does most of its elongation in a short spring flush lasting just a few weeks, then stops and forms a terminal bud for the following year. The shape of this growth curve is asymmetric: fast acceleration at first, then a long slow approach to the final length. Botanists model this with a Gompertz function.

The second kind is called secondary growth, or thickening. This happens along the entire length of every living branch, every year, via a thin layer of dividing cells called the cambium. The cambium adds a new ring of wood each growing season, which is why you can count rings to determine a tree's age. A branch that stopped elongating ten years ago is still getting thicker every year. Young branches add thick rings, old branches add thinner ones. Importantly, diameter never decreases. Wood does not un-grow.

This means that in a growing tree, the tips are where new length appears, but the entire existing structure is slowly getting fatter. For the display, this matters: even on days when no new branches sprout, the existing branches are imperceptibly thickening, providing subtle visual change.

## Buds and Branching

Buds form along new shoots in a spiral arrangement. The angle between successive buds is close to 137.5 degrees, known as the golden angle. The following spring, some of these buds activate and extend into new shoots.

Not all buds activate. The terminal bud, the one at the very tip, is strongly favored. This is called apical dominance: the terminal bud chemically suppresses lateral buds below it. Lateral buds activate with lower probability, and this probability decreases the farther the bud is from the tip. Light availability at the bud's position also affects whether it activates. Buds in well-lit positions are more likely to grow; buds in shade tend to stay dormant.

In young cherry trees, apical dominance is strong, producing a clear central leader, a single dominant trunk that grows upward. As the tree ages, apical dominance weakens. The central leader loses its advantage, lateral branches begin to dominate, and the tree transitions from a narrow pyramidal shape to the wide spreading form characteristic of mature cherry trees. Botanists call this transition from an excurrent habit to a decurrent habit.

## The Pipe Model and Branch Diameter

There is an elegant principle called the pipe model, proposed by Shinozaki in 1964. Imagine that every leaf on the tree is connected to the roots by one continuous pipe of water-conducting tissue. The cross-sectional area of any branch is simply the total area of all the pipes running through it, which equals the number of leaves that branch supports multiplied by the area of one pipe.

In practice this means: the trunk, which supports every leaf on the tree, is the thickest. A scaffold branch supporting a quarter of the leaves has about a quarter of the trunk's cross-sectional area. A tiny twig supporting one leaf cluster is very thin. The taper from trunk to twig is not arbitrary. It is a direct consequence of how much leaf area sits downstream.

This is closely related to Leonardo da Vinci's observation that at any branch junction, the cross-sectional area of the parent roughly equals the sum of the cross-sectional areas of all the children. The exponent in this relationship is approximately two, meaning it is area-preserving. Across many species, the measured exponent falls between about 1.8 and 2.3. For cherry trees, a value of 2.0 is appropriate. Research confirmed that this exponent barely changes as a tree ages, shifting by only about 0.1 to 0.2 over an entire lifetime from sapling to old growth. For our purposes, it is a constant.

## Self-Pruning

One of the most important processes for visual realism is self-pruning. A branch costs carbon to maintain. The wood of a branch requires ongoing respiration, and the cost scales with the branch's volume, roughly its length times its diameter squared. If a branch cannot photosynthesize enough to cover this cost, it dies.

Interior branches are shaded by the outer canopy, so they are the first to go. In a forest, this is dramatic: competition between neighboring trees pushes the live crown upward and the lower trunk becomes completely bare. In an open-grown ornamental cherry, the process is slower and more subtle. The tree gets light from all sides, so it retains its lower branches for a long time. Pruning mainly affects the deep interior of the crown.

## Branch Merging: The Key Structural Insight

Here is the insight that governs the entire design of the simulation. When all the child branches at a junction die except one, the surviving branch effectively merges with its parent into a single longer segment. The dead stubs eventually rot away, the cambium grows over the wound, and what used to be a branching node becomes a point along a continuous branch. The former junction leaves behind a slight change in direction, a kink, that gives the branch its organic character.

This is how mature trees develop their characteristic long, clean scaffold branches and bare lower trunks. The botanical literature calls this natural pruning, self-pruning, crown recession, or bole cleaning.

The critical consequence for our model is this: there are no scaffold branches at birth. What we see as the defining scaffold branches of a mature tree are simply the winners of decades of competition. Every branch starts the same way, as a bud that activated. The large-scale structure of the tree, how many scaffolds there are, at what angles they spread, how long they are, is entirely an emergent product of growth, self-pruning, and competition. We do not pre-design the scaffold. We simulate all branches uniformly and let the structure emerge on its own.

This is the central design principle of the project.

## Tropisms

Tropisms are directional growth responses.

Gravitropism means the direction of growth is influenced by gravity. In practice, branches are pulled downward proportional to their accumulated length and weight. Cherry branches characteristically arch outward and then droop slightly under their own weight. Young upright shoots gradually develop a graceful curve over the years.

Phototropism means growth is biased toward light, or more generally toward open space. In the simulation, this translates to a tendency to grow away from crowded regions and toward areas with low branch density. Combined with gravitropism, this produces branches that spread outward and upward to fill the available growing space.

---

# Part Three: Cherry Trees Specifically

## Physical Form

Ornamental cherry trees are moderate-sized deciduous trees. At maturity they reach about 8 to 12 meters tall with trunk diameters of 30 to 60 centimeters. Their defining visual feature is a wide, spreading crown that is typically wider than it is tall, with a width-to-height ratio of about 1.3 to 1.8. The overall shape is often described as vase-shaped, spreading, or umbrella-like.

The trunk forks relatively low, at roughly 25 percent of the total height, into 3 to 5 main scaffold branches. These scaffolds spread outward at wide angles, typically 45 to 70 degrees from vertical, much wider than many other tree species.

Cherry trees have an alternate branching pattern, meaning branches emerge one at a time along the parent in a spiral arrangement at the golden angle. The length ratio between parent and child branches is about 0.65 to 0.75, meaning each generation of branches is about two-thirds to three-quarters as long as its parent. The maximum branching order is typically 5 to 7.

## How the Shape Changes Over a Lifetime

A young cherry tree in its first ten years has a strong upright central leader, a narrow pyramidal crown, and branches ascending at 30 to 45 degrees from vertical. Branching is dense throughout.

A maturing tree between 10 and 25 years shows the crown broadening significantly. The central leader weakens as the tree adopts a decurrent growth habit. Scaffold branches begin arching outward. Self-pruning starts thinning the interior.

A mature tree over 25 years old has a broad spreading crown, long clean scaffolds with kinks from former junctions, and foliage concentrated at the periphery. The interior is sparse. This is the form seen in the reference images.

## The Seasonal Cycle

The annual cycle of a cherry tree is one of the most visually dramatic of any species, and it maps beautifully to the three-color display.

Winter dormancy runs from about mid-November through mid-February. The tree is completely bare. All branch architecture is visible as a stark silhouette, dark branches against a light background. This is when the tree looks most like a Japanese ink drawing.

Bud swell happens from late February through mid-March. Rounded flower buds on short spurs along the older branches become visibly swollen and take on a pink tinge.

Bloom is the hero moment. It typically runs from mid-March through early April, lasting about 5 to 10 days at peak. The critical fact is that blossoms appear before the leaves. For one to two weeks, the tree is covered in flowers on bare branches. Each flower cluster grows on a short spur attached to wood that is at least two years old. The clusters are distributed along the length of established branches, not just at tips, creating lines of bloom that trace the branch architecture. On the display, this produces the perfect image: black branch skeleton plus bright red blossom clusters on white background.

Petal fall follows bloom by about a week. Petals drop, creating the famous cherry blossom snow. On the display, red dots gradually thin on the branches while red dots appear scattered on the ground below.

Leaf emergence overlaps with late petal fall. Small leaves unfurl from buds on current-year shoots. Within two to three weeks, leaves are fully expanded.

The spring growth flush runs from April through May. This is the only time branches actually get longer. Vigorous shoots can extend 15 to 40 centimeters. The crown fills in with foliage.

Summer canopy lasts from June through August. The crown is a solid mass of dark foliage. Branch structure is completely hidden.

Autumn color happens in October and November. Leaves turn yellow to orange-bronze. The color progresses from the interior and lower crown outward.

Leaf drop runs from November through early December. Leaves fall, progressively revealing the branch architecture again. The tree returns to its bare winter silhouette.

One essential detail for the model: blossoms appear on short spurs on wood that is at least two years old, concentrated on branch orders two through four. They do not appear on the trunk, on primary scaffolds, or on current-year growth. Young branches must age for at least one full year before they can flower.

---

# Part Four: The Execution Plan

## Core Design Principle

All branches are born equal. Structure emerges.

There are no scaffold branches and no detail branches. A bud activates, a shoot extends, and the branch either survives to become part of the tree's defining structure, or it dies and is absorbed. The trunk forks because one junction's competitors were pruned. A major branch has a kink because there used to be a branching node there. The entire large-scale architecture is an emergent product of three forces: growth, self-pruning, and collision avoidance.

The simulation runs in 3D model space. Rendering to the display is a separate concern handled later. Performance is not a constraint for now. Use as much compute as needed to get the tree right.

## How the Tree is Represented in Code

The tree is a mutable graph of nodes and segments. Each node is a point in 3D space where something happens: a branch tip, a junction between parent and children, or a bud site. Each segment is an edge connecting two nodes, representing a piece of branch. A segment stores its length, diameter, direction, age, and status.

Each node can hold dormant buds, which are potential future branches waiting to activate. Each bud has a phyllotactic angle (its rotational position in the golden-angle spiral) and a vigor score.

The tree also maintains a 3D voxel grid, a coarse division of space into small cubes. This grid tracks which regions of space are occupied by branches and how densely. It serves double duty: estimating light availability for pruning decisions, and detecting crowding for collision avoidance.

When branch merging occurs, two segments on either side of a now-single-child node are combined into one longer segment. The removed node's 3D position is kept as an internal waypoint on the merged segment, preserving the kink that gives the branch its character.

## The Simulation Loop

Each iteration of the simulation represents one growing season, approximately one year. Within each season, five phases execute in order.

### Phase A: Bud Activation and Shoot Extension

This is the spring growth phase. For every living branch tip that has dormant buds, each bud is scored for how likely it is to activate. The score depends on apical dominance, meaning the terminal bud gets the highest score and lateral buds are suppressed in proportion to their distance from the tip. It also depends on the light estimate at the bud's position, queried from the voxel grid, so buds in the interior are naturally suppressed. It depends on vigor, meaning thick well-fed branches can activate more buds than thin struggling ones. And it includes random noise from a seeded random number generator for natural variation.

Buds are then activated probabilistically based on these scores. Each activated bud becomes a new shoot.

Each new shoot gets an initial direction: the parent's direction rotated by the branching angle and the bud's phyllotactic rotation. This direction is then modified by several biases. Gravitropism pulls the direction slightly downward, more so for older and heavier branches. Phototropism biases the direction toward open space, computed as the gradient of the density field in the voxel grid, meaning grow away from crowded regions. If collision avoidance is active, the direction is also pushed away from occupied voxels nearby.

The shoot's length depends on the parent's vigor times a species growth rate times random noise. For cherry trees, vigorous shoots grow 15 to 40 centimeters, weak shoots only 2 to 10 centimeters.

Finally, new buds form along each new shoot at golden-angle intervals, roughly one bud per 3 to 8 centimeters. These buds are dormant until the next season.

### Phase B: Secondary Growth

This is the diameter update phase. First, the pipe model is computed bottom-up, from tips to root. Each terminal branch contributes a base leaf area. At each junction, children's downstream leaf areas are summed. Diameter is set proportional to the square root of the total downstream leaf area.

Second, every living segment gets slightly thicker from cambial growth. The annual ring width decreases with the segment's age: fast thickening when young, slowing as it ages.

Third, the non-shrinking rule: a segment's diameter is the maximum of the pipe model result and its previous diameter plus the ring width. When pruning removes downstream branches, the pipe model gives a lower number, but the existing wood remains. The branch just stops getting the extra growth boost.

### Phase C: Self-Pruning

Light is estimated for each branch segment using a heuristic. The plan is to start with the simplest heuristic and escalate only if needed (more on this below).

For each branch, the simulation compares estimated photosynthetic income against respiratory cost. Income is proportional to the light score times the leaf area. Cost is proportional to the wood volume, which scales with length times diameter squared. A branch running a deficit accumulates stress. After two or three consecutive years of deficit, depending on tuning, the branch dies.

The death process is gradual. Dying branches lose their leaf area over one to two seasons, not instantly, to avoid sudden diameter jumps in ancestor branches through the pipe model. Dead branches persist as stubs for one to three years, because real dead branches do not vanish instantly. After the stub period, the dead segment is removed from the tree graph.

### Phase D: Branch Merging

After dead segments are removed, the simulation checks all nodes. If a node has exactly one surviving child segment and one parent segment, those two segments are merged into one. The node's 3D position is retained as an internal waypoint, creating the characteristic kink. The merged segment inherits the parent's base diameter and the child's tip properties.

Over many simulated years, this process transforms a densely branched young tree into a mature tree with a few long, characterful branches. Each branch bears the directional memory of its pruning history through its kinks and curves.

### Phase E: Update the Spatial Grid

The 3D voxel grid is rebuilt from the current tree state. Every voxel is cleared, then for each living segment, the voxels it passes through are marked with a branch presence flag, cumulative branch volume, and cumulative leaf area. This grid is then available for the next season's bud activation, growth direction decisions, and pruning calculations.

## Pruning Strategy: Start Simple, Escalate If Needed

There are four levels of pruning heuristic, from cheapest to most sophisticated.

Level one is crown depth. For each branch, compute its distance from the nearest point on the crown surface, which is a convex hull or fitted ellipsoid around all branch tips. Normalize by the crown radius. Branches deeper inside the crown are more likely to die. This is the cheapest to compute and should be tried first.

The test is: does the interior thin over time? Does the trunk region clear? Do the surviving branches form a plausible cherry silhouette? If the result is too uniform, with all interior branches dying at the same rate and no spatial variation, escalate to level two.

Level two adds local density awareness from the voxel grid. Count occupied voxels in a neighborhood around each branch. High local density means more competition and higher death probability, even if crown depth is moderate. Low local density means the branch found a gap and can survive even if somewhat interior.

The test is: do sparse regions within the crown retain branches? Do dense clusters thin unevenly? If the vertical structure is wrong, for example lower branches dying too fast or too slow, escalate to level three.

Level three is the shadow cone. For each branch, look upward and count how much branch and leaf mass lies above it, between the branch and the sky. This can be done cheaply using the voxel grid by summing occupied voxels in a column above the branch. Branches under heavy canopy die; branches with open sky survive.

Level four is the last resort: cast multiple rays from each branch toward the sky hemisphere, walking through the voxel grid and accumulating occlusion using exponential extinction. This is the most accurate heuristic short of full ray tracing but also the most expensive. It should only be needed if levels one through three produce visually inadequate results, which is unlikely.

## Collision Avoidance: Also Start Simple

There are three levels of collision avoidance.

Level one is no explicit collision avoidance at all. Instead, rely entirely on self-pruning. Branches that end up in crowded positions will have low light scores and die. Transient overlaps are allowed during growth; they resolve within a few seasons as the losers are pruned. This is the most biologically accurate approach and should be tried first.

The test is: do branches visually overlap in the 2D rendering? If overlaps occur, are they transient? If persistent overlaps remain after pruning, escalate to level two.

Level two is soft repulsion. When extending a new shoot, query nearby voxels. If there are occupied voxels close to the growth path, add a gentle repulsive bias to the direction vector, pushing the shoot away from occupied space. This naturally combines with phototropism since both push growth toward open areas.

The test is: do branches curve around each other gracefully, or does the repulsion produce unnatural artifacts like weird S-curves? If it looks artificial, reduce the repulsion strength and lean back toward level one.

Level three is hard voxel occupancy. Voxels occupied by branches are marked as solid, and new shoots simply cannot extend into them. This guarantees no overlap but may produce blunt terminations or unnatural redirection. Use only if absolutely necessary.

## Implementation Steps

The plan is broken into eight steps. Each step is independently testable. The simulation is evaluated visually at every step before moving on.

### Step One: Minimal Viable Growth

Build the tree data structure, implement bud activation with apical dominance only, implement shoot extension with fixed directions and no tropisms, and implement the pipe model for diameter computation. Add a simple 2D rendering, just an orthographic side view with line thickness representing diameter.

Run for about five simulated years. Evaluate: does it look like a young sapling with a dominant central leader and smaller laterals? Are the diameter proportions right, with the trunk thickest and sub-branches thinnest? Is the branch count reasonable?

If branching is too dense or too sparse, tune bud spacing and activation probability. If the trunk does not dominate, increase apical dominance strength.

### Step Two: Add Tropisms

Implement gravitropism as a directional bias toward gravity, proportional to segment length and weight. Implement simplified phototropism as an upward and outward bias, just a preference for growing away from the trunk center. No voxel grid yet.

Evaluate: do branches arch outward in a cherry-like spreading pattern? Does the tree transition from upright leader to spreading form over about ten years?

If branches droop too much, reduce gravitropism. If the tree is too columnar, increase branching angles or reduce apical dominance with age.

### Step Three: Add Self-Pruning

Implement crown surface estimation using a convex hull or ellipsoid fit to the branch tips. Implement the level one pruning heuristic based on crown depth. Implement carbon balance with stress accumulation. Implement the death process with gradual leaf loss, stub persistence, and eventual removal. Implement branch merging: detect nodes with a single surviving child, merge the segments, retain waypoints for kinks.

This is the most critical step. Evaluate at simulated years five, ten, twenty, and thirty. Does the interior thin over time? Do long clean scaffold branches emerge from the merging process? Do the merged branches have characterful kinks? Compare the progression from young and bushy to middle-aged with emerging scaffolds to mature with clean structure against reference photos of real cherry trees.

If pruning is too aggressive, reduce the death rate. If too weak, increase sensitivity to crown depth. If the wrong branches survive, escalate the pruning heuristic to level two.

### Step Four: Add the Voxel Grid and Collision Avoidance

Implement the 3D voxel grid, starting at about five centimeters per voxel. Rebuild it after each season. Upgrade bud activation to account for local density. Optionally add soft repulsion during shoot extension, or skip it if pruning alone handled collisions adequately in step three. Optionally upgrade the pruning heuristic to level two if needed.

Evaluate: does the crown fill space more evenly? Are buds in crowded regions suppressed? In the 2D projection, are there obvious overlaps?

If voxel resolution is too coarse, increase it. If too fine, decrease. If repulsion creates artifacts, dial it back.

### Step Five: Tune for Cherry Tree

Apply cherry-specific parameters: branching angles of 45 to 65 degrees, length ratio around 0.7, age-dependent apical dominance that is strong when young and weak when mature, and a target crown width-to-height ratio of about 1.4. Run the simulation for 30 to 50 tree years.

Compare silhouettes against reference photos. Check: does the trunk fork low? Do three to five main scaffolds emerge? Is the crown wider than tall at maturity?

This step is pure parameter tuning. Expect many iterations. If the structure is fundamentally wrong rather than just needing parameter adjustments, revisit steps three and four.

### Step Six: 3D Rendering and Camera

Implement 3D to 2D projection with a fixed camera from the side. Render branches as tapered line segments whose thickness corresponds to projected diameter. Use Catmull-Rom spline interpolation through the waypoints at former junction kinks for smooth curves. Use a painter's algorithm with depth sorting for correct overlap.

Evaluate: does the 2D rendering look like the reference images? Are branch widths reasonable? Does spline interpolation produce natural curves?

If the 3D projection produces a confusing tangle, try a narrower field of view, a slight camera rotation, or explore 2D simulation as a fallback.

### Step Seven: Seasonal Cycle

Implement the seasonal visual layer, separate from structural growth. Blossom rendering as red dots and clusters on spurs along branches that are at least two years old, peaking in spring. Leaf rendering as a dark canopy in summer, ramping up in spring and down in autumn. Petal fall as scattered red dots drifting to the ground. Bare branches as the default winter state.

Map simulation years to display time, either one tree year per real year for maximum atmosphere, or faster for testing. Map daily updates within a year to the appropriate seasonal visual state.

### Step Eight: Time Interpolation for Daily Updates

Between the annual simulation steps, produce smooth daily frames. The options are: simulate at sub-annual resolution, such as monthly steps, for more accuracy but more computation; or interpolate between annual snapshots, where shoots extend gradually over spring days and diameters change smoothly.

Evaluate: are day-to-day changes small enough to feel imperceptible as individual jumps? If not, switch to sub-annual simulation during the active growth season.

## Open Questions to Resolve During Implementation

Several design parameters cannot be determined in advance and will need to be tuned through experimentation.

What voxel grid resolution works best? Start at five centimeters and adjust.

How fast should apical dominance decay with age? This controls the transition from pyramidal sapling to spreading mature tree, and cherry-specific timing needs experimentation.

How many consecutive deficit years before a branch dies? Start at two to three and tune based on how quickly the interior thins.

How long should dead stubs persist visually? This affects realism during the messy middle-age phase.

How much should the kinks at former junctions be smoothed during rendering? Too smooth loses character, too sharp looks broken.

Should the simulation be 3D or could 2D work? If 3D projection at 800 by 480 pixels produces a confusing tangle, 2D simulation in a plane is the fallback. Evaluate at step six.

What is the right mapping from tree years to real time? One tree year per real year is the most atmospheric, but one tree year per real month lets you see maturity in two to three years. This could be made configurable.
