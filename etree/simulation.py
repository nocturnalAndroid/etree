import numpy as np

from .growth import create_initial_tree, grow_one_year
from .params import GrowthParams
from .pipe_model import compute_diameters
from .serialize import serialize_tree


def run_simulation(params: GrowthParams) -> list[dict]:
    """Run the full simulation for N years, returning a list of snapshots."""
    rng = np.random.default_rng(params.seed)

    tree = create_initial_tree(params, rng)
    compute_diameters(tree, params)

    snapshots = [serialize_tree(tree)]

    for year in range(params.num_years):
        grow_one_year(tree, params, rng)
        compute_diameters(tree, params)
        snapshots.append(serialize_tree(tree))

    # Compute global bounds across all snapshots for consistent rendering
    global_bounds = {
        'min_x': min(s['bounds']['min_x'] for s in snapshots),
        'max_x': max(s['bounds']['max_x'] for s in snapshots),
        'min_y': min(s['bounds']['min_y'] for s in snapshots),
        'max_y': max(s['bounds']['max_y'] for s in snapshots),
    }

    # Add padding (10%)
    dx = global_bounds['max_x'] - global_bounds['min_x']
    dy = global_bounds['max_y'] - global_bounds['min_y']
    pad = max(dx, dy, 10) * 0.1
    global_bounds['min_x'] -= pad
    global_bounds['max_x'] += pad
    global_bounds['min_y'] -= pad
    global_bounds['max_y'] += pad

    return {
        'snapshots': snapshots,
        'global_bounds': global_bounds,
    }
