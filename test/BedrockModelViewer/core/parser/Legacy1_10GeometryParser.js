/**
 * Legacy1_10GeometryParser.js
 * ------------------------------------------------------------------------
 * Parses ONLY the "legacy" Bedrock geometry document shape -- the one used
 * from format_version 1.8.0 through 1.10.0. Deliberately kept independent
 * from Modern1_12GeometryParser.js: this project intentionally does not
 * render 1.8.x (out of scope), but the STRUCTURAL shape below is shared
 * by 1.8.0 and 1.10.0 alike, and 1.10.0 legacy-structured files are
 * common in the wild (confirmed: `spider.geo.json` and every custom
 * "4D skinpack" geometry inspected for this project use this shape even
 * when their own `format_version` says 1.10.0).
 *
 * Import this file directly (without GeometryParser.js's dispatcher) if
 * you only ever need to handle the 1.10-and-earlier generation -- e.g. a
 * future MBSM tool that specifically targets legacy skinpacks.
 *
 * -- Document shape --------------------------------------------------
 * {
 *   "format_version": "1.10.0",
 *   "geometry.<name_a>": {
 *     "texturewidth": number,      <- note: no underscore (unlike modern)
 *     "textureheight": number,
 *     "visible_bounds_width": number,      (optional)
 *     "visible_bounds_height": number,     (optional)
 *     "visible_bounds_offset": [x,y,z],    (optional)
 *     "bones": [ ... ]
 *   },
 *   "geometry.<name_b>": { ... }    <- a single file may define several
 * }
 *
 * There is no `description` wrapper -- the geometry's identifier IS the
 * top-level key itself (e.g. `"geometry.spider.v1.8"`).
 * ------------------------------------------------------------------------
 */

import { toVec3, normalizeBones } from './shared/BoneNormalizer.js';

/** @returns {boolean} true if `json` looks like a legacy-structured geometry document. */
export function isLegacyStructure(json) {
  if (!json || typeof json !== 'object') return false;
  return Object.keys(json).some((key) => key !== 'format_version' && key.startsWith('geometry.'));
}

function normalizeLegacyEntry(key, entry, sourceFile, formatVersion) {
  const warnings = [];
  const bones = normalizeBones(entry.bones, warnings);
  return {
    sourceFile,
    identifier: key,
    formatVersion,
    formatFamily: 'legacy',
    supported: true,
    textureWidth: Number(entry.texturewidth) || 64,
    textureHeight: Number(entry.textureheight) || 64,
    visibleBounds: entry.visible_bounds_width
      ? {
          width: Number(entry.visible_bounds_width) || 1,
          height: Number(entry.visible_bounds_height) || 1,
          offset: toVec3(entry.visible_bounds_offset),
        }
      : null,
    bones,
    warnings,
  };
}

export const Legacy1_10GeometryParser = {
  /**
   * @param {object} json - parsed JSON document, already confirmed legacy-structured
   * @param {string} sourceFile
   * @param {string} formatVersion
   * @returns {object[]} NormalizedModel[] -- one per "geometry.*" key found
   */
  parse(json, sourceFile, formatVersion) {
    const results = [];
    for (const [key, value] of Object.entries(json)) {
      if (key === 'format_version') continue;
      if (!key.startsWith('geometry.')) continue;
      if (!value || !Array.isArray(value.bones)) continue;
      results.push(normalizeLegacyEntry(key, value, sourceFile, formatVersion));
    }
    return results;
  },
};
