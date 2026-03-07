import math

import numpy as np

from .params import GrowthParams
from .tree import Bud, Node, Segment, TreeState, Vec3

GOLDEN_ANGLE = 2 * math.pi * (1 - (1 + math.sqrt(5)) / 2)  # ~137.5 degrees in radians


def create_initial_tree(params: GrowthParams, rng: np.random.Generator) -> TreeState:
    """Create a single trunk with buds placed along it."""
    tree = TreeState()

    # Root node at ground level
    root = tree.add_node(Vec3(0, 0, 0))
    tree.root_node_id = root.id

    # Tip node at top of trunk
    trunk_dir = Vec3(0, 1, 0)
    trunk_len = params.initial_trunk_length
    tip_pos = root.position + trunk_dir * trunk_len
    tip = tree.add_node(tip_pos)

    # Trunk segment
    seg = tree.add_segment(root.id, tip.id, trunk_len, trunk_dir, vigor=1.0)

    # Place buds along the trunk
    _place_buds_on_segment(tree, seg, params, rng)

    return tree


def _place_buds_on_segment(tree: TreeState, seg: Segment, params: GrowthParams,
                           rng: np.random.Generator):
    """Place buds at phyllotactic intervals along a segment."""
    end_node = tree.nodes[seg.end_node_id]
    start_pos = tree.nodes[seg.start_node_id].position

    num_buds = max(1, int(seg.length / params.bud_spacing))
    for i in range(num_buds):
        # Fraction along segment (evenly spaced, last bud near tip)
        t = (i + 1) / (num_buds + 1)
        bud_pos = start_pos + seg.direction * (seg.length * t)
        angle = GOLDEN_ANGLE * (i + 1)

        # Vigor decays with distance from tip: buds near base get less vigor
        distance_from_tip = 1.0 - t  # 0 at tip, 1 at base
        bud_vigor = seg.vigor * (1.0 - distance_from_tip * (1.0 - params.lateral_vigor_fraction))

        bud = Bud(
            phyllotactic_angle=angle,
            vigor=bud_vigor,
            position=bud_pos,
            node_id=end_node.id,
            age=0,
        )
        end_node.buds.append(bud)

    # Terminal bud at the very tip
    terminal_bud = Bud(
        phyllotactic_angle=rng.uniform(0, 2 * math.pi),
        vigor=seg.vigor * params.terminal_vigor_retention,
        position=end_node.position,
        node_id=end_node.id,
        age=0,
    )
    end_node.buds.append(terminal_bud)


def _rodrigues_rotate(vec: Vec3, axis: Vec3, angle_rad: float) -> Vec3:
    """Rotate vec around axis by angle_rad using Rodrigues' rotation formula."""
    k = axis.normalized()
    cos_a = math.cos(angle_rad)
    sin_a = math.sin(angle_rad)
    # v_rot = v*cos(a) + (k x v)*sin(a) + k*(k.v)*(1-cos(a))
    return vec * cos_a + k.cross(vec) * sin_a + k * (k.dot(vec) * (1 - cos_a))


def _find_perpendicular(v: Vec3) -> Vec3:
    """Find an arbitrary vector perpendicular to v."""
    if abs(v.x) < 0.9:
        candidate = Vec3(1, 0, 0)
    else:
        candidate = Vec3(0, 1, 0)
    return v.cross(candidate).normalized()


def _compute_shoot_direction(parent_dir: Vec3, phyllotactic_angle: float,
                             branching_angle_rad: float, is_terminal: bool,
                             terminal_deviation_rad: float, rng: np.random.Generator,
                             shoot_origin: Vec3, parent_length: float,
                             params: GrowthParams) -> Vec3:
    """Compute the direction for a new shoot using Rodrigues rotation + tropisms."""
    if is_terminal:
        # Small random deviation from parent direction
        deviation = rng.uniform(0, terminal_deviation_rad)
        perp = _find_perpendicular(parent_dir)
        random_angle = rng.uniform(0, 2 * math.pi)
        perp_rotated = _rodrigues_rotate(perp, parent_dir, random_angle)
        direction = _rodrigues_rotate(parent_dir, perp_rotated, deviation).normalized()
    else:
        # Full branching: rotate parent_dir by branching_angle around a perpendicular
        # that has been rotated by phyllotactic_angle around parent_dir
        perp = _find_perpendicular(parent_dir)
        perp_rotated = _rodrigues_rotate(perp, parent_dir, phyllotactic_angle)
        direction = _rodrigues_rotate(parent_dir, perp_rotated, branching_angle_rad).normalized()

    # Gravitropism: bias downward, weighted by parent segment length
    if params.gravitropism_strength > 0:
        length_ratio = parent_length / params.shoot_length_base
        gravity_bias = Vec3(0, -1, 0) * params.gravitropism_strength * length_ratio
        direction = direction + gravity_bias

    # Phototropism: bias upward + radially outward from trunk
    if params.phototropism_strength > 0:
        radial = Vec3(shoot_origin.x, 0, shoot_origin.z)
        if radial.length() > 0.01:
            radial = radial.normalized()
        else:
            radial = Vec3(0, 0, 0)
        photo_bias = (Vec3(0, 1, 0) * 0.5 + radial * 0.5) * params.phototropism_strength
        direction = direction + photo_bias

    return direction.normalized()


def _split_segment_at_bud(tree: TreeState, seg: Segment, bud: Bud) -> Node:
    """Split a segment at a bud's position, creating a junction node.

    Returns the new junction node. The original segment is split into two:
    start->junction and junction->end.
    """
    start_node = tree.nodes[seg.start_node_id]
    end_node = tree.nodes[seg.end_node_id]

    # Compute the fraction along the segment where the bud sits
    total_vec = end_node.position - start_node.position
    bud_vec = bud.position - start_node.position
    total_len = total_vec.length()
    if total_len < 1e-10:
        t = 0.5
    else:
        t = bud_vec.length() / total_len
    t = max(0.05, min(0.95, t))  # clamp to avoid degenerate splits

    # Create junction node
    junction = tree.add_node(bud.position)

    # First half: start -> junction (reuse original segment ID)
    len1 = seg.length * t
    seg.end_node_id = junction.id
    seg.length = len1
    junction.parent_segment_id = seg.id

    # Second half: junction -> end (new segment)
    len2 = seg.length * (1 - t) / t if t > 0 else seg.length
    # Recalculate properly
    len2 = total_len * (1 - t)
    seg2 = tree.add_segment(junction.id, end_node.id, len2, seg.direction, vigor=seg.vigor)
    seg2.age = seg.age
    seg2.status = seg.status

    # Move the end node's parent from old segment to new segment
    end_node.parent_segment_id = seg2.id

    # Update start_node's child list: it still points to seg (which now ends at junction)
    # Move buds from end_node that belong to the original segment to junction
    # (buds are on the end_node; they stay there since they were at the tip)

    return junction


def _process_node_buds(tree: TreeState, node: Node, params: GrowthParams,
                       rng: np.random.Generator,
                       effective_terminal_prob: float, effective_lateral_prob: float):
    """Activate buds on a node, extend shoots from activated ones."""
    if not node.buds:
        return

    is_terminal_node = len(node.child_segment_ids) == 0
    buds = list(node.buds)
    node.buds.clear()

    # The last bud in the list is the terminal bud (placed last in _place_buds_on_segment)
    # Process from tip to base to avoid tiny sub-segment issues
    activated_buds = []
    remaining_buds = []

    for i, bud in enumerate(buds):
        is_terminal_bud = (i == len(buds) - 1) and is_terminal_node

        if is_terminal_bud:
            prob = effective_terminal_prob
        else:
            prob = effective_lateral_prob * bud.vigor

        if rng.random() < prob:
            activated_buds.append((bud, is_terminal_bud))
        else:
            # Keep as dormant if not too old
            bud.age += 1
            if bud.age < params.max_bud_age:
                remaining_buds.append(bud)

    node.buds = remaining_buds

    # Sort activated buds: process those furthest from the node's parent first
    # (to handle segment splitting correctly — split from tip to base)
    if node.parent_segment_id is not None:
        parent_start = tree.nodes[tree.segments[node.parent_segment_id].start_node_id].position
        activated_buds.sort(
            key=lambda b: -(b[0].position - parent_start).length()
        )

    for bud, is_terminal in activated_buds:
        _extend_shoot(tree, node, bud, is_terminal, params, rng)


def _extend_shoot(tree: TreeState, origin_node: Node, bud: Bud, is_terminal: bool,
                  params: GrowthParams, rng: np.random.Generator):
    """Extend a new shoot from an activated bud."""
    # Determine the parent direction and length
    if origin_node.parent_segment_id is not None:
        parent_seg = tree.segments[origin_node.parent_segment_id]
        parent_dir = parent_seg.direction
        parent_length = parent_seg.length
    else:
        parent_dir = Vec3(0, 1, 0)
        parent_length = params.shoot_length_base

    # If the bud is not at the node's position, we need to split the segment
    bud_at_node = (bud.position - origin_node.position).length() < 0.01
    if not bud_at_node and origin_node.parent_segment_id is not None:
        # Find which segment the bud lies on
        parent_seg = tree.segments[origin_node.parent_segment_id]
        junction = _split_segment_at_bud(tree, parent_seg, bud)
        branch_from = junction
        parent_dir = parent_seg.direction
    else:
        branch_from = origin_node

    # Compute branching angle
    branching_angle_deg = rng.uniform(params.branching_angle_min, params.branching_angle_max)
    branching_angle_rad = math.radians(branching_angle_deg)
    terminal_deviation_rad = math.radians(params.terminal_deviation_max)

    # Compute shoot direction (with tropisms)
    shoot_dir = _compute_shoot_direction(
        parent_dir, bud.phyllotactic_angle, branching_angle_rad,
        is_terminal, terminal_deviation_rad, rng,
        shoot_origin=branch_from.position,
        parent_length=parent_length,
        params=params,
    )

    # Compute shoot length from vigor
    noise = rng.normal(0, params.shoot_length_noise)
    shoot_length = params.shoot_length_base * bud.vigor * max(0.3, 1.0 + noise)
    shoot_length = max(1.0, shoot_length)  # minimum 1 cm

    # Compute vigor for the new shoot
    if is_terminal:
        new_vigor = bud.vigor * params.terminal_vigor_retention
    else:
        new_vigor = bud.vigor * params.lateral_vigor_fraction

    # Create the new shoot tip
    tip_pos = branch_from.position + shoot_dir * shoot_length
    tip_node = tree.add_node(tip_pos)

    # Create the segment
    seg = tree.add_segment(branch_from.id, tip_node.id, shoot_length, shoot_dir,
                           vigor=new_vigor)

    # Place buds on the new shoot
    _place_buds_on_segment(tree, seg, params, rng)


def grow_one_year(tree: TreeState, params: GrowthParams, rng: np.random.Generator):
    """Execute one year of growth (Phase A only in Step 1)."""
    tree.current_year += 1

    # Age all existing segments
    for seg in tree.segments.values():
        seg.age += 1
        if seg.status == 'growing':
            seg.status = 'mature'

    # Compute effective bud probabilities with age-dependent apical dominance decay
    decay_factor = max(0.3, 1.0 - params.apical_dominance_decay * tree.current_year)
    effective_terminal_prob = params.terminal_bud_probability * decay_factor
    effective_lateral_prob = params.lateral_bud_probability

    # Collect nodes that have buds (snapshot the list to avoid mutation issues)
    nodes_with_buds = [n for n in tree.nodes.values() if n.buds]

    # Process buds on each node
    for node in nodes_with_buds:
        _process_node_buds(tree, node, params, rng,
                           effective_terminal_prob, effective_lateral_prob)
