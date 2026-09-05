# Bedrock Model Viewer

A focused, standalone viewer for Minecraft **Bedrock Edition** entity geometry.
It does one thing: load a model, find its texture, reconstruct its geometry
correctly, and render it in real 3D (Three.js/WebGL). No editor, no
validator, no fixer, no converter — see [`Scope`](#scope) below.

This project is the **laboratory for MBSM's future 4D/5D viewer**. The
`core/` folder is written so it can be lifted into MBSM later with no
changes (see [Reusing the core in MBSM](#reusing-the-core-in-mbsm)).

## Scope

**Supported:** `format_version` **1.10.0 and above** (1.10.0, 1.12.0, 1.16.0,
1.21.x, and anything newer that keeps the same JSON shape).

**Not supported (on purpose):** `format_version` below 1.10.0 (i.e. 1.8.x).
Files below the threshold are still listed in the sidebar so you can see
they were found, but they're disabled and clearly marked "unsupported" —
the app never tries to render them.

**Not supported (yet):** `poly_mesh` cubes. These are detected and reported
as a warning on the model that uses them (with the exact bone name) rather
than silently skipped or rendered wrong. No real `poly_mesh` example
existed in the resource pack used to build this project, so the code path
was validated with a small synthetic test file
(`tests/synthetic_polymesh_test.geo.json`) rather than a real model — worth
re-testing against a real one if you find it causes trouble.

This viewer is not an editor, validator, fixer, analyzer, comparator, or
skinpack maker. It doesn't modify files. It only shows you what a Bedrock
model actually looks like.

## Running it

No build step. Two ways to open it:

- **Recommended:** serve the folder with any static file server, e.g.
  `python -m http.server 8000` (needed for `fetch()`-based imports to work
  reliably across browsers) — then open `http://localhost:8000/`.
- Some browsers also allow opening `index.html` directly via `file://`,
  but ES module imports are unreliable that way in Chrome specifically.
  Use the local-server method if the page loads blank.

All dependencies (Three.js, `OrbitControls`, JSZip) are vendored locally
under `vendor/` — nothing is fetched from a CDN, so it works offline.

## Loading models

- **Open files…** or drag-and-drop onto the viewport: pick any
  combination of `.geo.json` / `.json` geometry files, `.png` textures,
  and `.zip` / `.mcpack` resource packs, in any order and in any number of
  separate drops.
- A single file can define multiple geometries (Bedrock's
  `"minecraft:geometry"` array, or several legacy `"geometry.*"` keys in
  one file) — all of them show up as separate entries in the **Models**
  list.
- Loading a `.zip`/`.mcpack` scans every `.geo.json`/textures inside it
  without eagerly decoding every PNG (textures are decoded lazily, only
  when actually needed), then lists every geometry it found, format
  version included, so unsupported ones are visible but disabled rather
  than silently dropped.
- **Texture order never matters.** Load the geometry first and the
  texture afterwards, or the other way around — both work, and the model
  never disappears while you're adding files (confirmed in `tests/`, see
  below).
- **Texture association** for resource packs is automatic: candidate
  textures are scored against the model's identifier/path (exact name
  match scores highest, then substring/token overlap, then a small bonus
  for living under `textures/entity/`). If a decent match is found it's
  auto-selected; you can always override it from the **Texture** dropdown
  in the left panel. No match → the model still renders, untextured
  (solid placeholder colour), with "Texture not found" shown rather than
  blocking the viewer.

## Controls

Orbit / pan / zoom (mouse or touch — drag to orbit, right-drag or
shift-drag to pan, wheel/pinch to zoom), **Frame Model** and **Reset
Camera** buttons, plus toggles for **Grid**, **Axes**, **Bones** (skeleton
lines from every bone pivot to its parent's), **Pivots** (small marker at
every bone pivot), **Wireframe**, and **Auto-rotate**, and a background
switch (dark / light / gradient). The camera frames itself to whatever
model is loaded (project requirement: never scale geometry to compensate
for a tiny/huge model — see `utils/CameraFraming.js`).

## Architecture

```
index.html, styles.css, app.js   <- UI only (DOM, file input, toolbar wiring)

core/
  parser/
    FormatVersion.js             <- table-driven format_version support/comparison
    GeometryParser.js            <- BedrockModelParser: thin dispatcher (legacy vs modern)
    Legacy1_10GeometryParser.js  <- ONLY the 1.8.0-1.10.0 document shape ("geometry.*" keys)
    Modern1_12GeometryParser.js  <- ONLY the 1.12.0+ document shape ("minecraft:geometry")
    shared/
      BoneNormalizer.js          <- bone/cube normalization + diagnostics, shared by both generations
  uv/
    UVMapper.js                  <- box UV + per-face UV -> pixel rects, mirror handling
  transforms/
    CoordinateSystem.js          <- THE ONLY place that converts Bedrock units -> Three.js units
  textures/
    TextureManager.js            <- independent texture cache + name-heuristic auto-association
  renderer/
    CubeGeometryBuilder.js       <- per-cube BufferGeometry with explicit per-face UV
    ModelRenderer.js             <- NormalizedModel + texture -> real THREE.Group/Mesh hierarchy
    DebugTools.js                <- grid/axes/bone lines/pivot markers/wireframe/bounding box
  resourcepack/
    ResourcePackLoader.js        <- zip/mcpack scanning (JSZip injected, not imported directly)
    SkinsManifestParser.js       <- skins.json -> {geometry: texture} authoritative map

utils/
  CameraFraming.js               <- dynamic camera distance from a model's bounding sphere

vendor/                          <- Three.js, OrbitControls, JSZip (vendored, no CDN)
tests/                           <- real Bedrock models/textures used to validate this project
```

**Nothing under `core/` touches the DOM or imports from `app.js`.** The
only import `core/` files make outside their own folder is Three.js
(`vendor/three/...`) — the same dependency MBSM's existing 4D/5D code
already has. That's what makes `core/` liftable as a unit later.

### Legacy (1.10) vs Modern (1.12+): why they're two separate files

`Legacy1_10GeometryParser.js` and `Modern1_12GeometryParser.js` have **zero
dependency on each other** — each only imports the shared bone/cube
normalizer. `GeometryParser.js` (`BedrockModelParser`) is a thin dispatcher
that picks one based on the JSON's actual shape (not just its
`format_version` number — see `Legacy1_10GeometryParser.js`'s header: a
file can claim `format_version: "1.10.0"` while still using the *legacy*
document structure, which is common in the wild). This split was requested
specifically so either generation can be lifted into a future MBSM feature
on its own — e.g. a tool that only ever needs to deal with legacy
skinpacks doesn't have to import anything modern-specific, and vice versa.

### Why the renderer had to be rewritten, not patched

The previous `Bedrock_Model_Viewer_v1_model_fix` project (given as a
starting point) rendered geometry with **CSS 3D transforms on `<div>`
elements** (`transform: translate3d(...) rotateX(...) rotateY(...)
rotateZ(...)` on nested divs, with cropped `<canvas>` snippets as textures)
— not WebGL, not a real mesh, no actual UV buffer. That's a fundamentally
different technique from what this project needed (project brief:
"Preferiblemente usa Three.js WebGL/WebGL2"), so rather than patch it, the
renderer was rebuilt in Three.js from scratch. The old project's file
detection / drag-and-drop UX ideas were still useful and are reflected in
`app.js`.

## The Bedrock → Three.js coordinate system

Documented in full (with the reasoning, not just the constants) in
`core/transforms/CoordinateSystem.js`. Summary:

- **Scale:** 1 Bedrock unit = 1/16 Three.js unit (Bedrock models are
  authored at 16 units per block).
- **Position:** `three.x = -bedrock.x / 16`, `three.y = bedrock.y / 16`,
  `three.z = bedrock.z / 16`. Only X is negated.
- **Rotation:** `three.rotation = Euler(-rx, -ry, rz, 'ZYX')` in radians,
  applied per-bone and per-cube around the correct pivot (not just
  `mesh.rotation.x = ...` — see below).
- **Why:** this exact convention (negate X and Y, leave Z, Euler order
  ZYX) is **verified against two independent reference implementations'
  own published source** — Blockbench itself
  (`js/formats/bedrock/bedrock_old.js`: `rotation[0] *= -1; rotation[1]
  *= -1`; `js/io/format.ts`: `euler_order` defaults to `'ZYX'`, not
  overridden by either bedrock format module) and
  `bridge-core/model-viewer` (`lib/Model.ts`: `pivotGroup.rotation.order
  = 'ZYX'`; `.set(degToRad(-rX), degToRad(-rY), degToRad(rZ))`) — not
  derived from first principles or "whatever looked right". An earlier
  version of this file used `Euler(-rx, ry, -rz, 'XYZ')` (X and Z
  negated, XYZ order); seeing `three.rotation.copy(bedrockRotationToThree(...))`
  produce a plausible-looking result for single-axis rotations hid the
  bug for a while — it only became obvious (and got found and fixed) once
  tested against models with genuinely multi-axis rotations. See
  "Golden-test tool" below for how this is now checked automatically
  instead of by eyeballing renders.

### Pivots and hierarchy

`bone.pivot` and `cube.pivot` are **absolute model-space points**, not
positions relative to the parent. Three.js needs relative offsets for
correct nesting, so `ModelRenderer.js` always converts
`(childPivot - parentPivot)` before handing it to
`group.position`/`THREE.Group` nesting — never the raw pivot value. A cube
with its own `pivot`/`rotation` different from its bone's gets its own
intermediate `THREE.Group` so it rotates around the right point without
disturbing its siblings. This was the actual bug class the old CSS-based
viewer was prone to (flattening everything to a single `transformOrigin`
guess).

### UV

Box UV (`"uv":[u,v]`) uses Minecraft's standard 6-face unwrap layout
(documented with the exact pixel formula in `UVMapper.js`). Per-face UV
(`"uv":{"north":{...}, ...}`) is resolved per face directly, including
`uv_size`'s negative-number-means-flip behaviour (verified against a real
example in the pack, `fishing_hook.geo.json`'s "south" face:
`uv_size:[-3,3]`). `mirror` swaps east/west and flips every face
horizontally — it never changes geometry, only texture mapping.

**The one real bug found and fixed during testing:** Three.js textures
default to `flipY = true`, and the pixel-space UV rectangles this project
computes (row 0 = top of the source image, matching how Bedrock/Blockbench
express UV) need an explicit `v_gl = 1 - v_pixel` conversion before being
handed to `BufferGeometry` — without it, every cube silently sampled the
empty/transparent bottom portion of the texture atlas instead of the
painted area. This produced fully invisible (or fully black, with
`alphaTest` disabled) models even though the geometry itself was correct.
Caught by rendering a real 64×64 bee texture from the supplied pack and
noticing the model was invisible; fixed in
`UVMapper.rectToNormalizedUV()`, with the reasoning documented in that
function's comment so it doesn't get "simplified" away later.

### Inflate

Changes rendered cube dimensions only (`size + 2*inflate` on every axis);
the cube's *center* (used for positioning/pivot math) is always computed
from the pre-inflate size, so inflate never shifts a cube's pivot — tested
against `allay.geo.json`, which uses a real *negative* inflate (`-0.2`) on
several cubes.

## Golden-test tool: comparing this project's actual output against Blockbench's actual formula

`tools/compareModel.mjs` is a real comparison tool, not a one-off hand
calculation: it runs this project's own production parser
(`GeometryParser.js`) and renderer (`ModelRenderer.js`) on a geometry
file, independently computes what Blockbench's own published transform
formula would produce for the same file
(`tools/BlockbenchReference.js`), and prints a per-bone / per-cube
position diff table.

```
node tools/compareModel.mjs <path-to-geo.json> <geometry-identifier> [epsilon]
# e.g.
node tools/compareModel.mjs tests/necoarc_hair_bug.geo.json geometry.necoarc_hair_bug
```

Exits non-zero if any bone/cube differs by more than `epsilon` (default
0.01 Three.js units ≈ 0.16 Bedrock units).

**Why this exists:** an earlier round of testing relied on hand-tracing
Blockbench's formula with a calculator for one specific bone, which is
slow, easy to get subtly wrong, and doesn't scale to checking a whole
pack. This tool automates that same idea properly, and finding it via
this tool is how the real rotation-convention bug documented above (XYZ
vs ZYX, wrong axes negated) actually got caught and fixed — hand-tracing
had only been done for single-axis rotations, where the bug happens to
cancel out and look correct.

`BlockbenchReference.js` deliberately doesn't reuse
`core/transforms/CoordinateSystem.js` (comparing code against a copy of
itself proves nothing) — it re-derives Blockbench's exact
position/rotation conventions independently from Blockbench's own source,
and builds a **real THREE.Object3D hierarchy** to do the actual
position/rotation composition (rather than hand-rolling that math too).
That second part matters: an earlier version of this reference tool hand-
rolled the parent→child rotation-chain composition instead, and it was
subtly wrong for chains involving more than one rotated ancestor (found
via a real model, `nyancat`, where the root bone rotates 180° AND a
descendant bone rotates too — a case simple 1-2-level test models never
exercised). Delegating composition to Three.js's own battle-tested
matrix math avoids that whole class of bug while still independently
testing the part that actually matters: are the right values being
plugged in, with the right signs, in the right order.

**Current result: 55 of 55 renderable geometries in the supplied
56-model community skinpack (the 56th, `geometry.null`, is an empty `{}`
with no `bones` and is correctly excluded rather than rendered) match
Blockbench's own formula to within 0.0000 three-units** — including
`necoarc`'s previously-flagged `hairN` bones, `breeze`'s multi-axis
per-cube rotations, and `nyancat`'s multi-level rotation chain. Re-run
this after touching anything in `core/transforms/`, `core/renderer/`, or
either geometry parser.

### The same idea applied to UV: `tools/compareUV.mjs`

After geometry/position was verified, the model's author reported some
models' *textures* looked misplaced even though the geometry now looked
right. Same method, applied to UV this time: `js/outliner/types/cube.js`'s
`updateUV()` (the function that computes what you see when you preview a
cube's UV in Blockbench) was transcribed into
`tools/BlockbenchUVReference.js`, and `tools/compareUV.mjs` diffs this
project's real `UVMapper.js` output against it, face by face, for several
representative cube sizes plus a mirrored case:

```
node tools/compareUV.mjs
```

This immediately found two real, confirmed bugs in the box-UV layout
this project had been using (a plausible-looking formula that turned out
to not match Blockbench's actual one — see the fix in `UVMapper.js` for
the corrected, source-quoted version):

1. **`east` and `west` rectangles were swapped.** Both occupied the
   correct-sized region of the texture, just on the wrong side of each
   other.
2. **`up` and `down` were missing a flip** Blockbench's own formula
   applies (`up` flips both axes, `down` flips the width axis only) —
   confirmed directly from `updateUV()`'s use of negative `size` values
   for those two faces specifically.

Both are now fixed and verified to match Blockbench exactly (including
combined with `mirror`) for cube sizes ranging from perfect cubes to
tall/flat/asymmetric proportions. **Visually confirmed** with a purpose-
built synthetic test asset, `tests/uv_face_test.geo.json` +
`tests/uv_face_test.png` — a single cube with a distinct solid colour
baked into each of the six texel regions (red=south, green=north,
blue=east, yellow=west, white=up, black=down) — rendered from multiple
angles with the debug axes overlay on, confirming all six faces now
sample their correctly-labelled colour. Kept in `tests/` as a permanent
regression fixture: if a future UV change breaks a face again, this cube
will show the wrong colour on the wrong side immediately, without having
to reason about a real texture's more subtle content.

**Why this didn't visually change most of the real skinpack's models
much:** most of them turned out to be genuinely near-monochrome texture
designs (e.g. `coppergirl`, confirmed by zooming into the raw PNG — it's
a legitimately mostly-copper-coloured character, not a broken render), so
swapping which near-identical-looking region gets sampled doesn't produce
an obviously different result. The bug was real and is now fixed either
way; it just wasn't the dramatic visual cause of the "some textures look
misplaced" report on its own. It's a real, generally-applicable
correctness fix (verified against Blockbench's own source, not tuned to
any one model), so it's expected to matter more for texture packs with
strong per-face colour contrast (e.g. a character wearing different-
coloured sleeves or armour plates on each side) than for this
particular skinpack's mostly-uniform designs.

### Round 2: sweeping every real cube, not just a few sampled models

Told (correctly) to apply the same method across the whole pack instead
of chasing one model at a time, the natural next step was to stop
sampling individual models by eye and check EVERY cube's UV in one pass:
`tools/sweepUV.mjs` runs the real parser + `UVMapper.js` against every
box-UV cube in a geometry file (or an entire pack with `--all`) and
diffs each one against `BlockbenchUVReference.js`, instead of only
checking a handful of synthetic sizes.

First pass surfaced a real, separate bug from the east/west/up/down one
above: Blockbench's mirror inheritance rule is `if (cube.mirror ===
undefined) { use the bone's mirror } else { use the cube's own value }`
(quoted directly from `bedrock_old.js`/`bedrock.js`, identical in both).
This project's code did `cube.mirror || bone.mirror` instead — which
silently ignores an explicit `"mirror": false` on a cube whenever its
bone has `"mirror": true"`, forcing it mirrored anyway. Checked whether
this was a real pattern before assuming it mattered: **25 cubes across
the supplied 56-model pack** do exactly this (mostly `rightLeg`/
`rightPants`-style bones explicitly un-mirroring a `mirror:true` they'd
otherwise inherit — e.g. `geometry.enderwomaneyes`'s `rightLeg`,
`geometry.sillycat`'s `fur2`). Fixed with a dedicated
`resolveCubeMirror()` in `UVMapper.js` that matches Blockbench's
tri-state rule exactly (needed `BoneNormalizer.js` to stop collapsing
`cube.mirror` to a plain boolean, since "not declared" and "explicitly
false" have to stay distinguishable for the rule to be checkable at all).

Running the full sweep after both fixes: **1091 of 1091 box-UV cubes
across all 55 renderable geometries in the pack now match Blockbench's
formula** (bounding box, flip direction, and mirror all included; a
small floating-point epsilon and a "flip direction is meaningless when
that dimension is exactly 0" carve-out were needed in the sweep script
itself to avoid false positives on degenerate flat cubes and non-integer
sizes like `3.9` — real precision noise in the comparison, not in either
formula). Per-face UV (`"uv":{north:{...},...}`) isn't covered by this
sweep and remains the narrower-scoped item noted in Known Limitations
below.

## Case study: auditing a real 56-model community skinpack

A community `.mcpack` (56 legacy 1.10.0-structured geometries in a single
`geometry.json`, plus `skins.json`) was used as a second real-world test
after the initial resource-pack-based validation above. Two specific
models were reported as having visible errors: `geometry.necoarc` and
`geometry.shark_girl`. Both were diagnosed **before** writing any fix, as
requested, by loading them in the viewer and inspecting the model info /
warnings panel plus the raw JSON:

**`geometry.necoarc` — floating hair/ear pieces.** This is a genuine
mistake **in the source file**, not a viewer bug. Six `hairN` bones and
four `ear_partN` bones each rotate their own geometry (e.g. `hair2`:
`rotation:[0,0,-12.5]`) around a `pivot` that sits **20-27 Bedrock units
away** from that bone's own cube (measured directly from the JSON, see
`tests/necoarc_hair_bug.geo.json` for an isolated repro). A small rotation
angle applied around a far-away pivot sweeps the geometry through a large
arc — basic circular motion, nothing Bedrock-specific. Any spec-correct
renderer (this one, Blockbench, or the game itself) would render this
exact file exactly this way. **The fix is not something this project
should silently apply**: guessing a "better" pivot would be an
unjustified magic offset, and the same large-pivot-distance pattern is
completely legitimate elsewhere (e.g. a long weapon swinging from a
shoulder socket far away) — flagging it generically for every rotated
bone would make normal, correctly-authored models noisy. What ships
instead: `BoneNormalizer.js` now measures this exact
distance-vs-rotation relationship for every bone and adds a specific,
named warning (bone name + measured distance) to the model's `warnings`
array, shown directly in the info panel — so the next time this happens
(this exact bug was found in **5 of the pack's 56 models**:
`necoarc`, `female`, `CuteCreeper`, `skeletonwoman`, plus the underlying
`ear_part1-4` bones) it's immediately traceable to the source file instead
of being mistaken for a viewer bug again.

**`geometry.shark_girl` — showed as an untextured purple placeholder.**
This one WAS a real gap in the viewer, now fixed. The pack's own
`skins.json` maps `"geometry.shark_girl"` to `"goomba.png"` — an
unrelated-looking filename, presumably because the pack's author renamed
files at some point. `TextureManager`'s name-similarity heuristic
(the only association mechanism the viewer had) scores that pair at 0, so
it correctly reported "texture not found" — but `skins.json` had the
answer the whole time and nothing was reading it.
`SkinsManifestParser.js` now parses `skins.json` when present (from a
raw file drop or found inside a scanned `.zip`/`.mcpack`) into an
authoritative `identifier -> texture` map that's checked **before** the
name heuristic; the heuristic remains as a fallback for archives without
a `skins.json`, or for a manifest entry whose named texture wasn't
actually found (also observed in this same file: `skins.json` maps
`"HatsuneMiku"` to `"hastsunemiku.png"`, a typo — the real file is
`hatsunemiku.png` — which is exactly the case the fallback exists for).

**Verification that the fix didn't change anything it shouldn't have:**
re-rendered every model in the pack after the change. `necoarc`'s render
is pixel-for-pixel the same (as it must be — only a warning was added, no
math changed); `shark_girl` now auto-loads correctly textured from a raw
`.mcpack` drop with no manual texture selection needed. A 13th
previously-undiscovered case also surfaced during this pass:
`geometry.around` turned out to use `poly_mesh` on nearly every one of its
18 bones (a real-world `poly_mesh` example, unlike the synthetic one used
for the initial resource-pack testing) plus one bone with a
genuinely-missing parent (`"rightCopy"` references `"law"`, which isn't
defined anywhere in that geometry) — both now show as clear, specific
warnings instead of an empty/confusing render.

**This was then challenged, correctly, and re-verified with real
evidence instead of taking the first explanation at face value.** The
model's own author pointed out that Blockbench renders `necoarc` cleanly
with no visible separation — direct counter-evidence to the "authoring
mistake" framing above. Rather than defend the original explanation,
this was re-investigated from scratch:

1. Pulled Blockbench's actual published source (`bedrock_old.js`,
   `outliner.js`, `group.js`) and bridge-core's actual published source
   (`Model.ts`, `Cube.ts`) — not memory of either — and hand-traced both
   codebases' exact transform formulas using `hair2`'s real numbers.
2. Both independently compute the same transform this project uses:
   rotate the cube's absolute position around its bone's declared pivot.
   Queried this project's own live renderer for the same bone's computed
   world position and it matched the hand-traced formula to two decimal
   places (5.72 vs 5.71 units) — ruling out an implementation bug on this
   project's side.
3. Asked the model's author to confirm directly in Blockbench's own UI
   (not take my word for the JSON reading): selecting `hair2` and reading
   its rotation panel showed `X:0 Y:0 Z:-12.5` — an exact match to the
   parsed value, ruling out a data-reading error.
4. **What this changed:** the original warning measured the raw
   pivot-to-cube *distance* (26 Bedrock units) and called it "likely an
   authoring mistake" — overstating severity, since a huge pivot distance
   with a small rotation angle barely moves anything. The real, meaningful
   number is the *displacement the rotation actually causes*
   (~5.7 units, ~0.36 blocks) — modest in absolute terms, but larger than
   the thin hair-spike cube (2×5×2 units) it's shifting, which is why it
   reads as "detached" specifically for small pieces. `BoneNormalizer.js`
   now computes and reports that actual rotational displacement (rotating
   the cube's center around its pivot by the declared angle and measuring
   the distance moved) instead of the raw pivot distance, and only warns
   when that displacement exceeds the cube's own bounding diagonal — so
   the warning is calibrated to real visual impact, and its wording no
   longer calls something "likely a mistake" more strongly than the math
   supports. Re-checking the full pack with this calibration surfaced a
   genuinely useful detail the old metric missed: `ear_part1` rotates 30°
   (not 12.5° like the `hairN` bones) and displaces ~13.5 units against a
   1×4×2 cube — a much larger relative displacement, and a better
   explanation for why the rendered result looks more chaotic than any
   single `hairN` bone's modest shift would suggest on its own.

5. **This still wasn't the end of it.** The model's author pushed back a
   second time, correctly: pointing at Blockbench's own visual output and
   asking to trust that over "the math is internally consistent" was the
   right instinct, and the investigation up to that point had only ever
   hand-traced Blockbench's formula for *single-axis* rotations. Building
   the actual comparison tool (`tools/compareModel.mjs` +
   `BlockbenchReference.js`, described above) rather than doing more
   hand calculations immediately surfaced a real bug this project's
   renderer actually had: `CoordinateSystem.js` was negating X and Z with
   Euler order `'XYZ'`; Blockbench's and bridge-core's own source both
   negate X and Y with order `'ZYX'` instead. For a single-axis rotation
   (like `hair2`'s `[0,0,-12.5]`) both conventions happen to produce a
   displacement of the *same magnitude*, just in a different direction —
   which is exactly specific enough to look "roughly plausible" in a
   one-bone hand check while still being wrong, and why it took an actual
   multi-axis test case (`breeze`'s "rods" bone, rotation
   `[-157.5, ±60, 180]`) to force the real bug into the open. Fixed in
   `CoordinateSystem.js`; re-rendering `necoarc` afterward shows the
   `hairN`/`ear_partN` pieces sitting close to the head as a coherent
   (if slightly rough-edged) hairstyle, matching the shape in the
   author's own Blockbench screenshots far more closely than the
   pre-fix render, which scattered them widely around the whole body.
   The remaining small gap between pieces is the genuine, small,
   ~0.36-block data-side displacement from finding #4 above — now
   confirmed to be the ONLY remaining discrepancy, not one bug stacked on
   top of another.

## Validation performed

This project was tested against real files from the resource pack and
skinpack supplied with it (not just a synthetic test cube), rendered with
a headless browser, and inspected visually from multiple camera angles for
anatomical correctness (limbs on the correct side, not floating/crossed,
proportions matching the known creature):

| Test | File | What it covers |
|---|---|---|
| A/B | `spider.geo.json` (1.10.0, **legacy** structure) | multi-bone hierarchy, `texturewidth`/`textureheight` legacy fields, 8-leg symmetry confirmed from a top-down camera angle |
| C/D | `breeze.geo.json` (1.12.0) | cube-level `pivot` + `rotation` distinct from the bone's own (the "rods" bone's 3 cubes) |
| — | `bee.geo.json` (1.21.0) | 2-level hierarchy, multi-axis bone rotation on paired wing bones, this is where the UV flipY bug was caught and fixed |
| E | `fishing_hook.geo.json` (1.12.0) | richest real per-face UV example in the pack (6 faces individually defined, one with a negative `uv_size`) |
| F | `armor_stand.geo.json` (1.12.0) | `mirror:true` cube (`leftarm`); renders as a correctly-proportioned, symmetric armor stand |
| G | `allay.geo.json` (1.21.0) | negative `inflate`, locators, 2-level hierarchy |
| H | `bee.geo.json` / `spider.geo.json` | parent → child bone propagation |
| I | manual (geometry then texture, and reverse order) | confirmed via `tests/` workflow — model never disappears either way |
| J | `tests/mini_resourcepack_test.zip` | zip scanning, multi-geometry-per-file listing, auto texture association, an intentionally-included 1.8.0 file correctly listed as unsupported/disabled |
| poly_mesh | `tests/synthetic_polymesh_test.geo.json` (synthetic — no real example existed in the supplied resources) | detected, reported by name, skipped — never rendered as if it were supported |

**Update on this limitation:** the original version of this section said
comparison was against anatomy/internal-consistency checks only, "not a
pixel-diff against a live Blockbench render — there was no way to
automate driving Blockbench Web from this environment." That's still
true (no live Blockbench instance was ever driven from here), but it
understated what's actually checkable: `tools/compareModel.mjs` compares
this project's real output against Blockbench's own **published
transform formula**, hand-verified line-by-line against Blockbench's
source rather than assumed — and that comparison is what actually caught
the rotation-convention bug described above, which anatomy-only checks
had missed. A live pixel-diff against Blockbench Web would still be a
good addition (it would catch UV/texture-mapping errors this tool doesn't
check, like `mirror`'s texture-only behavior — still not
texture-verified against a real mirrored+textured model), but "no formula
comparison was possible" is no longer accurate.

## Reusing the core in MBSM

Nothing under `core/` imports `app.js`, touches `document`, or hardcodes
this project's file/folder names. To use it from MBSM:

```js
import { BedrockModelParser } from './core/parser/GeometryParser.js';
import { ModelRenderer } from './core/renderer/ModelRenderer.js';
import { TextureManager } from './core/textures/TextureManager.js';

const models = BedrockModelParser.parse(jsonAlreadyParsed, 'my_model.geo.json');
const supported = models.filter(m => m.supported);

const textures = new TextureManager();
textures.registerBlob('my_texture.png', blob);
const image = await textures.getImage('my_texture.png');
const threeTexture = new THREE.Texture(image);
threeTexture.needsUpdate = true;

const built = ModelRenderer.build(supported[0], threeTexture);
mbsmScene.add(built.root);
```

Copy the `core/` and `utils/CameraFraming.js` folders as-is. The only
naming collision risk MBSM should check for is its own use of `Viewer`,
`4D`/`5D`, or `Blockbench`-prefixed globals — every export in this project
is either a named ES module export or explicitly namespaced
(`BedrockModelParser`, `ModelRenderer`, `TextureManager`,
`ResourcePackLoader`, `DebugTools`), never a bare global.

## Adding support for a new format generation later

1. Add a structural branch in `GeometryParser.js`'s `detectStructure()` if
   the new generation's top-level JSON shape differs from "modern"
   (`minecraft:geometry` array + `description`).
2. Add a `normalize<Generation>Entry()` function that outputs the same
   `NormalizedModel`/`NormalizedBone`/`NormalizedCube` shape documented at
   the top of `GeometryParser.js` — everything downstream (`UVMapper.js`,
   `ModelRenderer.js`) only ever consumes that normalized shape and does
   not need to change.
3. Update `FormatVersion.js` if the minimum supported version changes.

## Known limitations

- `poly_mesh` cubes are detected and reported, never rendered (see Scope).
- Box-UV mirroring (the `"uv":[u,v]` + `mirror:true` case) is now verified
  against Blockbench's own `updateUV()` formula via `tools/compareUV.mjs`
  (see the UV case study above). **Per-face UV mirroring**
  (`"uv":{north:{...},...}` + `mirror:true`) is still only cross-checked
  structurally against documented Bedrock behaviour, not against
  Blockbench's source or a textured real-world model — narrower scope
  than the box-UV case, worth verifying the same way if it turns out to
  matter for a real pack.
- Mobile touch controls rely on `OrbitControls`' built-in touch handling
  (one-finger orbit, two-finger pinch/pan); no custom gesture layer was
  built on top since Three.js's own handling already covers the project's
  mobile requirement.
- No animation support (this is a static-pose viewer by design — the
  project brief explicitly scoped out `.animation.json` playback).
