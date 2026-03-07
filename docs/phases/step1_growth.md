# Step 1: Dev UI + Minimal Viable Growth

## Goal

A single trunk grows upward. Buds form along it. Some buds activate and produce lateral branches. Lateral branches produce their own buds and sub-branches. Runs for ~8 simulated years. Results are visible in a development UI with parameter sliders.

## Design

### Data Structures (`etree/tree.py`)

- **Vec3**: Lightweight 3D vector with arithmetic operators, length/normalized methods, numpy conversion.
- **Bud**: `phyllotactic_angle` (radians), `vigor` (0-1), `position` (Vec3), `node_id`.
- **Node**: `id`, `position` (Vec3), `parent_segment_id` (Optional[int]), `child_segment_ids` (list[int]), `buds` (list[Bud]).
- **Segment**: `id`, `start_node_id`, `end_node_id`, `length` (cm), `diameter` (cm), `direction` (Vec3 unit), `age` (years), `status` ('growing'|'mature'), `vigor` (0-1), `downstream_leaf_area` (float).
- **TreeState**: `nodes` (dict[int, Node]), `segments` (dict[int, Segment]), `root_node_id`, `current_year`, `next_node_id`, `next_segment_id`, `rng_state`.

Uses integer ID dicts (not object references) for JSON serialization and to avoid circular reference issues.

### Vigor Model

Each segment carries a `vigor` value (0-1). This drives both activation probability and shoot length:
- Terminal bud inherits `parent_vigor * terminal_vigor_retention` (default 0.95)
- Lateral bud inherits `parent_vigor * lateral_vigor_fraction` (default 0.5)
- `shoot_length = shoot_length_base * vigor * (1 + noise)`
- Natural decay: trunk vigor 1.0 (~25cm) -> lateral 0.5 (~12cm) -> sub-lateral 0.25 (~6cm)

### Phase A: Growth Algorithm

1. **create_initial_tree()**: Single trunk from (0,0,0) to (0,trunk_length,0), buds placed at phyllotactic intervals.
2. **Bud placement**: Golden angle (137.5 deg) intervals along new shoots. ~1 bud per `bud_spacing` cm.
3. **Bud activation**: Terminal bud (last bud on childless node) gets `terminal_bud_probability`. Laterals get `lateral_bud_probability * bud.vigor`. Stochastic activation via seeded PRNG.
4. **Shoot extension**: Compute branching direction via Rodrigues rotation. Find perpendicular to parent direction, rotate by phyllotactic angle around parent, then rotate parent by branching angle around that perpendicular. Terminal buds get small deviation (0-10 deg).
5. **Segment splitting**: When a lateral bud activates mid-segment, insert a junction node at bud position. Split parent segment into two sub-segments. Update all references.

### Phase B: Pipe Model

Bottom-up traversal from tips to root. Terminal segments get `base_leaf_area`. At junctions, sum children's downstream leaf areas. `diameter = pipe_scaling_constant * sqrt(downstream_leaf_area)`.

### Serialization

Each snapshot: `{year, segments: [{id, x1,y1,z1, x2,y2,z2, diameter, age, status}], bounds: {min_x, max_x, min_y, max_y}}`. Self-contained with world coordinates baked in.

### Dev UI

- **Flask server**: GET / (index.html), GET /api/params (slider metadata), POST /api/simulate (params -> snapshots JSON).
- **Sidebar** (280px): Parameter sliders built from metadata, Run button.
- **Main area**: CSS grid of canvases, one per year.
- **Renderer**: Orthographic side view (x->screen_x, y->screen_y flipped, z dropped). Global bounds across all snapshots for consistent scale. `lineWidth = diameter * scale`, `lineCap = 'round'`. Sort by z for painter's algorithm.

### Key Design Decisions

- 3D even in Step 1 because phyllotactic spiral is naturally 3D
- Integer ID dicts for serialization, debugging, split stability
- Single POST for simulation (under 100ms for <500 segments)
- Global bounds for consistent scale across year canvases
- Terminal bud = last bud on childless node

### Pitfalls to Avoid

1. Buds accumulate forever — remove buds older than 2-3 years
2. Multiple bud activations on one segment create tiny sub-segments — process buds tip-to-base
3. Too symmetrical — noise + probabilistic activation
4. Fishbone pattern — branching angle randomized + phyllotactic spiral rotates 137.5 deg

### Deliberate Omissions (deferred to later steps)

No tropisms, no pruning, no voxel grid, no collision avoidance, no seasonal rendering, no time interpolation, no 3D projection, no spline curves.

---

## TODO

### 1. Project Setup
- [ ] Create `requirements.txt` (flask, numpy)
- [ ] Create `run.py` entry point
- [ ] Create package structure (`etree/__init__.py`, `web/`)

### 2. Core Data Structures (`etree/tree.py`)
- [ ] Implement Vec3 class with arithmetic operators
- [ ] Implement Bud dataclass
- [ ] Implement Node dataclass
- [ ] Implement Segment dataclass
- [ ] Implement TreeState class with add_node, add_segment, get_terminal_nodes

### 3. Parameters (`etree/params.py`)
- [ ] Implement GrowthParams dataclass with defaults
- [ ] Implement slider_metadata() classmethod

### 4. Growth Simulation (`etree/growth.py`)
- [ ] Implement create_initial_tree (single trunk + buds)
- [ ] Implement bud placement along segments (phyllotactic spiral)
- [ ] Implement direction computation (Rodrigues rotation)
- [ ] Implement segment splitting (insert junction node)
- [ ] Implement bud activation scoring (apical dominance + vigor)
- [ ] Implement shoot extension from activated buds
- [ ] Implement grow_one_year orchestrator

### 5. Pipe Model (`etree/pipe_model.py`)
- [ ] Implement bottom-up diameter computation
- [ ] Verify: trunk thickest, laterals thinner, sub-branches thinnest

### 6. Serialization & Simulation (`etree/serialize.py`, `etree/simulation.py`)
- [ ] Implement serialize_tree (tree state -> JSON-ready dict)
- [ ] Implement run_simulation (N years, returns list of snapshots)

### 7. Flask Server (`web/app.py`)
- [ ] GET / route (serve index.html)
- [ ] GET /api/params route (slider metadata)
- [ ] POST /api/simulate route (params -> snapshots JSON)

### 8. Dev UI
- [ ] HTML template (sidebar + grid container)
- [ ] CSS layout (fixed sidebar, auto-flow grid)
- [ ] Slider panel (build from param metadata)
- [ ] Canvas renderer (orthographic side view, branch widths, depth sort)
- [ ] Main app (wire sliders -> simulate -> render grid)

### 9. Validation
- [ ] Same seed = same tree (determinism)
- [ ] Year 0 = single trunk, Year 8 = branching sapling
- [ ] Pipe model produces correct diameter hierarchy
- [ ] Parameter changes produce visually different trees
