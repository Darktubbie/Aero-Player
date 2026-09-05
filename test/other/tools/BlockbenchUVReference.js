/**
 * BlockbenchUVReference.js
 * ------------------------------------------------------------------------
 * Independent re-implementation of Blockbench's EXACT box-UV layout
 * formula, hand-transcribed from `js/outliner/types/cube.js`'s
 * `updateUV()` method (the SAME function that computes what you see when
 * you preview a cube's UV in Blockbench itself). Quoted here so it can
 * be checked line-by-line against the source instead of trusted from
 * memory:
 *
 *   let size = element.size(...)   // [w, h, d] = [x, y, z] cube dimensions
 *   let face_list = [
 *     {face:'east',  from:[0, size[2]],                size:[size[2], size[1]]},
 *     {face:'west',  from:[size[2]+size[0], size[2]],   size:[size[2], size[1]]},
 *     {face:'up',    from:[size[2]+size[0], size[2]],   size:[-size[0], -size[2]]},
 *     {face:'down',  from:[size[2]+size[0]*2, 0],       size:[-size[0], size[2]]},
 *     {face:'south', from:[size[2]*2+size[0], size[2]], size:[size[0], size[1]]},
 *     {face:'north', from:[size[2], size[2]],           size:[size[0], size[1]]},
 *   ]
 *   uv = [from[0]+u, from[1]+v, from[0]+size[0]+u, from[1]+size[1]+v]
 *
 * (mirror_uv handling omitted here -- this project's `mirror` diagnostic
 * is checked separately, see BlockbenchReference.js's header for why
 * position/UV concerns are kept in separate files.)
 *
 * A NEGATIVE size component means the resulting rect is flipped on that
 * axis (from > to) -- `up` is flipped on BOTH axes, `down` only on the
 * u/width axis. Every other face is unflipped. This project's own
 * UVMapper.js must produce the exact same four numbers (x0,y0,x1,y1) per
 * face, flips included, for texture placement to match Blockbench.
 * ------------------------------------------------------------------------
 */

/**
 * @param {[number,number,number]} size - [w,h,d] cube dimensions (post-inflate, matching what Blockbench's element.size() returns)
 * @param {{u:number,v:number}} uvOrigin
 * @returns {Object} {faceName: {x0,y0,x1,y1}} texel-space rect per face (x0/y0 may be > x1/y1, meaning flipped)
 */
export function computeBlockbenchBoxUV(size, uvOrigin) {
  const [w, h, d] = size;
  const { u, v } = uvOrigin;
  const faceList = {
    east: { from: [0, d], size: [d, h] },
    west: { from: [d + w, d], size: [d, h] },
    up: { from: [d + w, d], size: [-w, -d] },
    down: { from: [d + w * 2, 0], size: [-w, d] },
    south: { from: [d * 2 + w, d], size: [w, h] },
    north: { from: [d, d], size: [w, h] },
  };
  const result = {};
  for (const [faceName, f] of Object.entries(faceList)) {
    result[faceName] = {
      x0: f.from[0] + u,
      y0: f.from[1] + v,
      x1: f.from[0] + f.size[0] + u,
      y1: f.from[1] + f.size[1] + v,
    };
  }
  return result;
}

/**
 * Applies Blockbench's exact `mirror_uv` transform on top of an already-
 * computed box layout: `from[0]+=size[0]; size[0]*=-1` for every face
 * (always flips the u-axis, toggling any prior flip rather than forcing
 * one), THEN swaps the entire east/west rects with each other. Transcribed
 * from the same `updateUV()` this file's un-mirrored formula came from.
 * @param {Object} baseRects - output of computeBlockbenchBoxUV
 * @returns {Object} mirrored rects, same {faceName: {x0,y0,x1,y1}} shape
 */
export function applyBlockbenchMirror(baseRects) {
  const flipped = {};
  for (const [face, r] of Object.entries(baseRects)) {
    flipped[face] = { x0: r.x1, y0: r.y0, x1: r.x0, y1: r.y1 };
  }
  const east = flipped.east;
  flipped.east = flipped.west;
  flipped.west = east;
  return flipped;
}
