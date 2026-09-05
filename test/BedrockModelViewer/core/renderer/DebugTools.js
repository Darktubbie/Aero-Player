/**
 * DebugTools.js
 * ------------------------------------------------------------------------
 * Optional visual aids (project brief #22): grid, axes, bone pivot
 * markers, wireframe overlay, bounding box. Kept separate from
 * ModelRenderer.js so the "give me a correct model" path never has to
 * import/pay for debug-only code.
 * ------------------------------------------------------------------------
 */

import * as THREE from '../../vendor/three/three.module.min.js';

export function createGrid(sizeInBlocks = 8) {
  const grid = new THREE.GridHelper(sizeInBlocks, sizeInBlocks, 0x7c3aed, 0x2a2a35);
  grid.name = 'DebugGrid';
  return grid;
}

export function createAxes(length = 1) {
  const axes = new THREE.AxesHelper(length);
  axes.name = 'DebugAxes';
  return axes;
}

/** One small sphere per bone pivot, colour-coded so parents/children are distinguishable at a glance. */
export function createPivotMarkers(built) {
  const group = new THREE.Group();
  group.name = 'DebugPivots';
  const geometry = new THREE.SphereGeometry(0.02, 8, 8);
  const material = new THREE.MeshBasicMaterial({ color: 0xffcc33 });
  for (const [name, boneGroup] of built.boneGroups.entries()) {
    const marker = new THREE.Mesh(geometry, material);
    marker.name = `Pivot:${name}`;
    boneGroup.add(marker); // local (0,0,0) of the bone group IS its pivot
    group.add(marker === marker ? new THREE.Object3D() : marker); // keep group non-empty w/o double-adding
  }
  return group;
}

/** Thin lines from each bone pivot to its parent's pivot -- makes the skeleton hierarchy visible. */
export function createBoneLines(built) {
  const group = new THREE.Group();
  group.name = 'DebugBones';
  const material = new THREE.LineBasicMaterial({ color: 0x8b5cf6 });
  for (const [, boneGroup] of built.boneGroups.entries()) {
    if (!(boneGroup.parent && boneGroup.parent.isGroup)) continue;
    const geometry = new THREE.BufferGeometry().setFromPoints([
      new THREE.Vector3(0, 0, 0),
      boneGroup.position.clone(),
    ]);
    const line = new THREE.Line(geometry, material);
    boneGroup.parent.add(line);
    group.add(new THREE.Object3D()); // presence marker only, real line already parented above
  }
  return group;
}

export function setWireframe(built, enabled) {
  built.material.wireframe = enabled;
}

/** Computes an axis-aligned bounding box helper around the whole built model. */
export function createBoundingBoxHelper(rootObject3D) {
  const box = new THREE.BoxHelper(rootObject3D, 0x22c55e);
  box.name = 'DebugBoundingBox';
  return box;
}

export function createWorldOriginMarker() {
  const geometry = new THREE.SphereGeometry(0.03, 8, 8);
  const material = new THREE.MeshBasicMaterial({ color: 0xef4444 });
  const marker = new THREE.Mesh(geometry, material);
  marker.name = 'DebugWorldOrigin';
  return marker;
}
