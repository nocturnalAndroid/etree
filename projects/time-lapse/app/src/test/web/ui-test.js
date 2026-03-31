'use strict';

/**
 * Smoke tests for app.js.
 *
 * app.js is a browser-targeted script that relies on `document`, `fetch`,
 * `setInterval`, `setTimeout`, `Image`, and `alert`.  We provide a minimal
 * hand-rolled DOM stub so no external framework is needed — Node.js built-ins
 * are sufficient.
 *
 * Strategy
 * --------
 * 1.  Build a fake `document` with the elements app.js touches.
 * 2.  Stub `fetch`, `setInterval`, `Image`, etc. in global scope.
 * 3.  Load app.js via require() after setting up globals.
 * 4.  Inspect side-effects (DOM mutations, state object).
 */

// ─── Minimal DOM stub ────────────────────────────────────────────────────────

function makeElement(tag, id) {
  return {
    id,
    tagName: tag.toUpperCase(),
    style: {},
    className: '',
    textContent: '',
    value: '',
    disabled: false,
    innerHTML: '',
    options: [],
    children: [],
    _eventListeners: {},

    getAttribute(name) { return this['_attr_' + name] ?? null; },
    setAttribute(name, val) { this['_attr_' + name] = val; },

    addEventListener(event, fn) {
      this._eventListeners[event] = this._eventListeners[event] || [];
      this._eventListeners[event].push(fn);
    },
    dispatchEvent(event) {
      const listeners = this._eventListeners[event.type] || [];
      listeners.forEach(fn => fn(event));
    },
    querySelectorAll() { return []; },
    querySelector() { return null; },
    appendChild(child) { this.children.push(child); return child; },
  };
}

// IDs that app.js references
const DOM_IDS = [
  'status-dot', 'status-label', 'photo-count',
  'start-btn', 'stop-btn',
  'config-section',
  'camera-select',
  'wb-select',
  'interval-value', 'interval-unit',
  'upload-pending', 'upload-failed',
  'drive-folder-row', 'drive-folder',
  'ntfy-topic', 'settings-msg', 'settings-body', 'settings-chevron',
  'next-capture', 'elapsed-time',
  'last-photo', 'no-photo',
];

const elements = {};
DOM_IDS.forEach(id => { elements[id] = makeElement('div', id); });

// camera-select needs options-like behaviour
elements['camera-select'].options = [];
elements['camera-select'].value = '';

global.document = {
  getElementById(id) { return elements[id] || makeElement('div', id); },
  activeElement: null,
  addEventListener(event, fn) {
    // Capture 'DOMContentLoaded' so we can trigger it manually
    if (event === 'DOMContentLoaded') {
      global._domContentLoadedHandlers = global._domContentLoadedHandlers || [];
      global._domContentLoadedHandlers.push(fn);
    }
  },
  createElement(tag) { return makeElement(tag, '__created__'); },
};

// ─── Other browser globals ────────────────────────────────────────────────────

// Capture interval IDs so we can inspect them
const intervals = [];
global.setInterval = (fn, ms) => { const id = intervals.length; intervals.push({ fn, ms }); return id; };
global.clearInterval = () => {};
global.setTimeout = (fn, ms) => { /* no-op */ };
global.alert = () => {};

// Stub Image constructor
global.Image = function () {
  this.src = '';
  this.onload = null;
  this.onerror = null;
  Object.defineProperty(this, 'src', {
    set(val) { this._src = val; },
    get() { return this._src; },
  });
};

// fetch stub — overridden per-test
global.fetch = () => Promise.reject(new Error('fetch not configured'));

// ─── Load app.js ──────────────────────────────────────────────────────────────

// app.js is a browser-style script that binds everything with `var` at the top
// level.  In Node.js, `require()` wraps the file in a function so those `var`s
// are local — they never reach `global`.  We use vm.runInThisContext() instead,
// which evaluates the source in the current global scope, mirroring a browser
// <script> tag.

const path = require('path');
const fs = require('fs');
const vm = require('vm');
const appJsPath = path.resolve(__dirname, '../../main/assets/web/app.js');
const appJsSource = fs.readFileSync(appJsPath, 'utf8');
vm.runInThisContext(appJsSource, { filename: appJsPath });

// ─── Test harness ─────────────────────────────────────────────────────────────

let passed = 0;
let failed = 0;

function assert(description, condition) {
  if (condition) {
    console.log('  PASS: ' + description);
    passed++;
  } else {
    console.error('  FAIL: ' + description);
    failed++;
  }
}

function assertEqual(description, actual, expected) {
  const ok = actual === expected;
  if (ok) {
    console.log('  PASS: ' + description);
    passed++;
  } else {
    console.error('  FAIL: ' + description + ' — expected ' + JSON.stringify(expected) + ' got ' + JSON.stringify(actual));
    failed++;
  }
}

// ─── Tests ────────────────────────────────────────────────────────────────────

console.log('\n=== formatDuration ===');

// app.js exposes formatDuration on global scope (browser-style script)
assert('formatDuration is a function', typeof formatDuration === 'function');
assertEqual('formatDuration(0) → "0s"',          formatDuration(0),    '0s');
assertEqual('formatDuration(1) → "1s"',          formatDuration(1),    '1s');
assertEqual('formatDuration(59) → "59s"',        formatDuration(59),   '59s');
assertEqual('formatDuration(60) → "1m 00s"',     formatDuration(60),   '1m 00s');
assertEqual('formatDuration(61) → "1m 01s"',     formatDuration(61),   '1m 01s');
assertEqual('formatDuration(3600) → "1h 00m 00s"', formatDuration(3600), '1h 00m 00s');
assertEqual('formatDuration(3661) → "1h 01m 01s"', formatDuration(3661), '1h 01m 01s');
assertEqual('formatDuration(7322) → "2h 02m 02s"', formatDuration(7322), '2h 02m 02s');

console.log('\n=== getIntervalMs ===');

assert('getIntervalMs is a function', typeof getIntervalMs === 'function');

// Set DOM values to drive getIntervalMs
elements['interval-value'].value = '5';
elements['interval-unit'].value = 'seconds';
assertEqual('5 seconds → 5000 ms', getIntervalMs(), 5000);

elements['interval-value'].value = '2';
elements['interval-unit'].value = 'minutes';
assertEqual('2 minutes → 120000 ms', getIntervalMs(), 120000);

elements['interval-value'].value = '1';
elements['interval-unit'].value = 'hours';
assertEqual('1 hour → 3600000 ms', getIntervalMs(), 3600000);

elements['interval-value'].value = '-1';
elements['interval-unit'].value = 'seconds';
assertEqual('negative value → null', getIntervalMs(), null);

elements['interval-value'].value = '0';
elements['interval-unit'].value = 'seconds';
assertEqual('zero value → null', getIntervalMs(), null);

elements['interval-value'].value = 'abc';
elements['interval-unit'].value = 'seconds';
assertEqual('non-numeric value → null', getIntervalMs(), null);

// ─── fetchStatus + DOM update test ────────────────────────────────────────────

console.log('\n=== fetchStatus updates DOM (isRunning=true) ===');

const statusResponse = {
  isRunning: true,
  photoCount: 42,
  startTimeMs: 1700000000000,
  nextCaptureMs: 1700000005000,
  uploadPending: 3,
  uploadFailed: 1,
  driveSessionFolder: 'session_abc',
  cameras: [
    { id: 'main', displayName: 'Main Camera' },
  ],
  ntfyTopic: 'my-topic',
};

global.fetch = () =>
  Promise.resolve({
    ok: true,
    json: () => Promise.resolve(statusResponse),
  });

// Run fetchStatus and let the promise chain settle via a micro-task flush
const fetchDone = new Promise(resolve => {
  // Temporarily wrap updateUI to signal when the update has run
  const origUpdateUI = global.updateUI;
  global.updateUI = function (wasRunning) {
    origUpdateUI(wasRunning);
    resolve();
  };
  fetchStatus();
}).then(() => {
  // Assert DOM side-effects
  // app.js does `countEl.textContent = state.photoCount` — assigns a number,
  // which browsers auto-coerce to string but our stub stores as-is.
  assertEqual(
    'photo-count textContent reflects photoCount',
    String(elements['photo-count'].textContent),
    '42'
  );

  // When isRunning=true: start-btn hidden, stop-btn visible
  assertEqual(
    'start-btn hidden when running',
    elements['start-btn'].style.display,
    'none'
  );
  assertEqual(
    'stop-btn visible when running',
    elements['stop-btn'].style.display,
    'block'
  );

  // Config section should be locked (opacity dimmed, pointer-events disabled)
  assertEqual(
    'config-section opacity dimmed when running',
    elements['config-section'].style.opacity,
    '0.45'
  );
  assertEqual(
    'config-section pointer-events none when running',
    elements['config-section'].style.pointerEvents,
    'none'
  );

  // Upload counts: app.js assigns numbers; coerce to string for comparison.
  assertEqual(
    'upload-pending textContent',
    String(elements['upload-pending'].textContent),
    '3'
  );
  assertEqual(
    'upload-failed textContent',
    String(elements['upload-failed'].textContent),
    '1'
  );

  // Drive folder row visible
  assertEqual(
    'drive-folder textContent',
    elements['drive-folder'].textContent,
    'session_abc'
  );
  assertEqual(
    'drive-folder-row visible',
    elements['drive-folder-row'].style.display,
    'flex'
  );

  // ntfy topic field updated (field not focused → should update)
  assertEqual(
    'ntfy-topic value updated from status',
    elements['ntfy-topic'].value,
    'my-topic'
  );
}).catch(err => {
  console.error('  FAIL: fetchStatus promise rejected:', err.message);
  failed++;
});

// ─── fetchStatus + DOM update test (isRunning=false) ─────────────────────────

const fetchDone2 = fetchDone.then(() => {
  console.log('\n=== fetchStatus updates DOM (isRunning=false) ===');

  const idleResponse = { ...statusResponse, isRunning: false, driveSessionFolder: null };

  global.fetch = () =>
    Promise.resolve({
      ok: true,
      json: () => Promise.resolve(idleResponse),
    });

  return new Promise(resolve => {
    const origUpdateUI2 = global.updateUI;
    global.updateUI = function (wasRunning) {
      origUpdateUI2(wasRunning);
      resolve();
    };
    fetchStatus();
  }).then(() => {
    assertEqual(
      'stop-btn hidden when stopped',
      elements['stop-btn'].style.display,
      'none'
    );
    assertEqual(
      'start-btn visible when stopped',
      elements['start-btn'].style.display,
      'block'
    );
    assertEqual(
      'config-section fully enabled when stopped',
      elements['config-section'].style.opacity,
      '1'
    );
    assertEqual(
      'drive-folder-row hidden when no folder',
      elements['drive-folder-row'].style.display,
      'none'
    );
  });
});

// ─── Final summary ────────────────────────────────────────────────────────────

fetchDone2.finally(() => {
  console.log('\n─────────────────────────────────');
  console.log(`Results: ${passed} passed, ${failed} failed`);
  if (failed > 0) process.exit(1);
});
