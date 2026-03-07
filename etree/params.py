from dataclasses import dataclass, fields


@dataclass
class GrowthParams:
    # Branching geometry
    branching_angle_min: float = 50.0   # degrees
    branching_angle_max: float = 75.0   # degrees
    terminal_deviation_max: float = 10.0  # degrees, small wobble for terminal buds

    # Bud spacing and activation
    bud_spacing: float = 5.0           # cm between buds along a shoot
    terminal_bud_probability: float = 0.95
    lateral_bud_probability: float = 0.4
    apical_dominance_strength: float = 0.8  # vigor decay for lateral buds (not used directly; see vigor)
    max_bud_age: int = 3               # buds older than this are removed

    # Vigor model
    terminal_vigor_retention: float = 0.9   # terminal bud inherits this fraction of parent vigor
    lateral_vigor_fraction: float = 0.65    # lateral bud inherits this fraction of parent vigor

    # Shoot dimensions
    shoot_length_base: float = 30.0    # cm, base shoot length at vigor=1
    shoot_length_noise: float = 0.15   # fractional noise on shoot length
    initial_trunk_length: float = 40.0 # cm

    # Pipe model
    pipe_scaling_constant: float = 0.15
    base_leaf_area: float = 1.0        # leaf area contribution per terminal segment

    # Tropisms
    gravitropism_strength: float = 0.3
    phototropism_strength: float = 0.2
    apical_dominance_decay: float = 0.06

    # Simulation
    num_years: int = 8
    seed: int = 42

    @classmethod
    def slider_metadata(cls):
        meta = {
            'branching_angle_min': {
                'min': 20, 'max': 80, 'step': 1,
                'label': 'Branch Angle Min (deg)',
                'description': 'Angle between parent branch and new lateral shoot. Each new lateral gets a random angle in [min, max]. Measured in degrees from parent direction.',
            },
            'branching_angle_max': {
                'min': 20, 'max': 80, 'step': 1,
                'label': 'Branch Angle Max (deg)',
                'description': 'Angle between parent branch and new lateral shoot. Each new lateral gets a random angle in [min, max]. Measured in degrees from parent direction.',
            },
            'terminal_deviation_max': {
                'min': 0, 'max': 30, 'step': 1,
                'label': 'Terminal Deviation (deg)',
                'description': 'Maximum random wobble applied to terminal (tip) buds. Keeps the leader from being perfectly straight. Range: [0, max] degrees.',
            },
            'bud_spacing': {
                'min': 2, 'max': 15, 'step': 0.5,
                'label': 'Bud Spacing (cm)',
                'description': 'Distance between successive buds placed along a new shoot. Fewer buds = sparser branching. Number of buds per shoot = shoot_length / bud_spacing.',
            },
            'terminal_bud_probability': {
                'min': 0.0, 'max': 1.0, 'step': 0.05,
                'label': 'Terminal Bud Prob',
                'description': 'Probability that the terminal bud (at the shoot tip) activates next spring. High values maintain strong apical dominance.',
            },
            'lateral_bud_probability': {
                'min': 0.0, 'max': 1.0, 'step': 0.05,
                'label': 'Lateral Bud Prob',
                'description': 'Base probability for a lateral bud to activate. Actual probability = lateral_bud_prob \u00d7 bud_vigor.',
            },
            'max_bud_age': {
                'min': 1, 'max': 5, 'step': 1,
                'label': 'Max Bud Age (years)',
                'description': 'Buds older than this (in years) are removed. Prevents infinite bud accumulation on old wood.',
            },
            'terminal_vigor_retention': {
                'min': 0.5, 'max': 1.0, 'step': 0.05,
                'label': 'Terminal Vigor Retention',
                'description': 'Fraction of parent vigor inherited by the terminal bud. vigor_terminal = parent_vigor \u00d7 retention.',
            },
            'lateral_vigor_fraction': {
                'min': 0.1, 'max': 1.0, 'step': 0.05,
                'label': 'Lateral Vigor Fraction',
                'description': 'Fraction of parent vigor inherited by lateral buds. vigor_lateral = parent_vigor \u00d7 fraction. Controls how quickly sub-branches shrink.',
            },
            'shoot_length_base': {
                'min': 5, 'max': 60, 'step': 1,
                'label': 'Shoot Length Base (cm)',
                'description': 'Base shoot length at full vigor (vigor=1). Actual length = base \u00d7 vigor \u00d7 (1 + noise). Units: cm.',
            },
            'shoot_length_noise': {
                'min': 0.0, 'max': 0.5, 'step': 0.05,
                'label': 'Shoot Length Noise',
                'description': 'Random variation in shoot length. Drawn from N(0, noise). Actual length = base \u00d7 vigor \u00d7 max(0.3, 1 + N(0, noise)).',
            },
            'initial_trunk_length': {
                'min': 10, 'max': 80, 'step': 5,
                'label': 'Initial Trunk (cm)',
                'description': 'Length of the initial trunk segment at year 0. Units: cm.',
            },
            'pipe_scaling_constant': {
                'min': 0.05, 'max': 0.5, 'step': 0.01,
                'label': 'Pipe Scaling',
                'description': 'Controls branch thickness. diameter = scaling \u00d7 sqrt(downstream_leaf_area). Larger = thicker branches.',
            },
            'base_leaf_area': {
                'min': 0.1, 'max': 5.0, 'step': 0.1,
                'label': 'Base Leaf Area',
                'description': 'Leaf area contributed by each terminal segment. Propagated up via pipe model to compute diameters.',
            },
            'gravitropism_strength': {
                'min': 0.0, 'max': 0.5, 'step': 0.01,
                'label': 'Gravitropism',
                'description': 'Downward bias on branch direction, weighted by branch length. direction += strength \u00d7 (0, -1, 0) \u00d7 (parent_length / shoot_length_base).',
            },
            'phototropism_strength': {
                'min': 0.0, 'max': 0.5, 'step': 0.01,
                'label': 'Phototropism',
                'description': 'Upward + outward bias on branch direction. Pushes branches toward light (up) and away from trunk center (radially outward).',
            },
            'apical_dominance_decay': {
                'min': 0.0, 'max': 0.1, 'step': 0.005,
                'label': 'Apical Dom. Decay',
                'description': 'Per-year reduction in apical dominance. Effective prob = base \u00d7 max(0.3, 1 - decay \u00d7 tree_age). Weakens central leader over time.',
            },
            'num_years': {
                'min': 1, 'max': 20, 'step': 1,
                'label': 'Num Years',
                'description': 'Number of growth years to simulate.',
            },
            'seed': {
                'min': 1, 'max': 9999, 'step': 1,
                'label': 'Seed',
                'description': 'Random seed for deterministic simulation. Same seed = same tree.',
            },
        }
        result = []
        for f in fields(cls):
            if f.name in meta:
                m = meta[f.name]
                result.append({
                    'name': f.name,
                    'default': f.default,
                    **m,
                })
        return result
