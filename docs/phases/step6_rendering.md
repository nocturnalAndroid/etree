# Step 6: 3D Rendering and Camera

## Goal

Render the 3D tree as a 2D image suitable for the e-ink display.

## What to Implement

- 3D -> 2D projection (perspective or orthographic, fixed camera, side view)
- Branch rendering as tapered line segments (thickness = projected diameter)
- Catmull-Rom spline interpolation through waypoints (former junction kinks) for smooth curves
- Painter's algorithm (depth sorting) for overlap
- Output as image (PNG or similar)

## Evaluation Criteria

- Does the 2D rendering look like the reference images?
- Are branch widths reasonable at display resolution?
- Does depth sorting produce correct occlusion?
- Does spline interpolation produce natural-looking curves at kinks?

## Contingency

If 3D projection produces a confusing tangle (too much depth complexity), try: narrower field of view, slight rotation to separate overlapping branches, or explore 2D simulation as a fallback (growth in a plane, no depth).

---

## TODO

- [ ] Implement 3D to 2D projection (perspective or orthographic)
- [ ] Implement tapered branch rendering (trapezoid segments)
- [ ] Implement Catmull-Rom spline interpolation through waypoints
- [ ] Implement painter's algorithm (depth sorting)
- [ ] Implement PNG output at 800x480 resolution
- [ ] Test with 3-color palette (black, red, white)
- [ ] Validate against reference images
