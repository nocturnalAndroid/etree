let sliderPanel = null;
let debounceTimer = null;
let defaultParams = {};

function debouncedRun() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(runSimulation, 30);
}

// --- Preset management ---

const PRESETS_KEY = 'etree_presets';

function loadPresets() {
    try {
        return JSON.parse(localStorage.getItem(PRESETS_KEY)) || {};
    } catch {
        return {};
    }
}

function savePresetsToStorage(presets) {
    localStorage.setItem(PRESETS_KEY, JSON.stringify(presets));
}

function populatePresetSelect() {
    const select = document.getElementById('preset-select');
    const presets = loadPresets();

    // Clear existing options except "Default"
    select.innerHTML = '';
    const defaultOpt = document.createElement('option');
    defaultOpt.value = '__default__';
    defaultOpt.textContent = 'Default';
    select.appendChild(defaultOpt);

    for (const name of Object.keys(presets).sort()) {
        const opt = document.createElement('option');
        opt.value = name;
        opt.textContent = name;
        select.appendChild(opt);
    }
}

function applyPreset(name) {
    if (name === '__default__') {
        sliderPanel.setValues(defaultParams);
    } else {
        const presets = loadPresets();
        if (presets[name]) {
            sliderPanel.setValues(presets[name]);
        }
    }
    debouncedRun();
}

function savePreset() {
    const name = prompt('Preset name:');
    if (!name || name.trim() === '') return;
    const trimmed = name.trim();

    const presets = loadPresets();
    presets[trimmed] = sliderPanel.getValues();
    savePresetsToStorage(presets);
    populatePresetSelect();

    document.getElementById('preset-select').value = trimmed;
}

function deletePreset() {
    const select = document.getElementById('preset-select');
    const name = select.value;
    if (name === '__default__') return;

    const presets = loadPresets();
    delete presets[name];
    savePresetsToStorage(presets);
    populatePresetSelect();
    select.value = '__default__';
}

// --- Init & simulation ---

async function init() {
    const response = await fetch('/api/params');
    const metadata = await response.json();

    // Capture defaults
    metadata.forEach(p => { defaultParams[p.name] = p.default; });

    sliderPanel = buildSliders(document.getElementById('sliders'), metadata, debouncedRun);

    document.getElementById('run-btn').addEventListener('click', runSimulation);

    // Preset controls
    populatePresetSelect();
    document.getElementById('preset-select').addEventListener('change', (e) => {
        applyPreset(e.target.value);
    });
    document.getElementById('preset-save').addEventListener('click', savePreset);
    document.getElementById('preset-delete').addEventListener('click', deletePreset);

    // Auto-run on load
    runSimulation();
}

async function runSimulation() {
    const btn = document.getElementById('run-btn');
    const status = document.getElementById('status');
    const grid = document.getElementById('grid');

    btn.disabled = true;
    status.textContent = 'Simulating...';

    try {
        const params = sliderPanel.getValues();
        const response = await fetch('/api/simulate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(params),
        });

        const result = await response.json();
        const { snapshots, global_bounds } = result;

        // Clear grid
        grid.innerHTML = '';

        // Create a canvas for each year
        snapshots.forEach(snapshot => {
            const cell = document.createElement('div');
            cell.className = 'year-cell';

            const label = document.createElement('h3');
            label.textContent = `Year ${snapshot.year}`;

            const canvas = document.createElement('canvas');
            canvas.width = 400;
            canvas.height = 400;

            cell.appendChild(label);
            cell.appendChild(canvas);
            grid.appendChild(cell);

            renderSnapshot(canvas, snapshot, global_bounds);
        });

        status.textContent = `Done. ${snapshots.length} years, ${snapshots[snapshots.length - 1].segments.length} segments.`;
    } catch (err) {
        status.textContent = `Error: ${err.message}`;
        console.error(err);
    } finally {
        btn.disabled = false;
    }
}

document.addEventListener('DOMContentLoaded', init);
