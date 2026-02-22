# eTree Research Findings — Detailed Report

This document presents all findings from the Phase 1 research in a format suitable for listening. Mathematical relationships are described in plain language rather than notation.

---

## Part One: How Real Trees Grow

### The Two Kinds of Growth

Trees grow in two fundamentally different ways, and understanding this distinction is essential for the model.

The first kind is called primary growth, or elongation. This happens only at the tips of branches, in structures called apical meristems. When a branch segment finishes elongating, it stops getting longer permanently. A twig that grew to ten centimeters in the spring of 2024 will still be ten centimeters long in 2030. It does not stretch.

The second kind is called secondary growth, or thickening. This happens along the entire length of every branch, every year, via a thin layer of dividing cells called the cambium. The cambium adds a new ring of wood each growing season. This is why you can count rings to age a tree. A branch that stopped elongating ten years ago is still getting thicker every year.

This means that in a growing tree, the tips are where new length appears, but the entire existing structure is slowly getting fatter. For the display, this is important: even on days when no new branches sprout, the existing branches are imperceptibly thickening, providing subtle visual change.

### How Branches Know How Thick to Be

There is a beautiful principle called the pipe model, proposed by Shinozaki in 1964. It says: imagine that every leaf on the tree is connected to the roots by one continuous pipe of water-conducting tissue. The cross-sectional area of any branch is simply the total area of all the pipes running through it, which equals the number of leaves that branch supports multiplied by the area of one pipe.

In practice this means: the trunk, which supports every leaf on the tree, is the thickest. A major scaffold branch that supports, say, a quarter of the leaves, has about a quarter of the trunk's cross-sectional area. A tiny twig supporting one leaf cluster is very thin. The taper from trunk to twig is not arbitrary — it is a direct consequence of how much leaf area sits downstream.

This is closely related to Leonardo da Vinci's observation, sometimes called da Vinci's rule or Leonardo's rule, which says that at any branch junction, the cross-sectional area of the parent branch roughly equals the sum of the cross-sectional areas of all the children. The exponent in this relationship is approximately two, meaning it is area-preserving. Empirically, across many species, the exponent falls between about 1.8 and 2.3. For cherry trees, a value of about 2.0 is appropriate.

An important question was whether this exponent changes as a tree ages. The research found that the shift is small — about 0.1 to 0.2 over a tree's entire lifetime, from sapling to old growth. At the resolution of our display, this is visually negligible, so using a fixed value of 2.0 is fine.

### How Branches Decide Where to Grow

Real branch elongation in cherry trees follows a pattern called determinate growth. In spring, a bud that formed the previous year opens and rapidly extends into a new shoot. Most of the elongation happens in a short spring flush lasting a few weeks, then the shoot stops growing and forms a terminal bud for next year. The growth curve looks like a lopsided S-shape: fast acceleration at first, then a long, slow approach to the maximum length. Mathematically, botanists model this with a Gompertz curve, which captures exactly this asymmetric sigmoid behavior.

For diameter thickening, the growth is more gradual and approximately linear for long periods, then slowly saturating. A young branch adds thick rings; an old branch adds thinner ones.

### Self-Pruning and How Trunks Get Long

One of the most important processes for visual realism is self-pruning. Interior branches that are shaded by the outer canopy eventually die because they cannot photosynthesize enough to cover their own respiratory costs. The wood of a branch costs carbon to maintain — the bigger the branch, the more it costs. If it does not receive enough light to pay for itself, it dies.

In a forest, this is dramatic: competition between neighboring trees pushes the live crown upward, and the lower trunk becomes completely bare. In an open-grown ornamental cherry, the process is slower and more subtle — pruning mainly affects the deep interior of the crown, not the lower branches, because an isolated tree gets light from all sides.

Here is an insight that came from the research: when all but one child branch at a junction die, the surviving branch effectively merges with its parent into a single longer segment. The dead stubs eventually rot away, and the cambium grows over the wound, producing smooth bark. This is how mature trees develop their characteristic long, clean scaffold branches and bare lower trunks. The botanical literature calls this natural pruning, self-pruning, crown recession, or bole cleaning. For the model, implementing this branch merging when siblings are pruned will produce the correct progression from a densely branched young tree to a cleanly structured mature one.

### The Pipe Model Connection to Growth Allocation

The WBE model, proposed by West, Brown, and Enquist in 1997, extends the pipe model into a full theory of metabolic scaling. For our purposes, the actionable insight is simple: the resource flux through any branch, meaning the water and nutrients flowing through it, is proportional to the total leaf mass that branch supports downstream. This can be used directly for growth allocation: branches supporting more leaves get more resources and thicken faster. In practice, the pipe model and the WBE prediction give the same result for diameter computation, so we use the simpler pipe model.

---

## Part Two: Cherry Trees Specifically

### Physical Characteristics

Ornamental cherry trees, including Prunus serrulata, known as Japanese flowering cherry, and Prunus yedoensis, known as Yoshino cherry, are moderate-sized deciduous trees. At maturity they reach about 8 to 12 meters tall with trunk diameters of 30 to 60 centimeters. Their defining visual feature is a wide, spreading crown that is typically wider than it is tall, with a width-to-height ratio of about 1.3 to 1.8. The overall shape is often described as vase-shaped, spreading, or umbrella-like.

The trunk forks relatively low, at roughly 25 percent of the total height, into 3 to 5 main scaffold branches. These scaffold branches spread outward at wide angles, typically 45 to 70 degrees from vertical, much wider than many other tree species. Secondary branches emerge along the scaffolds and further divide into tertiary branches and short flowering spurs.

Cherry trees have an alternate branching pattern, meaning branches emerge one at a time along the parent in a spiral arrangement, with the angle between successive branches close to 137.5 degrees, the golden angle. The length ratio between parent and child branches is about 0.65 to 0.75, meaning each generation of branches is about two-thirds to three-quarters as long as its parent. The maximum branching order is typically 5 to 7.

### The Seasonal Cycle

The annual cycle of a cherry tree is one of the most visually dramatic of any tree species, and it maps beautifully to the three-color e-ink display.

Winter dormancy runs from about mid-November through mid-February. The tree is completely bare. All branch architecture is visible as a stark silhouette — dark branches against a light background. This is when the tree looks most like a Japanese ink drawing, and it should be a beautiful state on the display.

Bud swell happens from late February through mid-March. Rounded flower buds on short spurs along the older branches become visibly swollen and take on a pink tinge. The branch silhouette becomes slightly fuzzy. On the display, this could be shown as tiny red dots appearing along branches.

Bloom is the hero moment, typically mid-March through early April, lasting about 5 to 10 days at peak. This is critical: the blossoms appear before the leaves. For one to two weeks, the tree is covered in flowers on bare branches. Each flower cluster grows on a short spur attached to wood that is at least two years old. The clusters are distributed along the length of established branches, not just at tips, creating lines of bloom that trace the branch architecture. On the three-color display, this is the perfect moment: black branch skeleton plus bright red blossom clusters on a white background.

Petal fall follows bloom by about a week. Petals drop, especially in wind, creating what the Japanese call hanafubuki, or cherry blossom snow. Fallen petals scatter on the ground beneath the tree. On the display, red dots gradually thin on the branches while red dots appear scattered on the ground — a very atmospheric transition.

Leaf emergence overlaps with late petal fall. Small green leaves unfurl from pointed leaf buds on current-year shoots. New leaves are often bronze or reddish-tinged before turning green. Within two to three weeks, leaves are fully expanded.

The spring growth flush runs from April through May. This is the only time branches actually get longer. Current-year shoots can extend 15 to 40 centimeters in a vigorous year. The crown fills in with foliage.

Summer canopy lasts from June through August. The crown is a solid mass of dark foliage. Branch structure is completely hidden. On the e-ink display with only black and white available for foliage, the crown could be rendered as a solid dark mass or stippled texture, with only the trunk and major scaffolds visible below.

Autumn color happens in October and November. Cherry leaves turn yellow to orange-bronze, with the color progressing from the interior and lower crown outward. On the display, the red channel could be reused for colored leaves.

Leaf drop runs from November through early December. Leaves fall, progressively revealing the branch architecture again. The tree returns to its bare winter silhouette, completing the cycle.

The key structural insight for the model: blossoms appear on short spurs on wood that is at least two years old, concentrated on branch orders 2 through 4. They do not appear on the trunk or primary scaffolds. They do not appear on current-year growth. Young branches must age for at least one full year before they can flower. This biological detail is important for visual accuracy — blossoms should trace the older interior branching, not the newest twig tips.

---

## Part Three: The Four Candidate Approaches

### Approach One: L-Systems with Continuous Growth

L-systems are grammar-based rewriting systems invented by Aristid Lindenmayer in 1968. The basic idea is that you start with a simple string of symbols and repeatedly apply replacement rules. For example, the symbol F might mean "draw a line segment," and a rule might say "replace F with F, open bracket, plus, F, close bracket, F." The brackets mean "save your position, turn left, draw, then return to the saved position." Applying this rule repeatedly produces branching fractal structures.

For tree modeling, parametric L-systems extend this by attaching numbers to each symbol. A branch segment carries its length, diameter, and age as parameters. Rules can include conditions, so a branch only spawns children when its age exceeds a threshold.

The key question for our project was whether L-systems can produce smooth, continuous growth rather than discrete jumps between generations. The answer is yes, but it requires several techniques layered together.

First, every geometric property, including length, diameter, and angle, should be a smooth function of the module's age, not a constant assigned at creation. A branch created today starts with zero length and gradually extends via a sigmoid curve.

Second, branching events should be triggered by individual age thresholds with random variation rather than by global generation steps. This prevents all branches at the same depth from appearing simultaneously.

Third, buds should exist as dormant, invisible modules before they activate. This provides structural continuity — the bud was always there, it just had not started growing yet.

Fourth, diameters should be computed bottom-up using the pipe model as a post-processing step after each time step, rather than being baked into the L-system rules.

The strengths of L-systems are that they are extremely well-documented, with implementations like L-Py in Python and the full textbook "The Algorithmic Beauty of Plants" freely available online. They offer fine-grained control over branching patterns through rule parameters.

The main weakness is that L-systems are fundamentally recursive grammars, and this produces a subtly self-similar regularity that can look algorithmic. Getting organic irregularity requires stochastic rules, context-sensitive rules, environmental interaction, and gravitropic bending, all adding complexity. The mapping from rule parameters to visual outcome is indirect, making tuning an iterative process.

### Approach Two: Space Colonization Algorithm

The space colonization algorithm, introduced by Adam Runions and Przemyslaw Prusinkiewicz in 2007, takes a completely different approach. Instead of generating branches from rules, it grows branches toward targets.

The algorithm works as follows. You define a volume representing the desired crown shape and fill it with randomly placed attractor points, typically hundreds or thousands of them. You place one or more initial tree nodes at the base. Then you iterate.

In each iteration, three things happen. First, each attractor point finds the closest tree node within a maximum influence distance. Second, for each tree node that has attracted at least one point, you compute an average direction toward all its attractors and extend the tree by one step in that direction, creating a new node. Third, any attractor point that now has a tree node within a minimum kill distance is removed — it has been colonized.

The process repeats until all attractors are consumed or no attractors remain within range of any node.

The critical insight for our project is that this algorithm is inherently incremental. You can run just one or two iterations per daily update. The tree grows a little bit each day, with tips extending by one step size in the direction of their attractors. The state — consisting of the current tree nodes, their connections, and the remaining attractors — persists between updates. This produces naturally smooth, gradual growth.

The ratio between the kill distance and the influence distance controls the branching density. A small kill distance relative to the influence distance means attractors persist for many iterations, producing dense, fine branching. A large kill distance means attractors are consumed quickly, producing sparse branching with longer segments between forks.

The crown envelope directly controls the tree's shape. For a cherry tree, a wide, flattened ellipse produces the characteristic spreading crown. The envelope can expand over time to simulate the tree growing larger.

The strengths of this approach are substantial. Self-avoidance is automatic — branches compete for the same finite attractor points, so they naturally curve away from each other instead of overlapping. Crown filling is guaranteed — branches grow toward any remaining uncolonized space. Asymmetry is natural because the random attractor placement creates unique, irregular trees every time. These are exactly the properties that are hardest to achieve with L-systems.

The weaknesses are that fine control over branching patterns is limited. You cannot easily specify that branches should emerge at specific angles or in specific phyllotactic patterns — the structure emerges from the attractor geometry. Cherry-specific features like the low fork, spur distribution, and the blossoms-before-leaves timing require additions beyond the core algorithm. Also, no existing implementation handles the persistent, daily-update growth pattern our project needs.

Branch diameters are not produced by the algorithm and must be added separately using the pipe model or da Vinci's rule, computed bottom-up from leaf tips to trunk. Age-based cambium thickening can be layered on top. Similarly, tropisms like gravitational droop must be added as a bias in the direction computation.

### Approach Three: Direct Parametric Model

This is the most mathematically straightforward approach. The entire tree — every branch that will ever exist — is defined once at initialization from a seed. Each branch is specified with a parent, an attachment point along the parent, an activation time, a maximum length, a growth rate, a branching angle, and a curvature parameter. At any time, the tree's appearance is computed by simply evaluating smooth functions for each branch. There is no simulation, no iteration over time, and no stored state beyond the frozen topology.

Branch lengths follow Gompertz curves: starting at zero, accelerating quickly, then tapering off toward a maximum. This matches the real growth pattern of cherry branches, which do most of their elongation in a short spring flush. The Gompertz curve has the desirable property of asymmetric acceleration — growth starts fast and decays slowly, producing a natural-looking unfurling.

New branches appear by growing from zero length. A branch with an activation time of day 200 has zero length on day 199 and begins its Gompertz curve on day 200. Because the curve starts with zero derivative, the appearance is gradual — maybe one or two pixels of growth per day on the display. There is never a sudden pop.

Diameters are computed from the pipe model at render time: for each branch, count or estimate the total active leaf area downstream and set the diameter proportional to the square root of that total. Since the topology is fixed, this is still a direct function of time — no iteration needed.

The world-space position of any branch is computed by walking up the parent chain. Starting from the trunk base, you follow each parent to the attachment point, accounting for the parent's current length and curvature, until you reach the branch in question. This is spatial recursion, not temporal iteration — it computes positions at a single instant.

The topology itself is generated procedurally from a seed. A random number generator produces the number of scaffold branches, their angles, their attachment points, their growth rates, and so on recursively down to the finest twigs. Crucially, this generation happens once and the result is frozen. The seed determines the tree's entire life story.

The strengths are compelling. Smoothness is guaranteed by construction — if all component functions are smooth, the visual state is smooth at every time. Random access is trivial: you can compute the tree's appearance on any day without replaying all previous days. Determinism is perfect: same seed plus same day always produces the same image. Implementation is the simplest of all four approaches, perhaps 200 to 400 lines of code. Debugging is easy — you can plot any individual branch's growth curve in isolation.

The weaknesses are real but manageable. Self-pruning must either be pre-determined at initialization or added as a runtime heuristic, which somewhat compromises the pure-function-of-time elegance. Crown shape does not adapt to the tree's own growth — it is specified by the topology generator, not emergent. The tree cannot respond to external events because its entire future is predetermined. The topology generator requires careful parameter tuning to produce trees that look like cherry trees rather than generic fractals.

A mature cherry tree at the display's resolution needs roughly 2,000 to 3,000 branch segments. At about 12 floating-point parameters per branch, that is around 100 kilobytes — trivially small. And evaluating 2,000 Gompertz functions and drawing 2,000 line segments is trivially fast, even on modest hardware.

### Approach Four: Hybrid Parametric Skeleton Plus Rule-Based Detail

This approach splits the tree into two tiers that are computed differently but rendered as a single unified structure.

Tier One covers the trunk, the primary scaffold branches, and the secondary branches — orders zero through two. There are typically 15 to 20 of these in a cherry tree. They are defined parametrically, exactly as in approach three: smooth functions of time, guaranteed continuous, directly controllable. These are the branches the viewer's eye tracks. Any discontinuity in the trunk or a major scaffold branch would be immediately noticeable, so parametric smoothness is essential here.

Tier Two covers everything from order three onward: tertiary branches, twigs, and flowering spurs. There are potentially hundreds of these. They are generated by stochastic rules that operate on Tier One endpoints as attachment sites. These branches are numerous, thin — one to two pixels wide on the display — and visually noisy. Small perturbations in individual twigs are lost in the mass. This is where organic irregularity matters more than mathematical smoothness.

The boundary at order two is deliberate. Orders zero through two are few enough to define individually, visible enough that smoothness matters, and stable over the tree's lifetime. Orders three and above are numerous enough to benefit from rule-based generation, thin enough that small discontinuities are imperceptible, and dynamic — they can appear, grow, and die as the tree matures.

The two tiers are joined by bud sites. Each Tier One branch has a set of predetermined positions along its length where Tier Two branches may eventually sprout. These positions are deterministic, derived from the seed and the golden angle. Each bud has a scheduled activation time. When that time arrives, the bud begins growing a Tier Two subtree from zero length. The subtree reads the parent branch's current state — its diameter, direction, and age at the attachment point — to determine its own initial parameters.

Diameters are unified across both tiers by the pipe model. Starting from the leaf tips of the finest Tier Two branches, the algorithm sums downstream leaf area upward through the tree graph, across the tier boundary, all the way to the trunk. When a new Tier Two branch appears, it starts with zero leaf area and ramps up smoothly, preventing diameter jumps in its ancestors.

Self-pruning is handled by heuristics operating on Tier Two branches. The most effective combination is: distance from the crown surface, where branches deep inside the crown die first; local density, where branches in crowded regions thin out; and a simple light proxy combining vertical position with distance from the crown center. This combination produces about 90 percent of the visual quality of a full light simulation at a fraction of the computational cost. Tier One branches are permanent — they never die, matching the reality that a cherry tree's main scaffolds persist for its entire life.

Seasonal effects are implemented as a modulation layer on top of the structural growth. The structure evolves on a multi-year timescale. Seasonal appearance cycles annually. Flowering spurs, which are specialized Tier Two branches, carry blossoms when they are on wood at least two years old and the calendar is in the bloom window. Leaf density ramps up in spring and down in autumn. Petal fall creates scattered red dots on the ground. The structure and the appearance are separate concerns, making each easier to implement and tune.

Cherry-specific features map naturally to this architecture. The low trunk fork is a Tier One parameter. The 3 to 5 wide scaffold branches are Tier One branches with wide angle parameters. The flowering spurs on older wood are Tier Two specialized branches that activate after a two-year delay. The blossoms-before-leaves timing is a seasonal layer parameter — the blossom density function peaks before the leaf density function ramps up. The wide spreading crown is controlled by Tier One angles and a crown envelope fitted to Tier One branch tips.

The strengths of the hybrid approach are significant. It provides smoothness where it matters most, in the large visible branches, and organic realism where it matters most, in the crown texture and twig detail. Self-pruning is natural because Tier Two branches come and go. The seasonal architecture is the cleanest of all four approaches. Cherry-specific features have obvious mappings to the tier structure. And it most closely mirrors real tree biology, where the main scaffold is genetically determined while fine detail responds to the environment.

The weakness is complexity. This is the most architecturally elaborate approach, with two subsystems and an interface between them. It is more code than the parametric model. Tier Two rule tuning still requires iteration to get right. But each piece is individually well-understood, and the architecture decomposes cleanly.

---

## Part Four: Self-Pruning in Detail

Since self-pruning came up across all four approaches, here is a consolidated treatment of the heuristics.

The simplest approach is a crown-base-height parameter. You define a height on the trunk below which no branches survive, and this height rises over time. This produces the characteristic bare lower trunk of a mature tree but is too crude for interior crown thinning.

Better is distance-from-crown-surface pruning. You define the crown envelope — an ellipsoid or convex hull fitted to the branch tips — and compute how deeply each branch is buried within it. Branches close to the surface get plenty of light and survive. Branches deep inside the crown are heavily shaded and die. The probability of death increases exponentially with depth, following the same exponential decay that governs light penetration through foliage. This is computationally trivial once you have a crown envelope, and it produces realistic thinning at all heights, not just from the bottom.

Even better is combining distance-from-surface with local density. Count the number of branch tips within a small radius of each branch. If there are too many neighbors, competition is intense and the weakest branches die. This produces the natural-looking irregular spacing seen in real trees, where dense clusters thin out but isolated branches in gaps survive.

The most sophisticated simple heuristic is a light proxy. Combine vertical position (higher gets more light), distance from crown surface (closer to surface gets more light), and optionally a sun-side bias into a single light score. Compare this against the branch's respiratory cost, which scales with its wood volume. If the estimated light income does not cover the respiratory cost, the branch dies. This directly mimics the biological mechanism and produces the most realistic pruning pattern without any ray tracing.

Research by Palubicki and colleagues at SIGGRAPH 2009 compared a full voxel-based light simulation against simplified local light estimation. They found that the simplified version produced visually comparable tree shapes at dramatically lower computational cost. The key insight is that for visual realism, approximate light estimation is sufficient. The exact light distribution matters less than the general pattern of more light at the periphery and less in the interior.

For an open-grown cherry tree, pruning should be configured to be slow and mainly affecting the deep interior. Unlike forest trees, an isolated ornamental cherry gets light from all directions and retains its lower branches for a long time, giving it that characteristic wide, full crown.

An important implementation detail: when a branch dies, its leaf area should ramp to zero over several time steps rather than disappearing instantly. This prevents abrupt diameter changes in ancestor branches via the pipe model. Similarly, dead branches can persist visually as bare stubs for a while before being removed, adding realism.

---

## Part Five: Rendering Considerations for E-Ink

The target display is 800 by 480 pixels with three colors: white, black, and red. This constrains the rendering in specific ways.

At this resolution, the trunk of a mature cherry tree would be roughly 8 to 15 pixels wide. Primary scaffold branches would be 4 to 8 pixels. Secondary branches would be 2 to 4 pixels. Tertiary branches and twigs would be 1 to 2 pixels. Anything thinner than one pixel is invisible. This means the maximum useful branching depth is about 4 to 6 orders — fine twigs beyond that cannot be resolved.

Anti-aliasing is not available with only three colors. Branch edges will be hard-edged. Thicker branches of 2 or more pixels wide will look fine, but single-pixel branches at diagonal angles will look jagged. Smoothing branch paths with curved splines, such as Catmull-Rom interpolation through the branch nodes, will help by ensuring the path itself is smooth even if individual pixels are aliased.

Diameter tapering along segments is important for visual quality. Rather than drawing each segment with a uniform width, the diameter should taper linearly from the parent's diameter at the start to the child's diameter at the end. This produces the natural taper visible in the reference images.

For branches that are sub-pixel in thickness during their early growth, dithering can provide a fade-in effect. A branch could be drawn as alternating black and white pixels when it is very thin, transitioning to a solid line as it thickens. This gives the impression of gradual appearance within the constraints of a binary display.

The red channel should be reserved almost exclusively for cherry blossoms. This creates a striking visual contrast: the stark black branch architecture against the white background suddenly bursts into red during bloom season. Fallen petals as scattered red dots on the ground add atmosphere. In autumn, the red channel could be reused for colored leaves before they fall.

E-ink displays ghost, meaning remnants of previous images can persist faintly. Since the tree only grows and does not shrink, except for seasonal leaf drop, previous pixels mostly remain valid. A periodic full display refresh every few days would clear any ghosting artifacts.

Branch rendering order matters when branches overlap in the 2D projection. A simple painter's algorithm, drawing back-to-front by depth, ensures that branches in front correctly occlude those behind. For the 3D-to-2D projection, a fixed camera position is simplest, though a very slowly orbiting camera, changing by a fraction of a degree per day, could add long-term visual interest by gradually revealing different aspects of the tree's three-dimensional structure.

---

## Part Six: Comparison and Recommendation

Across all criteria, the hybrid approach, option four, scores highest for this project. It provides parametric smoothness for the visually dominant trunk and scaffold branches, organic irregularity for the crown detail and twig texture, natural self-pruning through heuristics on the rule-based tier, clean seasonal architecture with structure and appearance cleanly separated, and the best mapping of cherry-specific features to the model structure.

The direct parametric model, option three, is a strong second choice and is recommended as a starting point. It is dramatically simpler to implement, perhaps 200 to 400 lines of code compared to the hybrid's greater complexity. At the display's resolution during bloom season, when blossoms dominate the visual, the difference in organic quality between the two approaches may be hard to see. The parametric model's Tier One code carries directly into the hybrid version, so starting with it loses nothing.

The space colonization algorithm is the most biologically elegant and produces the most naturally asymmetric structures, but its limited control over fine details and the need to bolt on cherry-specific features make it less suitable as the primary approach. However, it could be valuable as the Tier Two method within the hybrid architecture — using space colonization instead of stochastic branching rules to fill in the crown detail.

Pure L-systems are the most thoroughly documented approach but require the most effort to overcome their inherent algorithmic regularity. The theory is beautiful, but achieving the organic look of a real cherry tree requires layering so many extensions — stochastic rules, context sensitivity, environmental queries, gravitropic bending, pipe model post-processing — that the simplicity of the grammar-based foundation is largely lost.

The recommended path forward is: first, build a rapid prototype using the direct parametric model to validate the growth curves, seasonal timing, cherry silhouette, and e-ink rendering. Second, graduate to the hybrid architecture for the production version, reusing the parametric Tier One code from the prototype and adding rule-based Tier Two detail, pipe model diameter computation, and self-pruning heuristics. This approach minimizes risk while building toward the highest-quality result.
