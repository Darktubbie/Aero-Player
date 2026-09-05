/**
 * Modern1_12GeometryParser.js
 * ------------------------------------------------------------------------
 * Parses ONLY the "modern" Bedrock geometry document shape -- format_version
 * 1.12.0 and above. Deliberately kept independent from
 * Legacy1_10GeometryParser.js so either generation can be lifted into
 * another project (e.g. MBSM) on its own, without dragging the other
 * generation's parsing code along.
 *
 * -- Document shape --------------------------------------------------
 * {
 *   "format_version": "1.12.0",
 *   "minecraft:geometry": [
 *     {
 *       "description": {
 *         "identifier": "geometry.foo",
 *         "texture_width": number,      <- note: underscored (unlike legacy)
 *         "texture_height": number,
 *         "visible_bounds_width": number,      (optional)
 *         "visible_bounds_height": number,     (optional)
 *         "visible_bounds_offset": [x,y,z]     (optional)
 *       },
 *       "bones": [ ... ]
 *     },
 *     { ... }    <- a single file may define several geometries
 *   ]
 * }
 * ------------------------------------------------------------------------
 */

import { toVec3, normalizeBones } from './shared/BoneNormalizer.js';

/** @returns {boolean} true if `json` looks like a modern-structured geometry document. */
export function isModernStructure(json) {
  return Boolean(json && Array.isArray(json['minecraft:geometry']));
}

function normalizeModernEntry(entry, sourceFile, formatVersion, index) {
  const warnings = [];
  const desc = entry.description || {};
  const bones = normalizeBones(entry.bones, warnings);
  return {
    sourceFile,
    identifier: desc.identifier || `geometry_${index}`,
    formatVersion,
    formatFamily: 'modern',
    supported: true,
    textureWidth: Number(desc.texture_width) || 64,
    textureHeight: Number(desc.texture_height) || 64,
    visibleBounds: desc.visible_bounds_width
      ? {
          width: Number(desc.visible_bounds_width) || 1,
          height: Number(desc.visible_bounds_height) || 1,
          offset: toVec3(desc.visible_bounds_offset),
        }
      : null,
    bones,
    warnings,
  };
}

export const Modern1_12GeometryParser = {
  /**
   * @param {object} json - parsed JSON document, already confirmed modern-structured
   * @param {string} sourceFile
   * @param {string} formatVersion
   * @returns {object[]} NormalizedModel[] -- one per entry in "minecraft:geometry"
   */
  parse(json, sourceFile, formatVersion) {
    return json['minecraft:geometry'].map((entry, i) =>
      normalizeModernEntry(entry, sourceFile, formatVersion, i)
    );
  },
};
