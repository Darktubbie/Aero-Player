/**
 * CubeGeometryBuilder.js
 * ------------------------------------------------------------------------
 * Builds ONE THREE.BufferGeometry per Bedrock cube, with fully explicit
 * per-vertex positions/normals/UVs -- we do NOT reuse THREE.BoxGeometry's
 * default UV layout because we need each of the 6 faces mapped to an
 * independently-resolved texture rectangle (see UVMapper.js), which is
 * exactly the kind of thing Bedrock's per-face UV format requires.
 *
 * Each face is defined by an outward normal and two tangent axes such
 * that tangentU x tangentV === normal (keeps winding/culling consistent
 * for every face). tangentU is the axis that increases with the
 * texture-rect's U (left->right, "east" in image terms); tangentV is the
 * axis that increases going DOWN the source image (matches how
 * UVMapper.js's rects are expressed in top-down pixel space before being
 * flipped into GL space here).
 * ------------------------------------------------------------------------
 */

import * as THREE from '../../vendor/three/three.module.min.js';
import { rectToNormalizedUV } from '../uv/UVMapper.js';

// bedrock face name -> { normal, tangentU (image-right), tangentV (image-down) }
// in THREE.js LOCAL cube space (already accounting for the global Bedrock
// X flip performed by CoordinateSystem.js -- see that file's header for
// why east/west swap sides here).
const FACE_AXES = {
  west: { normal: [1, 0, 0], tangentU: [0, 0, 1], tangentV: [0, -1, 0] },
  east: { normal: [-1, 0, 0], tangentU: [0, 0, -1], tangentV: [0, -1, 0] },
  up: { normal: [0, 1, 0], tangentU: [1, 0, 0], tangentV: [0, 0, -1] },
  down: { normal: [0, -1, 0], tangentU: [1, 0, 0], tangentV: [0, 0, 1] },
  south: { normal: [0, 0, 1], tangentU: [-1, 0, 0], tangentV: [0, -1, 0] },
  north: { normal: [0, 0, -1], tangentU: [1, 0, 0], tangentV: [0, -1, 0] },
};

function vec(a) {
  return new THREE.Vector3(a[0], a[1], a[2]);
}

/**
 * @param {THREE.Vector3} center - face-plane center in local cube space
 * @param {THREE.Vector3} tangentU
 * @param {THREE.Vector3} tangentV
 * @param {THREE.Vector3} normal
 * @param {number} halfU
 * @param {number} halfV
 * @param {{u0,v0,u1,v1}} glUV - already GL-space (v flipped), u0<u1 left->right, v0=bottom v1=top OR vice versa per flip
 * @param {{positions:number[], normals:number[], uvs:number[], indices:number[]}} buffers
 */
function addFace(center, tangentU, tangentV, normal, halfU, halfV, glUV, buffers) {
  const base = buffers.positions.length / 3;
  // Corners in param space (pu,pv): (-1,-1) (1,-1) (1,1) (-1,1) -> CCW seen from +normal
  const params = [
    [-1, -1],
    [1, -1],
    [1, 1],
    [-1, 1],
  ];
  // uv param -1 -> "start" of rect (u0 / v0 as given), +1 -> "end" (u1 / v1)
  const uvParams = [
    [glUV.u0, glUV.v0],
    [glUV.u1, glUV.v0],
    [glUV.u1, glUV.v1],
    [glUV.u0, glUV.v1],
  ];
  for (let i = 0; i < 4; i++) {
    const [pu, pv] = params[i];
    const pos = center
      .clone()
      .addScaledVector(tangentU, pu * halfU)
      .addScaledVector(tangentV, pv * halfV);
    buffers.positions.push(pos.x, pos.y, pos.z);
    buffers.normals.push(normal.x, normal.y, normal.z);
    buffers.uvs.push(uvParams[i][0], uvParams[i][1]);
  }
  buffers.indices.push(base, base + 1, base + 2, base, base + 2, base + 3);
}

/**
 * Builds a BufferGeometry for one normalized cube.
 * @param {object} cube - NormalizedCube (see GeometryParser.js)
 * @param {Object} resolvedUV - output of UVMapper.resolveCubeUV(cube.uv, cube.size, mirror)
 * @param {number} textureWidth
 * @param {number} textureHeight
 * @returns {THREE.BufferGeometry}
 */
export function buildCubeGeometry(cube, resolvedUV, textureWidth, textureHeight) {
  const inflate = cube.inflate || 0;
  // Render size includes inflate (expands each face outward); UV rectangles
  // were already resolved from the PRE-inflate size by the caller.
  const renderSize = [
    (cube.size[0] + inflate * 2) / 16,
    (cube.size[1] + inflate * 2) / 16,
    (cube.size[2] + inflate * 2) / 16,
  ];
  const half = [renderSize[0] / 2, renderSize[1] / 2, renderSize[2] / 2];

  const buffers = { positions: [], normals: [], uvs: [], indices: [] };
  const center = new THREE.Vector3(0, 0, 0);

  const halfByAxis = { x: half[0], y: half[1], z: half[2] };
  const axisIndex = { x: 0, y: 1, z: 2 };

  for (const [faceName, axes] of Object.entries(FACE_AXES)) {
    const rect = resolvedUV[faceName];
    if (!rect) continue; // per-face UV may omit faces on purpose
    const glUV = rectToNormalizedUV(rect, textureWidth, textureHeight);

    const normal = vec(axes.normal);
    const tangentU = vec(axes.tangentU);
    const tangentV = vec(axes.tangentV);

    // Face plane center = cube center offset along the normal by half the
    // extent on that axis.
    const normalAxisHalf =
      Math.abs(axes.normal[0]) === 1
        ? half[0]
        : Math.abs(axes.normal[1]) === 1
        ? half[1]
        : half[2];
    const faceCenter = center.clone().addScaledVector(normal, normalAxisHalf);

    // Half extents along tangentU / tangentV (whichever axis each points along)
    const halfU =
      Math.abs(axes.tangentU[0]) === 1
        ? half[0]
        : Math.abs(axes.tangentU[1]) === 1
        ? half[1]
        : half[2];
    const halfV =
      Math.abs(axes.tangentV[0]) === 1
        ? half[0]
        : Math.abs(axes.tangentV[1]) === 1
        ? half[1]
        : half[2];

    addFace(faceCenter, tangentU, tangentV, normal, halfU, halfV, glUV, buffers);
  }

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(buffers.positions, 3));
  geometry.setAttribute('normal', new THREE.Float32BufferAttribute(buffers.normals, 3));
  geometry.setAttribute('uv', new THREE.Float32BufferAttribute(buffers.uvs, 2));
  geometry.setIndex(buffers.indices);
  return geometry;
}
