/**
 * CoordinateSystem.js
 * ------------------------------------------------------------------------
 * The ONLY place in this project that converts Bedrock model-space units
 * into Three.js world-space units. Every constant here is documented; if
 * you need to change one, change it here and nowhere else.
 *
 * -- Unit scale -------------------------------------------------------
 * Bedrock model geometry is authored in units where 16 units = 1 in-game
 * block. This is stated directly in Minecraft Creator's geometry
 * documentation and is why cube sizes like [8,8,8] (half a block) or
 * [16,16,16] (a full block) are common. We render 1 Bedrock unit = 1/16
 * Three.js unit, so a full block-sized cube ends up 1 Three.js unit wide
 * -- convenient for camera/grid math (see ModelRenderer.js debug grid).
 *
 *   BEDROCK_UNIT = 1 / 16
 *
 * -- Axis mapping -------------------------------------------------------
 * Both Bedrock and Three.js are right-handed with +Y up, so no axis needs
 * to be *swapped* for Y or Z. Bedrock's +X, however, points towards
 * what Minecraft calls "west" when looking down -Z (matches the
 * north/south/east/west face-naming table in UVMapper.js: east = +X).
 * Rendered head-on with Three.js's default camera looking down -Z, a
 * Bedrock model's own +X ends up mirrored left/right compared to how
 * Blockbench (and the game itself, from a fixed viewing angle) present
 * it, unless X is negated. We negate X on both position AND rotation
 * for this reason:
 *
 *   three.x = -bedrock.x / 16
 *   three.y =  bedrock.y / 16
 *   three.z =  bedrock.z / 16
 *
 * This was verified empirically against this project's real test models
 * (see /tests): a humanoid-shaped bone hierarchy renders arms/legs on the
 * anatomically correct side, and symmetric paired bones (e.g. bee's
 * leftwing_bone/rightwing_bone, dolphin's left_fin/right_fin) render as
 * proper mirror images of each other rather than crossed/overlapping,
 * only when X is negated consistently for position AND rotation.
 *
 * -- Rotation ---------------------------------------------------------
 * Bedrock rotation values are degrees, applied around the bone/cube
 * pivot. Because X is negated for position, X rotation must ALSO be
 * negated to keep rotation handedness consistent with the mirrored X
 * axis (a positive Bedrock rotation must still turn the model the same
 * *visual* direction after the axis flip).
 *
 * CONFIRMED against TWO independent reference implementations (not
 * assumed): Blockbench's own source (`js/io/format.ts`,
 * `new Property(ModelFormat, 'enum', 'euler_order', {default: 'ZYX'})`,
 * not overridden by either bedrock format module -- and
 * `js/formats/bedrock/bedrock_old.js`, which does
 * `group.rotation[0] *= -1; group.rotation[1] *= -1;` leaving Z
 * untouched) and bridge-core/model-viewer's source (`lib/Model.ts`:
 * `pivotGroup.rotation.order = 'ZYX'; pivotGroup.rotation.set(
 * degToRad(-rX), degToRad(-rY), degToRad(rZ))`). Both negate X AND Y
 * (not Z), and both use Euler order ZYX (not XYZ). An earlier version of
 * this file negated X and Z with order XYZ -- that was wrong, found by
 * hand-tracing Blockbench's actual formula against a real bug report
 * instead of trusting this comment's own prior reasoning. Y is negated
 * despite not being mirrored in position because rotation and position
 * mirroring don't have to follow the same axes -- this is simply what
 * both reference implementations do, verified rather than derived.
 *
 *   three.rotation = Euler(-bedrock.rx, -bedrock.ry, bedrock.rz, 'ZYX')  (radians)
 *
 * If you port this to MBSM's existing viewer/animation code, keep this
 * exact convention -- it's verified against Blockbench's and
 * bridge-core's own published source, not derived from first principles.
 * ------------------------------------------------------------------------
 */

import * as THREE from '../../vendor/three/three.module.min.js';

export const BEDROCK_UNIT = 1 / 16;

/**
 * @param {[number,number,number]} bedrockVec3 - absolute Bedrock-space point
 * @returns {THREE.Vector3}
 */
export function bedrockPositionToThree([x, y, z]) {
  return new THREE.Vector3(-x * BEDROCK_UNIT, y * BEDROCK_UNIT, z * BEDROCK_UNIT);
}

/**
 * @param {[number,number,number]} bedrockDegrees
 * @returns {THREE.Euler}
 */
export function bedrockRotationToThree([rx, ry, rz]) {
  return new THREE.Euler(
    THREE.MathUtils.degToRad(-rx),
    THREE.MathUtils.degToRad(-ry),
    THREE.MathUtils.degToRad(rz),
    'ZYX'
  );
}

/**
 * Converts a Bedrock-space vector DIFFERENCE (e.g. cube-pivot minus
 * bone-pivot, both absolute) into a Three.js-space offset. Same axis
 * convention as bedrockPositionToThree, but named separately so call
 * sites document *why* they're converting a relative value.
 */
export function bedrockOffsetToThree([x, y, z]) {
  return new THREE.Vector3(-x * BEDROCK_UNIT, y * BEDROCK_UNIT, z * BEDROCK_UNIT);
}

export function subtractVec3(a, b) {
  return [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
}
