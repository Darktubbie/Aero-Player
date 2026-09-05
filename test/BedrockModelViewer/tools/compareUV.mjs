#!/usr/bin/env node
/**
 * compareUV.mjs
 * ------------------------------------------------------------------------
 * Compares this project's REAL UVMapper.js output against Blockbench's
 * exact box-UV formula (BlockbenchUVReference.js) for a set of synthetic
 * cube sizes, printing every face's rect side by side. Run with no args
 * for a default sweep of representative sizes (cube, tall, wide, flat).
 * ------------------------------------------------------------------------
 */
import { resolveBoxUV } from '../core/uv/UVMapper.js';
import { computeBlockbenchBoxUV, applyBlockbenchMirror } from './BlockbenchUVReference.js';

const sizes = [
  [4, 4, 4], // cube
  [8, 12, 4], // tall torso-like
  [4, 12, 4], // limb-like
  [10, 8, 6], // asymmetric
];

let anyMismatch = false;

for (const size of sizes) {
  console.log(`\n=== size [${size.join(',')}] uv=[0,0] ===`);
  const ours = resolveBoxUV({ u: 0, v: 0 }, size, false);
  const reference = computeBlockbenchBoxUV(size, { u: 0, v: 0 });

  console.log('face'.padEnd(8), 'ours (x,y,w,h,flipU,flipV)'.padEnd(38), 'reference (x0,y0,x1,y1)'.padEnd(30), 'match?');
  for (const face of ['north', 'south', 'east', 'west', 'up', 'down']) {
    const o = ours[face];
    const r = reference[face];
    // Normalize reference into (x,y,w,h,flipU,flipV) the same way our resolver expresses it.
    const rFlipU = r.x0 > r.x1;
    const rFlipV = r.y0 > r.y1;
    const rX = Math.min(r.x0, r.x1);
    const rY = Math.min(r.y0, r.y1);
    const rW = Math.abs(r.x1 - r.x0);
    const rH = Math.abs(r.y1 - r.y0);

    const match = o.x === rX && o.y === rY && o.w === rW && o.h === rH && !!o.flipU === rFlipU && !!o.flipV === rFlipV;
    if (!match) anyMismatch = true;

    console.log(
      face.padEnd(8),
      `x:${o.x} y:${o.y} w:${o.w} h:${o.h} flipU:${!!o.flipU} flipV:${!!o.flipV}`.padEnd(38),
      `x:${rX} y:${rY} w:${rW} h:${rH} flipU:${rFlipU} flipV:${rFlipV}`.padEnd(30),
      match ? '✅' : '❌ MISMATCH'
    );
  }
}

// Mirror sanity check (position/size-only invariants, since
// BlockbenchUVReference.js doesn't model mirror_uv -- see its header).
// What we CAN check without re-deriving mirror_uv: mirroring must swap
// east/west entirely (same w/h as before, just relabeled) and must never
// touch north/south/up/down's x/y/w/h, only their flipU.
console.log('\n=== mirror sanity check, size [4,12,4] ===');
{
  const plain = resolveBoxUV({ u: 0, v: 0 }, [4, 12, 4], false);
  const mirrored = resolveBoxUV({ u: 0, v: 0 }, [4, 12, 4], true);
  let ok = true;
  if (mirrored.east.x !== plain.west.x || mirrored.east.w !== plain.west.w) ok = false;
  if (mirrored.west.x !== plain.east.x || mirrored.west.w !== plain.east.w) ok = false;
  for (const f of ['north', 'south', 'up', 'down']) {
    if (mirrored[f].x !== plain[f].x || mirrored[f].w !== plain[f].w) ok = false;
    if (mirrored[f].flipU === plain[f].flipU) ok = false; // must toggle
  }
  console.log(ok ? '✅ mirror swap/flip behaves as expected' : '❌ mirror swap/flip broken');
  if (!ok) anyMismatch = true;
}

console.log(anyMismatch ? '\n❌ UV layout disagrees with Blockbench.' : '\n✅ UV layout matches Blockbench for all tested sizes.');

// ---- Mirror mode: replicate Blockbench's exact mirror_uv algorithm ----
// (from[0]+=size[0]; size[0]*=-1 for every face, THEN swap east/west)

console.log('\n=== mirror=true, size [8,12,4] uv=[0,0] ===');
const oursMirrored = resolveBoxUV({ u: 0, v: 0 }, [8, 12, 4], true);
const referenceMirrored = applyBlockbenchMirror(computeBlockbenchBoxUV([8, 12, 4], { u: 0, v: 0 }));
let mirrorMismatch = false;
console.log('face'.padEnd(8), 'ours'.padEnd(38), 'reference'.padEnd(30), 'match?');
for (const face of ['north', 'south', 'east', 'west', 'up', 'down']) {
  const o = oursMirrored[face];
  const r = referenceMirrored[face];
  const rFlipU = r.x0 > r.x1;
  const rFlipV = r.y0 > r.y1;
  const rX = Math.min(r.x0, r.x1);
  const rY = Math.min(r.y0, r.y1);
  const rW = Math.abs(r.x1 - r.x0);
  const rH = Math.abs(r.y1 - r.y0);
  const match = o.x === rX && o.y === rY && o.w === rW && o.h === rH && !!o.flipU === rFlipU && !!o.flipV === rFlipV;
  if (!match) mirrorMismatch = true;
  console.log(
    face.padEnd(8),
    `x:${o.x} y:${o.y} w:${o.w} h:${o.h} flipU:${!!o.flipU} flipV:${!!o.flipV}`.padEnd(38),
    `x:${rX} y:${rY} w:${rW} h:${rH} flipU:${rFlipU} flipV:${rFlipV}`.padEnd(30),
    match ? '✅' : '❌ MISMATCH'
  );
}
console.log(mirrorMismatch ? '\n❌ Mirror UV disagrees with Blockbench.' : '\n✅ Mirror UV matches Blockbench.');

process.exit(anyMismatch || mirrorMismatch ? 1 : 0);
