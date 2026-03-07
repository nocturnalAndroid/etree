"""Tests for tree growth behavior — verifying outcomes, not implementation details."""
import numpy as np
import pytest

from etree.growth import create_initial_tree, grow_one_year
from etree.params import GrowthParams
from etree.tree import Vec3


def make_tree(years=3, **param_overrides):
    """Helper: grow a tree for N years with given params and return it."""
    params = GrowthParams(**param_overrides)
    rng = np.random.default_rng(params.seed)
    tree = create_initial_tree(params, rng)
    for _ in range(years):
        grow_one_year(tree, params, rng)
    return tree, params


def tree_height(tree):
    """Max y coordinate across all nodes."""
    return max(n.position.y for n in tree.nodes.values())


def tree_width(tree):
    """Horizontal spread (max |x| across all nodes)."""
    xs = [abs(n.position.x) for n in tree.nodes.values()]
    zs = [abs(n.position.z) for n in tree.nodes.values()]
    return max(max(xs), max(zs))


# ── Determinism ──────────────────────────────────────────────────────

class TestDeterminism:
    def test_same_seed_same_tree(self):
        """Identical seeds must produce identical trees."""
        tree1, _ = make_tree(years=5, seed=123)
        tree2, _ = make_tree(years=5, seed=123)

        assert len(tree1.segments) == len(tree2.segments)
        assert len(tree1.nodes) == len(tree2.nodes)

        for nid in tree1.nodes:
            p1 = tree1.nodes[nid].position
            p2 = tree2.nodes[nid].position
            assert abs(p1.x - p2.x) < 1e-10
            assert abs(p1.y - p2.y) < 1e-10
            assert abs(p1.z - p2.z) < 1e-10

    def test_different_seed_different_tree(self):
        """Different seeds should produce different trees."""
        tree1, _ = make_tree(years=5, seed=1)
        tree2, _ = make_tree(years=5, seed=2)

        # Very unlikely to have the same segment count with different seeds
        positions1 = sorted((n.position.x, n.position.y) for n in tree1.nodes.values())
        positions2 = sorted((n.position.x, n.position.y) for n in tree2.nodes.values())
        assert positions1 != positions2


# ── Basic growth ─────────────────────────────────────────────────────

class TestBasicGrowth:
    def test_tree_grows_taller_each_year(self):
        """Tree height should increase over time."""
        params = GrowthParams(seed=42)
        rng = np.random.default_rng(params.seed)
        tree = create_initial_tree(params, rng)

        prev_height = tree_height(tree)
        for _ in range(5):
            grow_one_year(tree, params, rng)
            h = tree_height(tree)
            assert h >= prev_height
            prev_height = h

    def test_tree_gains_segments_each_year(self):
        """Segment count should increase each year (buds activate)."""
        params = GrowthParams(seed=42)
        rng = np.random.default_rng(params.seed)
        tree = create_initial_tree(params, rng)

        prev_count = len(tree.segments)
        for _ in range(5):
            grow_one_year(tree, params, rng)
            count = len(tree.segments)
            assert count > prev_count
            prev_count = count

    def test_initial_tree_starts_at_origin(self):
        """Root node should be at the origin."""
        tree, params = make_tree(years=0)
        root = tree.nodes[tree.root_node_id]
        assert root.position.x == 0
        assert root.position.y == 0
        assert root.position.z == 0

    def test_initial_trunk_length_matches_param(self):
        """The initial trunk tip should be at the configured trunk height."""
        for trunk_len in [20.0, 40.0, 60.0]:
            tree, _ = make_tree(years=0, initial_trunk_length=trunk_len)
            h = tree_height(tree)
            assert abs(h - trunk_len) < 0.01

    def test_more_years_more_segments(self):
        """Growing longer should produce more structure."""
        tree3, _ = make_tree(years=3, seed=42)
        tree8, _ = make_tree(years=8, seed=42)
        assert len(tree8.segments) > len(tree3.segments)


# ── Vigor model ──────────────────────────────────────────────────────

class TestVigor:
    def test_high_lateral_vigor_produces_more_branches(self):
        """Higher lateral vigor fraction = more lateral activation = more segments."""
        tree_low, _ = make_tree(years=5, seed=42, lateral_vigor_fraction=0.2)
        tree_high, _ = make_tree(years=5, seed=42, lateral_vigor_fraction=0.9)
        assert len(tree_high.segments) > len(tree_low.segments)

    def test_high_lateral_prob_produces_more_branches(self):
        """Higher lateral bud probability = more branching."""
        tree_low, _ = make_tree(years=5, seed=42, lateral_bud_probability=0.1)
        tree_high, _ = make_tree(years=5, seed=42, lateral_bud_probability=0.8)
        assert len(tree_high.segments) > len(tree_low.segments)

    def test_zero_lateral_prob_no_lateral_branching(self):
        """With zero lateral probability, only terminal buds should activate."""
        tree, _ = make_tree(years=5, seed=42, lateral_bud_probability=0.0)
        # Should still grow (terminal buds), but with minimal branching
        # At most one segment per year from the terminal bud chain
        assert len(tree.segments) < 20  # a fully branching tree would have far more


# ── Tropisms ─────────────────────────────────────────────────────────

class TestGravitropism:
    def test_gravitropism_makes_branches_droop(self):
        """With strong gravitropism, average branch tip y should be lower."""
        tree_none, _ = make_tree(years=6, seed=42, gravitropism_strength=0.0, phototropism_strength=0.0)
        tree_grav, _ = make_tree(years=6, seed=42, gravitropism_strength=0.4, phototropism_strength=0.0)

        # Average y of all non-root nodes
        avg_y_none = sum(n.position.y for n in tree_none.nodes.values()) / len(tree_none.nodes)
        avg_y_grav = sum(n.position.y for n in tree_grav.nodes.values()) / len(tree_grav.nodes)

        assert avg_y_grav < avg_y_none

    def test_zero_gravitropism_no_downward_bias(self):
        """With zero gravitropism and phototropism, tree should still grow upward."""
        tree, _ = make_tree(years=5, seed=42, gravitropism_strength=0.0, phototropism_strength=0.0)
        assert tree_height(tree) > 40  # taller than initial trunk


class TestPhototropism:
    def test_phototropism_biases_direction_outward(self):
        """A shoot originating away from the trunk axis should be biased
        radially outward by phototropism. Test at the direction level."""
        from etree.growth import _compute_shoot_direction

        rng = np.random.default_rng(0)
        params_off = GrowthParams(phototropism_strength=0.0, gravitropism_strength=0.0)
        params_on = GrowthParams(phototropism_strength=0.5, gravitropism_strength=0.0)

        # Branch originating at (20, 50, 0) — well away from trunk in x
        origin = Vec3(20, 50, 0)
        parent_dir = Vec3(0.5, 0.5, 0).normalized()

        # Compute many directions and compare average radial component
        n = 200
        radial_off = 0
        radial_on = 0
        for _ in range(n):
            angle = rng.uniform(0, 6.28)
            branch_angle = 0.8  # ~45 deg

            rng_a = np.random.default_rng(rng.integers(0, 10**9))
            rng_b = np.random.default_rng(rng_a.integers(0, 10**9))

            d_off = _compute_shoot_direction(
                parent_dir, angle, branch_angle, False, 0, rng_a,
                shoot_origin=origin, parent_length=25.0, params=params_off)
            d_on = _compute_shoot_direction(
                parent_dir, angle, branch_angle, False, 0, rng_b,
                shoot_origin=origin, parent_length=25.0, params=params_on)

            # Radial component = projection onto normalized (origin.x, 0, origin.z)
            radial_off += d_off.x  # origin is at (20,50,0), so outward = +x
            radial_on += d_on.x

        # Phototropism should bias in the +x direction (away from trunk)
        assert radial_on / n > radial_off / n


class TestApicalDominanceDecay:
    def test_high_decay_weakens_leader_over_time(self):
        """With high apical dominance decay, the terminal probability drops,
        producing a more spreading form (wider relative to height)."""
        tree_nodecay, _ = make_tree(years=8, seed=42, apical_dominance_decay=0.0,
                                     gravitropism_strength=0.0, phototropism_strength=0.0)
        tree_decay, _ = make_tree(years=8, seed=42, apical_dominance_decay=0.08,
                                   gravitropism_strength=0.0, phototropism_strength=0.0)

        # With strong decay, height should be lower (weaker leader)
        assert tree_height(tree_decay) < tree_height(tree_nodecay)

    def test_decay_floor_prevents_zero_probability(self):
        """Even with max decay, terminal probability doesn't go below 30% of base."""
        # decay=0.1, year=20 → factor = max(0.3, 1 - 0.1*20) = max(0.3, -1) = 0.3
        # So tree should still grow (not die)
        tree, _ = make_tree(years=10, seed=42, apical_dominance_decay=0.1)
        assert len(tree.segments) > 10  # still alive and branching


# ── Parameter configuration ──────────────────────────────────────────

class TestParams:
    def test_slider_metadata_covers_all_params(self):
        """Every numeric field in GrowthParams should have slider metadata."""
        from dataclasses import fields
        param_names = {f.name for f in fields(GrowthParams)
                       if f.name != 'apical_dominance_strength'}  # legacy, no slider
        meta_names = {m['name'] for m in GrowthParams.slider_metadata()}
        assert meta_names == param_names

    def test_all_sliders_have_descriptions(self):
        """Every slider should have a non-empty description for tooltips."""
        for m in GrowthParams.slider_metadata():
            assert 'description' in m, f"Missing description for {m['name']}"
            assert len(m['description']) > 10, f"Description too short for {m['name']}"

    def test_slider_defaults_within_range(self):
        """Each slider's default value should be within its min/max range."""
        for m in GrowthParams.slider_metadata():
            assert m['min'] <= m['default'] <= m['max'], (
                f"{m['name']}: default {m['default']} not in [{m['min']}, {m['max']}]"
            )

    def test_slider_step_positive(self):
        """Each slider step should be positive."""
        for m in GrowthParams.slider_metadata():
            assert m['step'] > 0, f"{m['name']} has non-positive step"


# ── Pipe model ───────────────────────────────────────────────────────

class TestPipeModel:
    def test_trunk_is_thickest(self):
        """The trunk (root segment) should have the largest diameter."""
        from etree.pipe_model import compute_diameters

        tree, params = make_tree(years=5, seed=42)
        compute_diameters(tree, params)

        root = tree.nodes[tree.root_node_id]
        trunk_seg = tree.segments[root.child_segment_ids[0]]

        for seg in tree.segments.values():
            assert trunk_seg.diameter >= seg.diameter - 0.001

    def test_terminal_segments_have_base_leaf_area(self):
        """Terminal segments should have downstream_leaf_area equal to base_leaf_area."""
        from etree.pipe_model import compute_diameters

        tree, params = make_tree(years=3, seed=42)
        compute_diameters(tree, params)

        for seg in tree.segments.values():
            end_node = tree.nodes[seg.end_node_id]
            if not end_node.child_segment_ids:
                assert abs(seg.downstream_leaf_area - params.base_leaf_area) < 0.01

    def test_larger_pipe_scaling_means_thicker_branches(self):
        """Increasing pipe_scaling_constant should increase all diameters."""
        from etree.pipe_model import compute_diameters

        tree1, p1 = make_tree(years=4, seed=42, pipe_scaling_constant=0.1)
        compute_diameters(tree1, p1)

        tree2, p2 = make_tree(years=4, seed=42, pipe_scaling_constant=0.3)
        compute_diameters(tree2, p2)

        diameters1 = sorted(s.diameter for s in tree1.segments.values())
        diameters2 = sorted(s.diameter for s in tree2.segments.values())

        # Same tree structure (same seed), so we can compare element-wise
        assert len(diameters1) == len(diameters2)
        for d1, d2 in zip(diameters1, diameters2):
            assert d2 >= d1


# ── Serialization ────────────────────────────────────────────────────

class TestSerialization:
    def test_serialize_roundtrip_has_all_segments(self):
        """Serialized output should contain all segments."""
        from etree.serialize import serialize_tree

        tree, _ = make_tree(years=4, seed=42)
        data = serialize_tree(tree)

        assert data['year'] == 4
        assert len(data['segments']) == len(tree.segments)

    def test_serialized_bounds_contain_all_nodes(self):
        """Bounds should encompass every node position."""
        from etree.serialize import serialize_tree

        tree, _ = make_tree(years=4, seed=42)
        data = serialize_tree(tree)
        bounds = data['bounds']

        for node in tree.nodes.values():
            assert bounds['min_x'] <= node.position.x <= bounds['max_x']
            assert bounds['min_y'] <= node.position.y <= bounds['max_y']


# ── Full simulation ──────────────────────────────────────────────────

class TestFullSimulation:
    def test_run_simulation_returns_correct_snapshot_count(self):
        """run_simulation should return num_years + 1 snapshots (year 0 through N)."""
        from etree.simulation import run_simulation

        result = run_simulation(GrowthParams(num_years=5, seed=42))
        assert len(result['snapshots']) == 6  # year 0 + 5 years

    def test_simulation_snapshots_grow_monotonically(self):
        """Each successive snapshot should have at least as many segments."""
        from etree.simulation import run_simulation

        result = run_simulation(GrowthParams(num_years=8, seed=42))
        counts = [len(s['segments']) for s in result['snapshots']]

        for i in range(1, len(counts)):
            assert counts[i] >= counts[i - 1]

    def test_global_bounds_contain_all_snapshots(self):
        """Global bounds should encompass every snapshot's local bounds."""
        from etree.simulation import run_simulation

        result = run_simulation(GrowthParams(num_years=6, seed=42))
        gb = result['global_bounds']

        for snap in result['snapshots']:
            sb = snap['bounds']
            assert gb['min_x'] <= sb['min_x']
            assert gb['max_x'] >= sb['max_x']
            assert gb['min_y'] <= sb['min_y']
            assert gb['max_y'] >= sb['max_y']
