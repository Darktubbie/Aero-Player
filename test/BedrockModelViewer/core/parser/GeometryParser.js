/**
 * GeometryParser.js  (BedrockModelParser)
 * ------------------------------------------------------------------------
 * Thin dispatcher over the two generation-specific parsers:
 *   - Legacy1_10GeometryParser.js  (format_version 1.8.0 .. 1.10.0 shape)
 *   - Modern1_12GeometryParser.js  (format_version 1.12.0+ shape)
 *
 * This file exists so the REST of the project (ModelRenderer.js,
 * ResourcePackLoader.js, app.js) never has to know or care which
 * generation a file turned out to be -- they just call
 * `BedrockModelParser.parse(json, sourceFile)` like before. If you only
 * need ONE generation (e.g. a future MBSM feature scoped to 1.10-only
 * skinpacks), import Legacy1_10GeometryParser.js or
 * Modern1_12GeometryParser.js directly instead of this dispatcher --
 * they have zero dependency on each other.
 *
 * See either generation-specific file for its exact document shape, and
 * see core/parser/shared/BoneNormalizer.js for the bone/cube
 * normalization + diagnostics both generations share.
 *
 * -- NormalizedModel shape (produced by either generation) ------------
 * {
 *   sourceFile: string,
 *   identifier: string,
 *   formatVersion: string,
 *   formatFamily: 'legacy' | 'modern' | null,
 *   supported: boolean,
 *   reason?: string,              // present when supported === false
 *   textureWidth: number,
 *   textureHeight: number,
 *   visibleBounds: {width, height, offset:[x,y,z]} | null,
 *   bones: NormalizedBone[],
 *   warnings: string[],           // human-readable, safe to show in UI as-is
 * }
 *
 * NormalizedBone / NormalizedCube: see BoneNormalizer.js header comment.
 * ------------------------------------------------------------------------
 */

import { isFormatSupported, parseVersion, compareVersions } from './FormatVersion.js';
import { isLegacyStructure, Legacy1_10GeometryParser } from './Legacy1_10GeometryParser.js';
import { isModernStructure, Modern1_12GeometryParser } from './Modern1_12GeometryParser.js';

function unsupportedResult(sourceFile, formatVersion, reason) {
  return {
    sourceFile,
    identifier: sourceFile,
    formatVersion,
    formatFamily: null,
    supported: false,
    reason,
    textureWidth: 0,
    textureHeight: 0,
    visibleBounds: null,
    bones: [],
    warnings: [],
  };
}

export const BedrockModelParser = {
  /**
   * Parses a raw Bedrock geometry JSON document (already JSON.parse()'d)
   * into zero or more NormalizedModel objects. A single file may define
   * several geometries, all are returned.
   *
   * Unsupported format_versions (< 1.10.0, i.e. 1.8.x) are still returned
   * so the caller/UI can list & explain them, but with `supported:false`
   * and no bones -- callers must check `.supported` before rendering.
   *
   * @param {object} json - parsed JSON document
   * @param {string} sourceFile - filename, used for diagnostics/UI only
   * @returns {object[]} NormalizedModel[]
   */
  parse(json, sourceFile = 'unknown') {
    const formatVersion = String(json?.format_version ?? 'unknown');

    if (!isFormatSupported(formatVersion)) {
      return [
        unsupportedResult(
          sourceFile,
          formatVersion,
          `format_version ${formatVersion} is below the minimum supported 1.10.0 (1.8.x is intentionally out of scope for this viewer).`
        ),
      ];
    }

    if (isModernStructure(json)) {
      return Modern1_12GeometryParser.parse(json, sourceFile, formatVersion);
    }

    if (isLegacyStructure(json)) {
      return Legacy1_10GeometryParser.parse(json, sourceFile, formatVersion);
    }

    return [
      unsupportedResult(
        sourceFile,
        formatVersion,
        'Could not recognize this file as Bedrock geometry (no "minecraft:geometry" array and no "geometry.*" key found).'
      ),
    ];
  },

  isFormatSupported,
  parseVersion,
  compareVersions,
};
