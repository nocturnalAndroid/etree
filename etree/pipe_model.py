import math

from .params import GrowthParams
from .tree import TreeState


def compute_diameters(tree: TreeState, params: GrowthParams):
    """Bottom-up pipe model: compute diameters from leaf area.

    Terminal segments contribute base_leaf_area. At each junction,
    parent's downstream_leaf_area = sum of children's. Then:
        diameter = pipe_scaling_constant * sqrt(downstream_leaf_area)
    """
    # Reset all downstream leaf areas
    for seg in tree.segments.values():
        seg.downstream_leaf_area = 0.0

    # Find terminal segments (end node has no children)
    terminal_seg_ids = []
    for seg in tree.segments.values():
        end_node = tree.nodes[seg.end_node_id]
        if not end_node.child_segment_ids:
            terminal_seg_ids.append(seg.id)
            seg.downstream_leaf_area = params.base_leaf_area

    # BFS from terminals toward root
    visited = set()
    queue = list(terminal_seg_ids)

    while queue:
        seg_id = queue.pop(0)
        if seg_id in visited:
            continue
        visited.add(seg_id)

        seg = tree.segments[seg_id]

        # Check if all children of this segment's end node have been processed
        end_node = tree.nodes[seg.end_node_id]
        children_ready = all(
            child_id in visited for child_id in end_node.child_segment_ids
        )

        if not children_ready and end_node.child_segment_ids:
            # Not ready yet, put back in queue
            visited.discard(seg_id)
            queue.append(seg_id)
            continue

        # Sum downstream leaf area from children
        if end_node.child_segment_ids:
            seg.downstream_leaf_area = sum(
                tree.segments[cid].downstream_leaf_area
                for cid in end_node.child_segment_ids
            )

        # Compute diameter
        seg.diameter = params.pipe_scaling_constant * math.sqrt(seg.downstream_leaf_area)
        seg.diameter = max(seg.diameter, 0.05)  # minimum visible diameter

        # Propagate to parent
        start_node = tree.nodes[seg.start_node_id]
        if start_node.parent_segment_id is not None:
            parent_id = start_node.parent_segment_id
            if parent_id not in visited:
                queue.append(parent_id)
