function renderSnapshot(canvas, snapshot, globalBounds) {
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;

    // Clear
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, w, h);

    if (!snapshot.segments.length) return;

    // Compute scale from global bounds to canvas
    const bx = globalBounds.max_x - globalBounds.min_x;
    const by = globalBounds.max_y - globalBounds.min_y;
    const scale = Math.min(w / bx, h / by);

    // Center the tree in the canvas
    const offsetX = (w - bx * scale) / 2 - globalBounds.min_x * scale;
    const offsetY = (h - by * scale) / 2 - globalBounds.min_y * scale;

    function toScreen(x, y) {
        return [
            x * scale + offsetX,
            h - (y * scale + offsetY)  // flip Y: model Y-up -> screen Y-down
        ];
    }

    // Sort segments by z (back to front) for painter's algorithm
    const sorted = [...snapshot.segments].sort((a, b) => {
        const za = (a.z1 + a.z2) / 2;
        const zb = (b.z1 + b.z2) / 2;
        return za - zb;
    });

    // Draw segments
    sorted.forEach(seg => {
        const [sx1, sy1] = toScreen(seg.x1, seg.y1);
        const [sx2, sy2] = toScreen(seg.x2, seg.y2);

        const lineWidth = Math.max(1, seg.diameter * scale);

        ctx.beginPath();
        ctx.moveTo(sx1, sy1);
        ctx.lineTo(sx2, sy2);
        ctx.strokeStyle = '#2a1a0a';
        ctx.lineWidth = lineWidth;
        ctx.lineCap = 'round';
        ctx.stroke();
    });
}
