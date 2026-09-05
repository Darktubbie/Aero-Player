/**
 * BlockbenchReference.js
 * ------------------------------------------------------------------------
 * Independent re-implementation of Blockbench's EXACT bone/cube transform
 * CONVENTIONS, hand-transcribed from Blockbench's own published source
 * (JannisX11/blockbench):
 *
 *   js/formats/bedrock/bedrock_old.js  (legacy parsing: group.origin/
 *                                        rotation negation, cube.from/to)
 *   js/formats/bedrock/bedrock.js      (modern parsing: same negation
 *                                        convention, cube.origin/rotation
 *                                        for per-cube pivot overrides)
 *   js/outliner/types/group.js         (Group.updateTransform: bone
 *                                        position = origin - parent.origin)
 *   js/outliner/outliner.js            (NodePreviewController.updateTransform:
 *                                        generic mesh positioning, used by
 *                                        both Group and Cube -- confirms a
 *                                        cube rotates around its OWN origin
 *                                        directly, no extra nesting)
 *   js/io/format.ts                    (`euler_order` default: 'ZYX', not
 *                                        overridden by either bedrock format)
 *
 * This file exists ONLY as a golden-reference oracle for
 * tools/compareModel.mjs -- it is NEVER imported by the actual viewer
 * (app.js, ModelRenderer.js, etc).
 *
 * IMPORTANT LESSON LEARNED (kept here so it isn't relearned the hard way
 * again): an earlier version of this file hand-rolled the parent/child
 * rotation-chain composition instead of using real nested transforms, and
 * got it wrong for multi-bone rotation chains (root + wingtip0 + tail2 in
 * a real skinpack's `nyancat` model, each rotating on a different axis,
 * exposed a ~47 Bedrock unit divergence that single-bone and two-level
 * test cases never triggered). It ALSO briefly hand-rolled Euler axis
 * composition order and got that backwards too (caught on `breeze`'s
 * multi-axis "rods" cubes). Hierarchical 3D composition is exactly the
 * kind of math that's easy to get SUBTLY wrong by hand while looking
 * internally consistent -- so this file delegates the actual position/
 * rotation COMPOSITION to real THREE.Object3D nesting (the same engine
 * this project's own renderer uses, and the same category of math
 * Blockbench itself ultimately runs on since it's also THREE.js-based),
 * and keeps ONLY the Blockbench-specific CONVENTION choices (which axes
 * get negated, in what order, cube vs bone handling) as the hand-
 * transcribed, independently-reviewable part. That keeps this file
 * meaningfully independent of core/transforms/CoordinateSystem.js
 * (a real bug there still shows up as a real diff here) without
 * re-exposing this project to hand-rolled composition bugs.
 * ------------------------------------------------------------------------
 */

import * as THREE from '../vendor/three/three.module.min.js';

/** Group.origin: bedrock_old.js / bedrock.js both do `origin[0] *= -1` (X only). */
function groupOrigin([x, y, z]) {
  return [-x, y, z];
}

/** Group.rotation: both bedrock formats do `rotation[0] *= -1; rotation[1] *= -1;` (X,Y negated, Z untouched). */
function groupRotationDeg([rx, ry, rz]) {
  return [-rx, -ry, rz];
}

function toEuler([dx, dy, dz]) {
  return new THREE.Euler(THREE.MathUtils.degToRad(dx), THREE.MathUtils.degToRad(dy), THREE.MathUtils.degToRad(dz), 'ZYX');
}

/**
 * bedrock_old.js / bedrock.js cube from/to:
 *   from = origin.slice(); from[0] = -(from[0] + size[0])
 *   to[i] = size[i] + from[i]
 * Returns the cube's center in Blockbench's (mirrored-X) space.
 */
function cubeCenterMirrored(origin, size) {
  const from = [-(origin[0] + size[0]), origin[1], origin[2]];
  const to = [size[0] + from[0], size[1] + from[1], size[2] + from[2]];
  return [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
}

/**
 * Builds a REAL THREE.Object3D hierarchy following Blockbench's exact
 * conventions (see header), so the actual position/rotation COMPOSITION
 * is done by THREE.js itself, not by hand. Mirrors this project's own
 * ModelRenderer.js structurally (Group per bone, Object3D per cube) but
 * is written completely separately, using the groupOrigin/groupRotationDeg
 * conventions above instead of CoordinateSystem.js's.
 *
 * @param {object} model - NormalizedModel
 * @returns {{boneGroups: Map<string,THREE.Object3D>, cubeMarkers: {boneName:string, index:number, marker:THREE.Object3D}[]}}
 */
function buildReferenceHierarchy(model) {
  const boneByName = new Map(model.bones.map((b) => [b.name, b]));
  const boneGroups = new Map();
  const root = new THREE.Group();

  for (const bone of model.bones) {
    boneGroups.set(bone.name, new THREE.Object3D());
  }

  const cubeMarkers = [];

  for (const bone of model.bones) {
    const group = boneGroups.get(bone.name);
    const parentBone = bone.parent ? boneByName.get(bone.parent) : null;
    const parentGroup = parentBone ? boneGroups.get(parentBone.name) : root;
    const parentOrigin = parentBone ? groupOrigin(parentBone.pivot) : [0, 0, 0];
    const ownOrigin = groupOrigin(bone.pivot);

    group.position.set(ownOrigin[0] - parentOrigin[0], ownOrigin[1] - parentOrigin[1], ownOrigin[2] - parentOrigin[2]);
    group.rotation.copy(toEuler(groupRotationDeg(bone.rotation)));
    parentGroup.add(group);

    bone.cubes.forEach((cube, index) => {
      const center = cubeCenterMirrored(cube.origin, cube.size);
      const marker = new THREE.Object3D();

      if (cube.pivot || cube.rotation) {
        // Modern-format cube with its own pivot/rotation: rotates around
        // its OWN origin directly (outliner.js's generic updateTransform
        // -- no extra nested sub-group), then nests under the bone like
        // any other child.
        const cubeOrigin = cube.pivot ? groupOrigin(cube.pivot) : [0, 0, 0];
        const cubeRotationDeg = cube.rotation ? groupRotationDeg(cube.rotation) : [0, 0, 0];
        const cubeMesh = new THREE.Object3D();
        cubeMesh.position.set(
          cubeOrigin[0] - ownOrigin[0],
          cubeOrigin[1] - ownOrigin[1],
          cubeOrigin[2] - ownOrigin[2]
        );
        cubeMesh.rotation.copy(toEuler(cubeRotationDeg));
        group.add(cubeMesh);
        marker.position.set(center[0] - cubeOrigin[0], center[1] - cubeOrigin[1], center[2] - cubeOrigin[2]);
        cubeMesh.add(marker);
      } else {
        marker.position.set(center[0] - ownOrigin[0], center[1] - ownOrigin[1], center[2] - ownOrigin[2]);
        group.add(marker);
      }
      cubeMarkers.push({ boneName: bone.name, index, marker });
    });
  }

  root.updateMatrixWorld(true);
  return { root, boneGroups, cubeMarkers };
}

/**
 * @param {object} model - NormalizedModel (see GeometryParser.js)
 * @returns {{boneWorldOrigin: Map<string,number[]>, cubeWorldCenters: {boneName:string, index:number, center:number[]}[]}}
 */
export function computeBlockbenchReference(model) {
  const { boneGroups, cubeMarkers } = buildReferenceHierarchy(model);

  const boneWorldOrigin = new Map();
  for (const [name, group] of boneGroups.entries()) {
    const p = group.getWorldPosition(new THREE.Vector3());
    boneWorldOrigin.set(name, [p.x, p.y, p.z]);
  }

  const cubeWorldCenters = cubeMarkers.map(({ boneName, index, marker }) => {
    const p = marker.getWorldPosition(new THREE.Vector3());
    return { boneName, index, center: [p.x, p.y, p.z] };
  });

  return { boneWorldOrigin, cubeWorldCenters };
}
