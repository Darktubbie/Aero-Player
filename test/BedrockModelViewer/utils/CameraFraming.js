/**
 * CameraFraming.js
 * ------------------------------------------------------------------------
 * Dynamic camera framing (project brief #23): never scales model
 * geometry to "fix" tiny/huge models -- instead computes a bounding
 * sphere and places the camera at a distance derived from the camera's
 * own field of view, so a 1-block model and an 8-block model both fill a
 * sensible portion of the viewport.
 * ------------------------------------------------------------------------
 */

import * as THREE from '../vendor/three/three.module.min.js';

/**
 * @param {THREE.Object3D} object3D
 * @param {THREE.PerspectiveCamera} camera
 * @param {THREE.Vector3} [directionHint] - normalized direction the camera should sit along, relative to the target (default: a pleasant 3/4 angle, on the model's north/front side so front (-Z) faces are visible by default)
 * @returns {{target: THREE.Vector3, position: THREE.Vector3, radius: number}}
 */
export function frameObject(object3D, camera, directionHint) {
  const box = new THREE.Box3().setFromObject(object3D);
  if (box.isEmpty()) {
    return { target: new THREE.Vector3(0, 0, 0), position: new THREE.Vector3(1.5, 1.2, 1.5), radius: 1 };
  }
  const sphere = new THREE.Sphere();
  box.getBoundingSphere(sphere);
  const radius = Math.max(sphere.radius, 0.05);

  const fovRadians = THREE.MathUtils.degToRad(camera.fov);
  // Distance so the bounding sphere's diameter fits within ~70% of the
  // vertical field of view, leaving breathing room around the model.
  const distance = (radius / Math.sin(fovRadians / 2)) * 1.35;

  // Bedrock/Blockbench convention: north = -Z = front (see UVMapper.js).
  // The direction hint's Z component must therefore be NEGATIVE for the
  // camera to sit on the model's front side by default; a positive Z
  // places the camera behind the model, showing its back (south) faces
  // instead of the front (north) faces.
  const dir = (directionHint || new THREE.Vector3(0.65, 0.5, -0.9)).clone().normalize();
  const position = sphere.center.clone().addScaledVector(dir, distance);

  return { target: sphere.center.clone(), position, radius };
}

/** Applies a frameObject() result to a camera + OrbitControls instance. */
export function applyFraming(camera, controls, framing) {
  camera.position.copy(framing.position);
  camera.near = Math.max(framing.radius / 100, 0.01);
  camera.far = Math.max(framing.radius * 100, 100);
  camera.updateProjectionMatrix();
  controls.target.copy(framing.target);
  controls.update();
}
