/**
 * app.js
 * ------------------------------------------------------------------------
 * UI glue ONLY. Everything that understands Bedrock geometry, UV, or
 * coordinate transforms lives under core/ and utils/ and has zero
 * knowledge of the DOM. This file's job is: read files, call the core,
 * and reflect state into the page. This separation is what will let the
 * core/ folder be lifted into MBSM later without dragging this file's
 * document.querySelector calls along with it (see project brief #39/40).
 * ------------------------------------------------------------------------
 */

import * as THREE from './vendor/three/three.module.min.js';
import { OrbitControls } from './vendor/three/OrbitControls.js';

import { BedrockModelParser } from './core/parser/GeometryParser.js';
import { MIN_SUPPORTED_VERSION_STRING } from './core/parser/FormatVersion.js';
import { TextureManager } from './core/textures/TextureManager.js';
import { ModelRenderer } from './core/renderer/ModelRenderer.js';
import * as DebugTools from './core/renderer/DebugTools.js';
import { ResourcePackLoader } from './core/resourcepack/ResourcePackLoader.js';
import { isSkinsManifest, SkinsManifestParser } from './core/resourcepack/SkinsManifestParser.js';
import { frameObject, applyFraming } from './utils/CameraFraming.js';

/* global JSZip */

// ---------------------------------------------------------------------
// DOM references
// ---------------------------------------------------------------------
const $ = (id) => document.getElementById(id);
const el = {
  fileInput: $('fileInput'),
  openBtn: $('openBtn'),
  emptyOpenBtn: $('emptyOpenBtn'),
  statusText: $('statusText'),
  modelCount: $('modelCount'),
  modelSearch: $('modelSearch'),
  modelList: $('modelList'),
  textureSelect: $('textureSelect'),
  viewportWrap: $('viewportWrap'),
  viewportCanvasHost: $('viewportCanvasHost'),
  emptyState: $('emptyState'),
  modelInfo: $('modelInfo'),
  warningsBlock: $('warningsBlock'),
  btnFrame: $('btnFrame'),
  btnResetCamera: $('btnResetCamera'),
  toggleGrid: $('toggleGrid'),
  toggleAxes: $('toggleAxes'),
  toggleBones: $('toggleBones'),
  togglePivots: $('togglePivots'),
  toggleWireframe: $('toggleWireframe'),
  toggleAutoRotate: $('toggleAutoRotate'),
  backgroundSelect: $('backgroundSelect'),
  footerHint: $('footerHint'),
};

el.footerHint.textContent = `Drag & drop files anywhere · Supported: ${MIN_SUPPORTED_VERSION_STRING}+`;

// ---------------------------------------------------------------------
// Application state
// ---------------------------------------------------------------------
const state = {
  models: [], // NormalizedModel[] (flattened across every loaded file/archive)
  textureManager: new TextureManager(),
  skinsManifest: new Map(), // geometry identifier -> texture filename, from skins.json (authoritative when present)
  selectedIndex: -1,
  built: null, // current ModelRenderer.build() result
  debugGroups: {}, // grid/axes/bones/pivots THREE objects currently in the scene
  initialCameraFraming: null,
};

// ---------------------------------------------------------------------
// Three.js scene setup
// ---------------------------------------------------------------------
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(45, 1, 0.01, 1000);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
el.viewportCanvasHost.appendChild(renderer.domElement);

const controls = new OrbitControls(camera, renderer.domElement);
controls.enableDamping = true;
controls.dampingFactor = 0.08;

const hemiLight = new THREE.HemisphereLight(0xffffff, 0x444455, 1.1);
scene.add(hemiLight);
const dirLight = new THREE.DirectionalLight(0xffffff, 0.9);
dirLight.position.set(2, 3, 2);
scene.add(dirLight);
const dirLight2 = new THREE.DirectionalLight(0xffffff, 0.35);
dirLight2.position.set(-2, -1, -2);
scene.add(dirLight2);

const modelGroup = new THREE.Group();
modelGroup.name = 'CurrentModel';
scene.add(modelGroup);

const BACKGROUNDS = {
  dark: () => new THREE.Color(0x0b0b0f),
  light: () => new THREE.Color(0xe9e9ee),
};
function applyBackground(mode) {
  if (mode === 'gradient') {
    scene.background = null;
    el.viewportWrap.style.background = 'radial-gradient(circle at 30% 20%, #241a3d, #0b0b0f 70%)';
  } else {
    el.viewportWrap.style.background = '';
    scene.background = BACKGROUNDS[mode] ? BACKGROUNDS[mode]() : BACKGROUNDS.dark();
  }
}
applyBackground('dark');

function resizeRenderer() {
  const w = el.viewportCanvasHost.clientWidth || 1;
  const h = el.viewportCanvasHost.clientHeight || 1;
  renderer.setSize(w, h, false);
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
}
new ResizeObserver(resizeRenderer).observe(el.viewportCanvasHost);
resizeRenderer();

function animate() {
  requestAnimationFrame(animate);
  if (el.toggleAutoRotate.checked) modelGroup.rotation.y += 0.006;
  controls.update();
  renderer.render(scene, camera);
}
animate();

// ---------------------------------------------------------------------
// File loading
// ---------------------------------------------------------------------
function setStatus(text, isError = false) {
  el.statusText.textContent = text;
  el.statusText.style.color = isError ? '#f87171' : '';
}

function isTextureFile(name) {
  return /\.(png)$/i.test(name);
}
function isSkinsManifestFile(name) {
  return /(^|[/\\])skins\.json$/i.test(name);
}
function isGeometryFile(name) {
  return /\.geo\.json$/i.test(name) || /\.json$/i.test(name);
}
function isArchiveFile(name) {
  return /\.(zip|mcpack)$/i.test(name);
}

async function processFiles(fileList) {
  const files = Array.from(fileList || []);
  if (!files.length) return;
  setStatus('Loading…');

  let addedModels = 0;
  let addedTextures = 0;
  const jsonFiles = [];
  const manifestFiles = [];

  for (const file of files) {
    if (isArchiveFile(file.name)) {
      try {
        const { models, skinsManifest } = await ResourcePackLoader.scan(file, JSZip, file.name, state.textureManager);
        state.models.push(...models);
        addedModels += models.length;
        for (const [geo, tex] of skinsManifest.entries()) state.skinsManifest.set(geo, tex);
      } catch (err) {
        console.error('Could not open archive', file.name, err);
        setStatus(`Could not open ${file.name}`, true);
      }
    } else if (isTextureFile(file.name)) {
      state.textureManager.registerBlob(file.name, file);
      addedTextures++;
    } else if (isSkinsManifestFile(file.name)) {
      manifestFiles.push(file);
    } else if (isGeometryFile(file.name)) {
      jsonFiles.push(file);
    }
  }

  for (const file of manifestFiles) {
    try {
      const json = JSON.parse(await file.text());
      if (isSkinsManifest(json)) {
        const parsed = SkinsManifestParser.parse(json);
        for (const [geo, tex] of parsed.entries()) state.skinsManifest.set(geo, tex);
      }
    } catch (err) {
      console.warn('Invalid skins.json', file.name, err);
    }
  }

  for (const file of jsonFiles) {
    try {
      const json = JSON.parse(await file.text());
      const parsed = BedrockModelParser.parse(json, file.name);
      state.models.push(...parsed);
      addedModels += parsed.length;
    } catch (err) {
      console.warn('Invalid JSON', file.name, err);
    }
  }

  refreshModelList();
  refreshTextureSelect();

  const parts = [];
  if (addedModels) parts.push(`${addedModels} model${addedModels === 1 ? '' : 's'}`);
  if (addedTextures) parts.push(`${addedTextures} texture${addedTextures === 1 ? '' : 's'}`);
  setStatus(parts.length ? `Loaded ${parts.join(' + ')}` : 'No recognizable Bedrock files found', !parts.length);

  if (state.selectedIndex === -1 && state.models.some((m) => m.supported)) {
    const firstSupportedIndex = state.models.findIndex((m) => m.supported);
    selectModel(firstSupportedIndex);
  } else if (state.selectedIndex !== -1) {
    // A texture may have just arrived for the already-selected model.
    refreshTextureSelect();
    applySelectedTexture();
  }

  el.emptyState.classList.toggle('hidden', state.models.length > 0 || state.textureManager.list().length > 0);
}

// ---------------------------------------------------------------------
// Model list UI
// ---------------------------------------------------------------------
function refreshModelList() {
  const query = (el.modelSearch.value || '').toLowerCase();
  el.modelCount.textContent = String(state.models.filter((m) => m.supported).length);
  el.modelList.innerHTML = '';

  if (!state.models.length) {
    const p = document.createElement('p');
    p.className = 'empty-hint';
    p.textContent = 'Open a .geo.json, a texture, or drop a whole resource pack (.zip / .mcpack).';
    el.modelList.appendChild(p);
    return;
  }

  state.models.forEach((model, index) => {
    if (query && !model.identifier.toLowerCase().includes(query)) return;
    const btn = document.createElement('button');
    btn.className = 'model-item' + (index === state.selectedIndex ? ' active' : '') + (!model.supported ? ' unsupported' : '');
    const idSpan = document.createElement('span');
    idSpan.className = 'id';
    idSpan.textContent = model.identifier;
    const metaSpan = document.createElement('span');
    metaSpan.className = 'meta';
    metaSpan.textContent = model.supported
      ? `${model.formatVersion} · ${model.bones.length} bones`
      : `${model.formatVersion || '?'} · unsupported`;
    btn.appendChild(idSpan);
    btn.appendChild(metaSpan);
    btn.title = model.supported ? model.identifier : model.reason || 'Unsupported';
    btn.disabled = !model.supported;
    btn.addEventListener('click', () => selectModel(index));
    el.modelList.appendChild(btn);
  });
}
el.modelSearch.addEventListener('input', refreshModelList);

// ---------------------------------------------------------------------
// Texture selection UI
// ---------------------------------------------------------------------
function refreshTextureSelect() {
  const model = state.models[state.selectedIndex];
  const candidates = model ? state.textureManager.findCandidates(model.identifier + ' ' + (model.sourceFile || '')) : state.textureManager.findCandidates('');
  const manifestTexture = model ? state.skinsManifest.get(model.identifier) : null;
  const manifestAvailable = manifestTexture && state.textureManager.has(manifestTexture);

  el.textureSelect.innerHTML = '';
  const autoOption = document.createElement('option');
  autoOption.value = '';
  if (manifestAvailable) {
    autoOption.textContent = `Auto · ${manifestTexture} (from skins.json)`;
  } else if (candidates.length && candidates[0].score > 0) {
    autoOption.textContent = `Auto · ${candidates[0].name}`;
  } else {
    autoOption.textContent = 'Auto · none found';
  }
  el.textureSelect.appendChild(autoOption);
  for (const c of candidates.slice(0, 40)) {
    const opt = document.createElement('option');
    opt.value = c.name;
    opt.textContent = c.name;
    el.textureSelect.appendChild(opt);
  }
}
el.textureSelect.addEventListener('change', () => applySelectedTexture());

async function resolveTextureForCurrentModel() {
  const manualName = el.textureSelect.value;
  if (manualName) return manualName;
  const model = state.models[state.selectedIndex];
  if (!model) return null;

  // skins.json is authoritative when present -- but only trust it if the
  // texture it names was actually loaded (a real-world skins.json in the
  // wild had a typo, "hastsunemiku.png" vs the real "hatsunemiku.png";
  // falling through to the heuristic below handles that gracefully).
  const manifestTexture = state.skinsManifest.get(model.identifier);
  if (manifestTexture && state.textureManager.has(manifestTexture)) {
    return manifestTexture;
  }

  const candidates = state.textureManager.findCandidates(model.identifier + ' ' + (model.sourceFile || ''));
  return candidates.length && candidates[0].score > 0 ? candidates[0].name : null;
}

// ---------------------------------------------------------------------
// Model selection / building
// ---------------------------------------------------------------------
async function selectModel(index) {
  const model = state.models[index];
  if (!model || !model.supported) return;
  state.selectedIndex = index;
  refreshModelList();
  refreshTextureSelect();

  buildAndShow(model, null);
  const textureName = await resolveTextureForCurrentModel();
  if (textureName) {
    try {
      const texture = await loadThreeTexture(textureName);
      if (state.selectedIndex === index) {
        ModelRenderer.applyTexture(state.built, texture);
        updateInfoPanel(model, textureName);
      }
    } catch (err) {
      console.warn('Texture load failed', err);
      updateInfoPanel(model, null);
    }
  } else {
    updateInfoPanel(model, null);
  }
}

async function applySelectedTexture() {
  const model = state.models[state.selectedIndex];
  if (!model || !state.built) return;
  const textureName = await resolveTextureForCurrentModel();
  if (!textureName) {
    ModelRenderer.applyTexture(state.built, null);
    updateInfoPanel(model, null);
    return;
  }
  try {
    const texture = await loadThreeTexture(textureName);
    ModelRenderer.applyTexture(state.built, texture);
    updateInfoPanel(model, textureName);
  } catch (err) {
    console.warn(err);
  }
}

const threeTextureCache = new Map();
async function loadThreeTexture(name) {
  if (threeTextureCache.has(name)) return threeTextureCache.get(name);
  const image = await state.textureManager.getImage(name);
  const texture = new THREE.Texture(image);
  texture.magFilter = THREE.NearestFilter;
  texture.minFilter = THREE.NearestFilter;
  texture.generateMipmaps = false;
  texture.colorSpace = THREE.SRGBColorSpace;
  texture.needsUpdate = true;
  threeTextureCache.set(name, texture);
  return texture;
}

function clearDebugGroups() {
  for (const obj of Object.values(state.debugGroups)) {
    if (obj) scene.remove(obj);
  }
  state.debugGroups = {};
}

function buildAndShow(model, texture) {
  modelGroup.clear();
  clearDebugGroups();

  const built = ModelRenderer.build(model, texture);
  state.built = built;
  modelGroup.add(built.root);
  el.toggleWireframe.dispatchEvent(new Event('change'));

  const framing = frameObject(built.root, camera, new THREE.Vector3(0.65, 0.5, -0.9));
  state.initialCameraFraming = framing;
  applyFraming(camera, controls, framing);

  refreshDebugOverlays();
  el.emptyState.classList.add('hidden');
}

function refreshDebugOverlays() {
  if (!state.built) return;
  clearDebugGroups();
  if (el.toggleGrid.checked) {
    const grid = DebugTools.createGrid(8);
    scene.add(grid);
    state.debugGroups.grid = grid;
  }
  if (el.toggleAxes.checked) {
    const axes = DebugTools.createAxes(1);
    scene.add(axes);
    state.debugGroups.axes = axes;
  }
  if (el.toggleBones.checked) {
    const lines = DebugTools.createBoneLines(state.built);
    state.debugGroups.bones = lines;
  }
  if (el.togglePivots.checked) {
    const pivots = DebugTools.createPivotMarkers(state.built);
    state.debugGroups.pivots = pivots;
  }
}

function updateInfoPanel(model, textureName) {
  const cubeCount = model.bones.reduce((sum, b) => sum + b.cubes.length, 0);
  const usesPerFace = model.bones.some((b) => b.cubes.some((c) => c.uv.type === 'perFace'));
  const usesMirror = model.bones.some((b) => b.mirror || b.cubes.some((c) => c.mirror));
  const usesInflate = model.bones.some((b) => b.cubes.some((c) => c.inflate));
  const polyMeshCount = model.bones.filter((b) => b.hasUnsupportedPolyMesh).length;

  el.modelInfo.innerHTML = `
    <h3>${escapeHtml(model.identifier)}</h3>
    <div class="info-row"><span>format_version</span><span>${escapeHtml(model.formatVersion)}</span></div>
    <div class="info-row"><span>structure</span><span>${model.formatFamily}</span></div>
    <div class="info-row"><span>bones</span><span>${model.bones.length}</span></div>
    <div class="info-row"><span>cubes</span><span>${cubeCount}</span></div>
    <div class="info-row"><span>texture size</span><span>${model.textureWidth}×${model.textureHeight}</span></div>
    <div class="info-row"><span>uv</span><span>${usesPerFace ? 'box + per-face' : 'box'}</span></div>
    <div class="info-row"><span>mirror</span><span>${usesMirror ? 'yes' : 'no'}</span></div>
    <div class="info-row"><span>inflate</span><span>${usesInflate ? 'yes' : 'no'}</span></div>
    <div class="info-row"><span>poly_mesh bones</span><span>${polyMeshCount}</span></div>
    <div class="info-row"><span>texture</span><span>${textureName ? escapeHtml(textureName.split('/').pop()) : 'Texture not found'}</span></div>
    <div class="info-row"><span>source</span><span title="${escapeHtml(model.sourceFile || '')}">${escapeHtml((model.sourceFile || '').split('/').pop())}</span></div>
  `;

  if (model.warnings && model.warnings.length) {
    el.warningsBlock.hidden = false;
    el.warningsBlock.innerHTML = `<strong>${model.warnings.length} warning(s)</strong><ul>${model.warnings
      .map((w) => `<li>${escapeHtml(w)}</li>`)
      .join('')}</ul>`;
  } else {
    el.warningsBlock.hidden = true;
  }
}

function escapeHtml(str) {
  return String(str ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ---------------------------------------------------------------------
// Toolbar wiring
// ---------------------------------------------------------------------
el.btnFrame.addEventListener('click', () => {
  if (!state.built) return;
  const framing = frameObject(state.built.root, camera, new THREE.Vector3(0.65, 0.5, -0.9));
  applyFraming(camera, controls, framing);
});
el.btnResetCamera.addEventListener('click', () => {
  if (state.initialCameraFraming) applyFraming(camera, controls, state.initialCameraFraming);
});
[el.toggleGrid, el.toggleAxes, el.toggleBones, el.togglePivots].forEach((toggle) =>
  toggle.addEventListener('change', refreshDebugOverlays)
);
el.toggleWireframe.addEventListener('change', () => {
  if (state.built) DebugTools.setWireframe(state.built, el.toggleWireframe.checked);
});
el.backgroundSelect.addEventListener('change', () => applyBackground(el.backgroundSelect.value));

// ---------------------------------------------------------------------
// File input / drag & drop
// ---------------------------------------------------------------------
el.openBtn.addEventListener('click', () => el.fileInput.click());
el.emptyOpenBtn.addEventListener('click', () => el.fileInput.click());
el.fileInput.addEventListener('change', (e) => {
  processFiles(e.target.files);
  e.target.value = '';
});

['dragover', 'dragenter'].forEach((ev) =>
  window.addEventListener(ev, (e) => {
    e.preventDefault();
  })
);
window.addEventListener('drop', (e) => {
  e.preventDefault();
  processFiles(e.dataTransfer.files);
});

// Keyboard: R to reset camera.
window.addEventListener('keydown', (e) => {
  if (e.key.toLowerCase() === 'r' && !['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement?.tagName)) {
    el.btnResetCamera.click();
  }
});
