/**
 * ModelRenderer.js
 * ------------------------------------------------------------------------
 * Builds a real Three.js Object3D hierarchy (Group per bone, Mesh per
 * cube, proper parent/child nesting) from a NormalizedModel
 * (GeometryParser.js) and an optional texture. This is the "CORE" piece
 * meant to be reusable outside this standalone viewer (see project brief
 * section 2/40) -- it never touches the DOM and only imports Three.js and
 * sibling core/ modules.
 *
 * Usage:
 *   const built = ModelRenderer.build(normalizedModel, texture);
 *   scene.add(built.root);
 *   // built.boneGroups: Map<boneName, THREE.Group>       (for debug tools / animation)
 *   // built.cubePivotHelpers: THREE.Object3D[]           (small markers, see DebugTools.js)
 * ------------------------------------------------------------------------
 */

import * as THREE from '../../vendor/three/three.module.min.js';
import { bedrockOffsetToThree, bedrockRotationToThree, subtractVec3 } from '../transforms/CoordinateSystem.js';
import { resolveCubeUV, resolveCubeMirror } from '../uv/UVMapper.js';
import { buildCubeGeometry } from './CubeGeometryBuilder.js';

function makeMaterial(texture) {
  return new THREE.MeshLambertMaterial({
    map: texture || null,
    color: texture ? 0xffffff : 0x9d7bdb,
    transparent: true,
    alphaTest: 0.05,
    side: THREE.FrontSide,
  });
}

export const ModelRenderer = {
  /**
   * @param {object} model - NormalizedModel (must have model.supported === true)
   * @param {THREE.Texture|null} texture - already-created THREE.Texture, or null for an untextured placeholder
   * @returns {{root: THREE.Group, boneGroups: Map<string, THREE.Group>, cubeCount: number, pivotPoints: {boneName:string, position:THREE.Vector3}[]}}
   */
  build(model, texture) {
    const material = makeMaterial(texture);
    const root = new THREE.Group();
    root.name = `Model:${model.identifier}`;

    const boneByName = new Map(model.bones.map((b) => [b.name, b]));
    const boneGroups = new Map();
    const pivotPoints = [];

    // First pass: create an empty Group per bone so parent lookups always
    // succeed regardless of declaration order in the JSON.
    for (const bone of model.bones) {
      const group = new THREE.Group();
      group.name = `Bone:${bone.name}`;
      group.userData.boneName = bone.name;
      group.userData.mirror = bone.mirror;
      group.visible = !bone.neverRender;
      boneGroups.set(bone.name, group);
    }

    // Second pass: position/rotate + nest into parent, then attach cubes.
    for (const bone of model.bones) {
      const group = boneGroups.get(bone.name);
      const parentBone = bone.parent ? boneByName.get(bone.parent) : null;
      const parentGroup = parentBone ? boneGroups.get(parentBone.name) : root;
      const parentPivot = parentBone ? parentBone.pivot : [0, 0, 0];

      // Bedrock bone.pivot is an ABSOLUTE model-space point, but Three.js
      // hierarchy needs each child's position RELATIVE to its parent -- so
      // we convert the difference, not the raw pivot (see CoordinateSystem.js).
      const relativePivot = subtractVec3(bone.pivot, parentPivot);
      group.position.copy(bedrockOffsetToThree(relativePivot));
      group.rotation.copy(bedrockRotationToThree(bone.rotation));

      (parentGroup || root).add(group);
      pivotPoints.push({ boneName: bone.name, position: group.getWorldPosition(new THREE.Vector3()) });

      for (const cube of bone.cubes) {
        const cubeMirror = resolveCubeMirror(cube.mirror, bone.mirror);
        const resolvedUV = resolveCubeUV(cube.uv, cube.size, cubeMirror);
        const geometry = buildCubeGeometry(cube, resolvedUV, model.textureWidth, model.textureHeight);
        const mesh = new THREE.Mesh(geometry, material);
        mesh.userData.boneName = bone.name;

        const cubeCenterAbs = [
          cube.origin[0] + cube.size[0] / 2,
          cube.origin[1] + cube.size[1] / 2,
          cube.origin[2] + cube.size[2] / 2,
        ];

        if (cube.rotation || cube.pivot) {
          // Cube has its own pivot different from the bone's -- needs an
          // intermediate group so the rotation happens around the right point.
          const cubePivotAbs = cube.pivot || bone.pivot;
          const pivotGroup = new THREE.Group();
          pivotGroup.name = `CubePivot`;
          const relPivot = subtractVec3(cubePivotAbs, bone.pivot);
          pivotGroup.position.copy(bedrockOffsetToThree(relPivot));
          if (cube.rotation) {
            pivotGroup.rotation.copy(bedrockRotationToThree(cube.rotation));
          }
          const relCenter = subtractVec3(cubeCenterAbs, cubePivotAbs);
          mesh.position.copy(bedrockOffsetToThree(relCenter));
          pivotGroup.add(mesh);
          group.add(pivotGroup);
        } else {
          const relCenter = subtractVec3(cubeCenterAbs, bone.pivot);
          mesh.position.copy(bedrockOffsetToThree(relCenter));
          group.add(mesh);
        }
      }
    }

    const cubeCount = model.bones.reduce((sum, b) => sum + b.cubes.length, 0);
    return { root, boneGroups, cubeCount, pivotPoints, material };
  },

  /** Swaps the texture on an already-built model without rebuilding geometry (fast path for "load texture after geometry"). */
  applyTexture(built, texture) {
    built.material.map = texture || null;
    built.material.color.set(texture ? 0xffffff : 0x9d7bdb);
    built.material.needsUpdate = true;
  },
};
