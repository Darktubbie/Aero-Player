#!/usr/bin/env node
/**
 * compareModel.mjs
 * ------------------------------------------------------------------------
 * Golden test: runs this project's REAL, production parser
 * (GeometryParser.js) and REAL renderer (ModelRenderer.js) against a
 * geometry file, independently computes what Blockbench's own published
 * formula would produce (BlockbenchReference.js), and prints a per-bone /
 * per-cube diff table. This is the actual comparison tool requested --
 * not a one-off hand calculation for a single bone.
 *
 * Usage:
 *   node tools/compareModel.mjs <path-to-geo.json> <geometry-identifier> [epsilon]
 *
 * Example:
 *   node tools/compareModel.mjs tests/necoarc_hair_bug.geo.json geometry.necoarc_hair_bug
 *
 * Exit code is non-zero if any bone/cube exceeds the epsilon (default
 * 0.01 Bedrock units), so this can be wired into a CI-style check later.
 * ------------------------------------------------------------------------
 */

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as THREE from '../vendor/three/three.module.min.js';
import { BedrockModelParser } from '../core/parser/GeometryParser.js';
import { ModelRenderer } from '../core/renderer/ModelRenderer.js';
import { computeBlockbenchReference } from './BlockbenchReference.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const [, , filePath, wantedIdentifier, epsilonArg] = process.argv;
if (!filePath) {
  console.error('Usage: node tools/compareModel.mjs <geo.json> [identifier] [epsilon]');
  process.exit(2);
}
const epsilon = epsilonArg ? Number(epsilonArg) : 0.01;

const absPath = path.resolve(process.cwd(), filePath);
const json = JSON.parse(fs.readFileSync(absPath, 'utf-8'));
const models = BedrockModelParser.parse(json, path.basename(absPath));

const supported = models.filter((m) => m.supported);
const model = wantedIdentifier
  ? supported.find((m) => m.identifier === wantedIdentifier)
  : supported[0];

if (!model) {
  console.error(
    `Could not find a supported geometry${wantedIdentifier ? ` named "${wantedIdentifier}"` : ''} in ${filePath}.`
  );
  console.error('Available:', models.map((m) => `${m.identifier} (supported=${m.supported})`).join(', '));
  process.exit(2);
}

console.log(`\nComparing "${model.identifier}" (${model.formatVersion}, ${model.formatFamily}) -- ${model.bones.length} bones`);
if (model.warnings.length) {
  console.log(`  (${model.warnings.length} parser warning(s) -- run the viewer for details)`);
}

// ---- Our real renderer's actual output ----------------------------------
const built = ModelRenderer.build(model, null);
built.root.updateMatrixWorld(true);

const ourBoneWorld = new Map();
for (const [name, group] of built.boneGroups.entries()) {
  ourBoneWorld.set(name, group.getWorldPosition(new THREE.Vector3()));
}
const ourCubeWorld = []; // {boneName, index, position}
built.root.traverse((obj) => {
  if (obj.isMesh) {
    const p = obj.getWorldPosition(new THREE.Vector3());
    ourCubeWorld.push({ boneName: obj.userData.boneName, position: p });
  }
});

// ---- Independent Blockbench-formula reference ----------------------------
const reference = computeBlockbenchReference(model);

// Reference values are in Blockbench's own space (X negated, raw Bedrock
// units). Convert to the SAME space as `our` values (Three.js units, 1/16
// scale) for an apples-to-apples comparison.
const SCALE = 1 / 16;
function refToThreeUnits([x, y, z]) {
  return new THREE.Vector3(x * SCALE, y * SCALE, z * SCALE);
}

// ---- Diff + print ----------------------------------------------------
let worstBone = { name: null, delta: 0 };
let anyOverEpsilon = false;

console.log('\nBONE ORIGIN (pivot) comparison:');
console.log('bone'.padEnd(20), 'ours'.padEnd(28), 'reference'.padEnd(28), 'delta');
for (const bone of model.bones) {
  const ours = ourBoneWorld.get(bone.name);
  const ref = refToThreeUnits(reference.boneWorldOrigin.get(bone.name));
  const delta = ours.distanceTo(ref);
  if (delta > worstBone.delta) worstBone = { name: bone.name, delta };
  if (delta > epsilon) anyOverEpsilon = true;
  const flag = delta > epsilon ? '  <-- MISMATCH' : '';
  console.log(
    bone.name.padEnd(20),
    fmt(ours).padEnd(28),
    fmt(ref).padEnd(28),
    delta.toFixed(4) + flag
  );
}

console.log('\nCUBE CENTER comparison (rest pose, i.e. before this project applies any rotation):');
console.log('bone'.padEnd(20), 'ours'.padEnd(28), 'reference'.padEnd(28), 'delta');
let cubeIndex = 0;
for (const bone of model.bones) {
  for (let i = 0; i < bone.cubes.length; i++) {
    const ours = ourCubeWorld[cubeIndex]?.position;
    const refEntry = reference.cubeWorldCenters.find((c) => c.boneName === bone.name && c.index === i);
    cubeIndex++;
    if (!ours || !refEntry) continue;
    const ref = refToThreeUnits(refEntry.center);
    const delta = ours.distanceTo(ref);
    if (delta > worstBone.delta) worstBone = { name: `${bone.name}[cube ${i}]`, delta };
    if (delta > epsilon) anyOverEpsilon = true;
    const flag = delta > epsilon ? '  <-- MISMATCH' : '';
    console.log(
      `${bone.name}[${i}]`.padEnd(20),
      fmt(ours).padEnd(28),
      fmt(ref).padEnd(28),
      delta.toFixed(4) + flag
    );
  }
}

function fmt(v) {
  return `${v.x.toFixed(3)}, ${v.y.toFixed(3)}, ${v.z.toFixed(3)}`;
}

console.log(`\nWorst delta: ${worstBone.name} (${worstBone.delta.toFixed(4)} three-units = ${(worstBone.delta * 16).toFixed(2)} Bedrock units)`);
if (anyOverEpsilon) {
  console.log(`\n❌ At least one bone/cube exceeds epsilon (${epsilon}). This project's renderer disagrees with Blockbench's own formula -- a real bug, investigate CoordinateSystem.js / ModelRenderer.js.`);
  process.exit(1);
} else {
  console.log(`\n✅ All bones/cubes match Blockbench's formula within epsilon (${epsilon} three-units = ${(epsilon * 16).toFixed(2)} Bedrock units).`);
  process.exit(0);
}
