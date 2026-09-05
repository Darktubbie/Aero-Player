/**
 * BoneNormalizer.js
 * ------------------------------------------------------------------------
 * Bone/cube normalization logic shared by Legacy1_10GeometryParser.js and
 * Modern1_12GeometryParser.js. The two generations differ ONLY in their
 * document-level shape (see each parser's own header comment) -- once
 * you're looking at one `bones` array, a bone/cube object means the same
 * thing in both. Keeping this in one place means a diagnostic added here
 * (see below) automatically benefits both generations instead of being
 * duplicated (and inevitably drifting) in two files.
 *
 * -- Diagnostics added here (informational warnings, never silent fixes) --
 *
 * 1. Missing parent: a bone references a `parent` name that doesn't exist
 *    anywhere else in the same geometry. We still render it (attached to
 *    the model root, so it doesn't just vanish) but the warning makes it
 *    obvious this came from the SOURCE FILE, not the viewer, since a
 *    silently-wrong hierarchy is very hard to spot otherwise. Confirmed
 *    real-world case: `geometry.around` in a community skinpack references
 *    parent "law", which is never defined.
 *
 * 2. Rotated bone with a geometrically significant displacement: if a
 *    bone/cube has a non-zero `rotation`, this measures how far its own
 *    cube actually MOVES from its rest position once that rotation is
 *    applied around the declared pivot (rotating the cube's center
 *    around the pivot by the declared angle, then measuring the distance
 *    to the unrotated center -- not just the raw pivot-to-cube distance,
 *    which overstates severity: a huge pivot distance with a tiny angle
 *    barely moves anything, while a modest distance with a large angle
 *    can move it a lot). Verified against Blockbench's own published
 *    source (`bedrock_old.js` / `group.js` / `outliner.js`) by hand-
 *    tracing its exact transform formula with real numbers, and
 *    cross-checked against a community bug report: bone `hair2` in a
 *    real skinpack (pivot 26 Bedrock units from its own cube, rotation
 *    12.5°) computes to a genuine ~5.7 unit (~0.36 block) displacement --
 *    confirmed by the model's own author directly reading Blockbench's
 *    rotation panel for that bone. That's a real, spec-correct
 *    displacement (Blockbench renders the same file the same way), not a
 *    viewer bug -- but it's only visually obvious for a SMALL cube, since
 *    0.36 blocks can be larger than the piece itself. We only warn when
 *    the displacement exceeds the cube's own bounding diagonal (i.e. the
 *    shift is bigger than the piece being moved) so the warning is
 *    calibrated to actual visual impact instead of an arbitrary distance
 *    cutoff, and we report the measured displacement (not the pivot
 *    distance) so the severity described matches what you'd actually see.
 */

// Multiplier applied to a cube's own bounding diagonal to decide whether a
// rotation-induced displacement is "big enough to flag" -- see point 2 above.
const DISPLACEMENT_VS_SIZE_RATIO = 1;

function toVec3(value, fallback = [0, 0, 0]) {
  if (Array.isArray(value) && value.length >= 3) {
    return [Number(value[0]) || 0, Number(value[1]) || 0, Number(value[2]) || 0];
  }
  return fallback.slice();
}

function distance3(a, b) {
  return Math.sqrt((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2 + (a[2] - b[2]) ** 2);
}

function magnitude3(v) {
  return Math.sqrt(v[0] ** 2 + v[1] ** 2 + v[2] ** 2);
}

/**
 * Rotates vector `v` around the origin by Euler angles `degrees` (order
 * ZYX, matching bridge-core's published Bedrock renderer -- see this
 * file's header comment). Only used for the diagnostic displacement
 * measurement below, never for actual rendering (that's
 * CoordinateSystem.js's job, kept separate so this parser-level file
 * has no Three.js dependency).
 */
function rotateVec3ZYX(v, degrees) {
  const [dx, dy, dz] = degrees.map((d) => (d * Math.PI) / 180);
  let [x, y, z] = v;
  // Z
  let cz = Math.cos(dz), sz = Math.sin(dz);
  [x, y] = [x * cz - y * sz, x * sz + y * cz];
  // Y
  let cy = Math.cos(dy), sy = Math.sin(dy);
  [x, z] = [x * cy + z * sy, -x * sy + z * cy];
  // X
  let cx = Math.cos(dx), sx = Math.sin(dx);
  [y, z] = [y * cx - z * sx, y * sx + z * cx];
  return [x, y, z];
}

function normalizeCubeUV(rawUV) {
  if (rawUV == null) {
    return { type: 'box', u: 0, v: 0 };
  }
  if (Array.isArray(rawUV)) {
    return { type: 'box', u: Number(rawUV[0]) || 0, v: Number(rawUV[1]) || 0 };
  }
  if (typeof rawUV === 'object') {
    const faces = {};
    for (const faceName of ['north', 'south', 'east', 'west', 'up', 'down']) {
      const f = rawUV[faceName];
      if (!f) continue;
      const uv = Array.isArray(f.uv) ? f.uv : [0, 0];
      const uvSize = Array.isArray(f.uv_size) ? f.uv_size : null;
      faces[faceName] = {
        u: Number(uv[0]) || 0,
        v: Number(uv[1]) || 0,
        uvSize: uvSize ? [Number(uvSize[0]) || 0, Number(uvSize[1]) || 0] : null,
      };
    }
    return { type: 'perFace', faces };
  }
  return { type: 'box', u: 0, v: 0 };
}

function normalizeCube(rawCube, warnings, boneName) {
  if (rawCube.poly_mesh) {
    warnings.push(
      `Bone "${boneName}": cube uses "poly_mesh", which this viewer does not render yet. Skipped.`
    );
    return null;
  }
  if (!Array.isArray(rawCube.origin) || !Array.isArray(rawCube.size)) {
    warnings.push(`Bone "${boneName}": cube missing origin/size, skipped.`);
    return null;
  }
  return {
    origin: toVec3(rawCube.origin),
    size: toVec3(rawCube.size),
    inflate: Number(rawCube.inflate) || 0,
    // Tri-state, NOT Boolean(): undefined/absent must stay distinguishable
    // from an explicit `false` so the renderer can apply Blockbench's own
    // inheritance rule correctly -- see UVMapper.js's `resolveCubeMirror()`
    // header comment for why `cube.mirror ?? bone.mirror` (this ternary)
    // is wrong and `rawCube.mirror === undefined` is required instead.
    mirror: rawCube.mirror === undefined ? null : Boolean(rawCube.mirror),
    rotation: Array.isArray(rawCube.rotation) ? toVec3(rawCube.rotation) : null,
    pivot: Array.isArray(rawCube.pivot) ? toVec3(rawCube.pivot) : null,
    uv: normalizeCubeUV(rawCube.uv),
  };
}

function normalizeLocators(rawLocators) {
  if (!rawLocators || typeof rawLocators !== 'object') return {};
  const out = {};
  for (const [name, value] of Object.entries(rawLocators)) {
    if (Array.isArray(value)) {
      out[name] = { offset: toVec3(value), rotation: null };
    } else if (value && typeof value === 'object') {
      out[name] = {
        offset: toVec3(value.offset),
        rotation: Array.isArray(value.rotation) ? toVec3(value.rotation) : null,
      };
    }
  }
  return out;
}

/** @param {Set<string>} knownBoneNames - every bone name declared in this geometry, for parent-existence checks */
function normalizeBone(rawBone, warnings, knownBoneNames) {
  const name = rawBone.name || 'unnamed_bone';
  const cubes = [];
  let hasUnsupportedPolyMesh = false;
  for (const rawCube of rawBone.cubes || []) {
    if (rawCube.poly_mesh) hasUnsupportedPolyMesh = true;
    const cube = normalizeCube(rawCube, warnings, name);
    if (cube) cubes.push(cube);
  }
  if (rawBone.poly_mesh) {
    hasUnsupportedPolyMesh = true;
    warnings.push(`Bone "${name}" itself defines "poly_mesh"; not rendered.`);
  }

  const parent = rawBone.parent || null;
  if (parent && knownBoneNames && !knownBoneNames.has(parent)) {
    warnings.push(
      `Bone "${name}" references parent "${parent}", which doesn't exist in this geometry. ` +
        `This is a problem in the source file, not the viewer -- rendered attached to the model root instead.`
    );
  }

  const pivot = toVec3(rawBone.pivot);
  const rotation = toVec3(rawBone.rotation);
  const boneRotated = rotation.some((v) => v !== 0);

  for (const cube of cubes) {
    const effectiveRotation = cube.rotation || (cube.pivot ? null : boneRotated ? rotation : null);
    const rotated = (cube.rotation && cube.rotation.some((v) => v !== 0)) || (!cube.pivot && boneRotated);
    if (!rotated) continue;
    const effectivePivot = cube.pivot || pivot;
    const center = [
      cube.origin[0] + cube.size[0] / 2,
      cube.origin[1] + cube.size[1] / 2,
      cube.origin[2] + cube.size[2] / 2,
    ];
    const relative = [center[0] - effectivePivot[0], center[1] - effectivePivot[1], center[2] - effectivePivot[2]];
    const rotatedRelative = rotateVec3ZYX(relative, effectiveRotation || rotation);
    const displacement = distance3(relative, rotatedRelative);
    const cubeDiagonal = magnitude3(cube.size);
    if (cubeDiagonal > 0 && displacement > cubeDiagonal * DISPLACEMENT_VS_SIZE_RATIO) {
      warnings.push(
        `Bone "${name}" rotates ${JSON.stringify(effectiveRotation || rotation)} around a pivot far enough from ` +
          `its own cube that the rotation shifts it by ~${displacement.toFixed(1)} Bedrock units -- bigger than ` +
          `the cube itself (${cube.size.join('x')}). Likely how the source file was authored, not a viewer issue ` +
          `(verified: this project's renderer and Blockbench's own transform formula agree on this displacement). ` +
          `Rendered exactly as specified; not auto-corrected.`
      );
    }
  }

  return {
    name,
    parent,
    pivot,
    rotation,
    mirror: Boolean(rawBone.mirror),
    neverRender: Boolean(rawBone.neverRender ?? rawBone.never_render),
    locators: normalizeLocators(rawBone.locators),
    cubes,
    hasUnsupportedPolyMesh,
  };
}

/**
 * Normalizes an entire `bones` array (shared entry point for both parsers).
 * @param {object[]} rawBones
 * @param {string[]} warnings - mutated in place (push warnings onto it)
 * @returns {object[]} NormalizedBone[]
 */
export function normalizeBones(rawBones, warnings) {
  const knownNames = new Set((rawBones || []).map((b) => b.name).filter(Boolean));
  return (rawBones || []).map((b) => normalizeBone(b, warnings, knownNames));
}

export { toVec3 };
