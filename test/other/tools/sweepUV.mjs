#!/usr/bin/env node
/**
 * sweepUV.mjs
 * ------------------------------------------------------------------------
 * Like compareModel.mjs, but for UV instead of position/rotation: runs
 * this project's real parser + UVMapper.js against EVERY cube in a real
 * geometry file (not just synthetic sizes), independently computing each
 * face's rect via BlockbenchUVReference.js, and reports any mismatch.
 *
 * Usage:
 *   node tools/sweepUV.mjs <geo.json> [identifier]
 *   node tools/sweepUV.mjs <geo.json> --all     # every supported geometry in the file
 * ------------------------------------------------------------------------
 */
import fs from 'node:fs';
import path from 'node:path';
import { BedrockModelParser } from '../core/parser/GeometryParser.js';
import { resolveCubeUV, resolveCubeMirror } from '../core/uv/UVMapper.js';
import { computeBlockbenchBoxUV, applyBlockbenchMirror } from './BlockbenchUVReference.js';

const [, , filePath, identifierArg] = process.argv;
if (!filePath) {
  console.error('Usage: node tools/sweepUV.mjs <geo.json> [identifier|--all]');
  process.exit(2);
}

const json = JSON.parse(fs.readFileSync(path.resolve(process.cwd(), filePath), 'utf-8'));
const models = BedrockModelParser.parse(json, path.basename(filePath));
const supported = models.filter((m) => m.supported);

const targets =
  identifierArg && identifierArg !== '--all'
    ? supported.filter((m) => m.identifier === identifierArg)
    : supported;

let totalCubes = 0;
let mismatchCubes = 0;
const mismatchDetails = [];

for (const model of targets) {
  for (const bone of model.bones) {
    for (let i = 0; i < bone.cubes.length; i++) {
      const cube = bone.cubes[i];
      if (cube.uv.type !== 'box') continue; // per-face UV not covered by this sweep
      totalCubes++;
      const mirror = resolveCubeMirror(cube.mirror, bone.mirror);
      const ours = resolveCubeUV(cube.uv, cube.size, mirror);
      let reference = computeBlockbenchBoxUV(cube.size, { u: cube.uv.u, v: cube.uv.v });
      if (mirror) reference = applyBlockbenchMirror(reference);

      let cubeMismatch = false;
      const EPS = 1e-6;
      for (const face of ['north', 'south', 'east', 'west', 'up', 'down']) {
        const o = ours[face];
        const r = reference[face];
        const rFlipU = r.x0 > r.x1;
        const rFlipV = r.y0 > r.y1;
        const rX = Math.min(r.x0, r.x1);
        const rY = Math.min(r.y0, r.y1);
        const rW = Math.abs(r.x1 - r.x0);
        const rH = Math.abs(r.y1 - r.y0);
        // Flip direction is unobservable (and Blockbench's own from>to check
        // naturally reports "not flipped") when the corresponding dimension
        // is exactly 0 -- a zero-width/height region has nothing to flip,
        // so only compare flip flags when there's an actual extent to flip.
        const flipUMatters = rW > EPS;
        const flipVMatters = rH > EPS;
        const close = (a, b) => Math.abs(a - b) < EPS;
        if (
          !close(o.x, rX) ||
          !close(o.y, rY) ||
          !close(o.w, rW) ||
          !close(o.h, rH) ||
          (flipUMatters && !!o.flipU !== rFlipU) ||
          (flipVMatters && !!o.flipV !== rFlipV)
        ) {
          cubeMismatch = true;
        }
      }
      if (cubeMismatch) {
        mismatchCubes++;
        mismatchDetails.push(`${model.identifier} / bone "${bone.name}" / cube ${i} (size ${cube.size.join('x')}, uv [${cube.uv.u},${cube.uv.v}], mirror ${mirror})`);
      }
    }
  }
}

console.log(`Checked ${totalCubes} box-UV cubes across ${targets.length} geometries.`);
if (mismatchCubes) {
  console.log(`❌ ${mismatchCubes} mismatch(es):`);
  mismatchDetails.slice(0, 30).forEach((d) => console.log('  ', d));
  process.exit(1);
} else {
  console.log('✅ All box-UV cubes match Blockbench bounding boxes.');
  process.exit(0);
}
