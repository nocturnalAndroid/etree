from .tree import TreeState


def serialize_tree(tree: TreeState) -> dict:
    """Convert tree state to a JSON-ready dict with baked-in world coordinates."""
    segments = []
    min_x = min_y = float('inf')
    max_x = max_y = float('-inf')

    for seg in tree.segments.values():
        start = tree.nodes[seg.start_node_id].position
        end = tree.nodes[seg.end_node_id].position

        min_x = min(min_x, start.x, end.x)
        max_x = max(max_x, start.x, end.x)
        min_y = min(min_y, start.y, end.y)
        max_y = max(max_y, start.y, end.y)

        segments.append({
            'id': seg.id,
            'x1': start.x, 'y1': start.y, 'z1': start.z,
            'x2': end.x, 'y2': end.y, 'z2': end.z,
            'diameter': seg.diameter,
            'age': seg.age,
            'status': seg.status,
        })

    # Handle empty tree
    if not segments:
        min_x = max_x = min_y = max_y = 0.0

    return {
        'year': tree.current_year,
        'segments': segments,
        'bounds': {
            'min_x': min_x,
            'max_x': max_x,
            'min_y': min_y,
            'max_y': max_y,
        },
    }
