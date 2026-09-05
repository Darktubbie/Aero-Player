/**
 * UVMapper.js
 * ------------------------------------------------------------------------
 * Pure UV resolution: turns a NormalizedCube's `uv` field (see
 * GeometryParser.js) plus the cube's own size and the model's texture
 * dimensions into a concrete pixel rectangle for each of the 6 cube faces.
 * No Three.js, no DOM. Renderer.js turns these rectangles into normalized
 * (0..1) UV coordinates per vertex.
 *
 * -- Standard "box" UV layout --------------------------------------------
 * Transcribed directly from Blockbench's own source
 * (`js/outliner/types/cube.js`, `Cube.prototype.updateUV`) rather than
 * from memory of "the standard Minecraft box unwrap" -- an earlier
 * version of this file used a plausible-looking but subtly wrong layout
 * (east/west swapped, and missing the flip Blockbench applies to
 * up/down) found by building `tools/compareUV.mjs` and diffing against
 * this exact formula rather than eyeballing renders. Given a cube of
 * size (w, h, d) [x, y, z] and a UV origin (u, v):
 *
 *   east  : x=u,          y=v+d,   w=d, h=h
 *   west  : x=u+d+w,      y=v+d,   w=d, h=h
 *   up    : x=u+d,        y=v,     w=w, h=d,  flipped on BOTH axes
 *   down  : x=u+d+w,      y=v,     w=w, h=d,  flipped on the u/width axis only
 *   south : x=u+2d+w,     y=v+d,   w=w, h=h
 *   north : x=u+d,        y=v+d,   w=w, h=h
 *
 * Face-name <-> world-direction mapping used throughout this project
 * (matches the Bedrock/Java block-facing convention applied to entities):
 *   north = -Z (front)   south = +Z (back)
 *   east  = +X (right)   west  = -X (left)
 *   up    = +Y            down  = -Y
 *
 * -- inflate --------------------------------------------------------------
 * inflate only changes the rendered cube's *dimensions* (each face pushed
 * outward by `inflate` on every side); the UV layout is always computed
 * from the cube's ORIGINAL pre-inflate size, exactly like Blockbench does.
 * That's why `resolveBoxUV`/`resolvePerFaceUV` below take `baseSize`
 * (pre-inflate) rather than the inflated render size.
 *
 * -- mirror ---------------------------------------------------------------
 * `mirror` never changes geometry/position -- only how the texture is
 * mapped (see project brief #13). For the standard box layout, mirroring
 * swaps the east/west rectangles AND flips every face horizontally; this
 * is what lets a single arm texture be reused for both arms. Confirmed
 * against a real mirrored cube in the supplied resource pack
 * (`armor_stand.geo.json`, bone "leftarm", cube mirror:true).
 * ------------------------------------------------------------------------
 */

/**
 * Resolves the effective `mirror` value for a cube, matching Blockbench's
 * own inheritance rule EXACTLY (`js/formats/bedrock/bedrock_old.js` and
 * `bedrock.js`, both identical: `if (s.mirror === undefined) { mirror_uv
 * = group.mirror_uv } else { mirror_uv = s.mirror === true }`). This is
 * NOT the same as `cube.mirror || bone.mirror`: that OR-based version
 * incorrectly ignores an explicit `"mirror": false` on a cube whose bone
 * has `"mirror": true`, forcing it mirrored anyway. Confirmed as a real
 * pattern in the wild (not hypothetical): 25 cubes across a real 56-model
 * community skinpack explicitly set `mirror:false` on a cube nested in a
 * bone with `mirror:true` (e.g. a `rightLeg` bone/cube pair correcting a
 * `mirror:true` inherited from a copy-pasted `leftLeg`-derived bone) --
 * the OR-based version silently mirrored all of them anyway.
 * @param {boolean|null} cubeMirror - NormalizedCube.mirror (null = not declared on the cube itself)
 * @param {boolean} boneMirror - NormalizedBone.mirror
 * @returns {boolean}
 */
export function resolveCubeMirror(cubeMirror, boneMirror) {
  return cubeMirror === null ? boneMirror : cubeMirror;
}

/**
 * @param {{u:number, v:number}} uvOrigin
 * @param {[number,number,number]} baseSize - PRE-inflate [w,h,d]
 * @returns {Object} pixel rects per face: {north,south,east,west,up,down}
 *   each {x,y,w,h,flipU,flipV} -- flipU/flipV here reflect Blockbench's
 *   OWN box-layout flips (up/down), separate from the `mirror` flips
 *   applied afterward in resolveBoxUV.
 */
function computeBoxLayout(uvOrigin, baseSize) {
  const { u, v } = uvOrigin;
  const [w, h, d] = baseSize;
  return {
    east: { x: u, y: v + d, w: d, h, flipU: false, flipV: false },
    west: { x: u + d + w, y: v + d, w: d, h, flipU: false, flipV: false },
    up: { x: u + d, y: v, w, h: d, flipU: true, flipV: true },
    down: { x: u + d + w, y: v, w, h: d, flipU: true, flipV: false },
    south: { x: u + 2 * d + w, y: v + d, w, h, flipU: false, flipV: false },
    north: { x: u + d, y: v + d, w, h, flipU: false, flipV: false },
  };
}

/**
 * Resolves a `{type:'box', u, v}` NormalizedUV into per-face pixel rects
 * (in texel space, NOT yet normalized by texture width/height), applying
 * mirror when requested.
 * @returns {Object} {north,south,east,west,up,down} -> {x,y,w,h,flipU}
 */
export function resolveBoxUV(normalizedUV, baseSize, mirror) {
  const layout = computeBoxLayout({ u: normalizedUV.u, v: normalizedUV.v }, baseSize);
  const faces = {};
  for (const faceName of Object.keys(layout)) {
    const rect = layout[faceName];
    faces[faceName] = { x: rect.x, y: rect.y, w: rect.w, h: rect.h, flipU: rect.flipU, flipV: rect.flipV };
  }
  if (mirror) {
    // Swap east/west rectangles (the physical left/right cube faces now
    // sample the opposite side of the texture)...
    const east = faces.east;
    faces.east = faces.west;
    faces.west = east;
    // ...and flip every face horizontally, matching Bedrock's mirrored
    // box-UV behaviour (a mirrored cube reads its texture strip backwards).
    // Toggle (not force-true): `up`/`down` already carry their own
    // Blockbench-native flip (see computeBoxLayout), and mirroring one
    // that's already flipped un-flips it, exactly like Blockbench's own
    // `from[0]+=size[0]; size[0]*=-1` always swaps from/to regardless of
    // prior state.
    for (const faceName of Object.keys(faces)) {
      faces[faceName].flipU = !faces[faceName].flipU;
    }
  }
  return faces;
}

/**
 * Resolves a `{type:'perFace', faces}` NormalizedUV into per-face pixel
 * rects. Any face missing from the definition is simply omitted from the
 * result (renderer should fall back to "no texture"/skip that face, or a
 * flat colour, rather than guessing).
 *
 * `uv_size` is optional per the Bedrock schema (defaults to that face's
 * own on-model dimensions) and MAY be negative on either axis, which is a
 * flip flag rather than a real negative size (verified against
 * `fishing_hook.geo.json`'s "south" face: uv_size:[-3,3]).
 */
export function resolvePerFaceUV(normalizedUV, baseSize, mirror) {
  const [w, h, d] = baseSize;
  const faceDefaultSize = {
    up: [w, d],
    down: [w, d],
    north: [w, h],
    south: [w, h],
    east: [d, h],
    west: [d, h],
  };

  const faces = {};
  for (const [faceName, def] of Object.entries(normalizedUV.faces)) {
    const [defaultW, defaultH] = faceDefaultSize[faceName] || [1, 1];
    let sizeW = defaultW;
    let sizeH = defaultH;
    let flipU = false;
    let flipV = false;
    if (def.uvSize) {
      sizeW = def.uvSize[0];
      sizeH = def.uvSize[1];
      if (sizeW < 0) {
        flipU = true;
        sizeW = -sizeW;
      }
      if (sizeH < 0) {
        flipV = true;
        sizeH = -sizeH;
      }
    }
    faces[faceName] = { x: def.u, y: def.v, w: sizeW, h: sizeH, flipU, flipV };
  }

  if (mirror) {
    const east = faces.east;
    faces.east = faces.west;
    faces.west = east;
    for (const faceName of Object.keys(faces)) {
      faces[faceName].flipU = !faces[faceName].flipU;
    }
  }

  return faces;
}

/**
 * Single entry point used by the renderer: resolves ANY NormalizedUV
 * (box or perFace) into pixel-space face rectangles.
 * @param {object} normalizedUV
 * @param {[number,number,number]} baseSize pre-inflate cube size
 * @param {boolean} mirror - resolveCubeMirror(cube.mirror, bone.mirror)
 */
export function resolveCubeUV(normalizedUV, baseSize, mirror) {
  if (normalizedUV.type === 'perFace') {
    return resolvePerFaceUV(normalizedUV, baseSize, mirror);
  }
  return resolveBoxUV(normalizedUV, baseSize, mirror);
}

/**
 * Converts a pixel-space rect (from resolveCubeUV) into normalized 0..1
 * GL texture-space UVs, ready to assign to BufferGeometry vertices.
 *
 * IMPORTANT: rect.y / rect.h are in top-down PIXEL row space (row 0 = top
 * of the source image, matching how Bedrock/Blockbench express UV
 * coordinates). Three.js textures default to `flipY = true`
 * (THREE.Texture's documented default for anything loaded from an
 * <img>/<canvas>), under which GL v=1 samples pixel row 0 (the TOP of the
 * source image) and v=0 samples the LAST row (the BOTTOM). We must
 * convert pixel-row-space to that GL convention here -- skipping this
 * conversion silently samples the wrong vertical band of the texture
 * (verified against a real 64x64 bee texture from the supplied resource
 * pack, whose painted pixels only occupy the top ~24 rows: without this
 * flip every cube sampled the empty/transparent bottom of the texture and
 * rendered fully transparent).
 *
 * @returns {{u0:number,v0:number,u1:number,v1:number}}
 */
export function rectToNormalizedUV(rect, textureWidth, textureHeight) {
  if (!textureWidth || !textureHeight) {
    return { u0: 0, v0: 0, u1: 0, v1: 0 };
  }
  let pixelU0 = rect.x / textureWidth;
  let pixelU1 = (rect.x + rect.w) / textureWidth;
  let pixelV0 = rect.y / textureHeight; // top edge (smaller row)
  let pixelV1 = (rect.y + rect.h) / textureHeight; // bottom edge (larger row)
  if (rect.flipU) [pixelU0, pixelU1] = [pixelU1, pixelU0];
  if (rect.flipV) [pixelV0, pixelV1] = [pixelV1, pixelV0];
  // Pixel-row-space -> GL v-space (flipY=true convention): v_gl = 1 - v_pixel
  return { u0: pixelU0, u1: pixelU1, v0: 1 - pixelV0, v1: 1 - pixelV1 };
}
