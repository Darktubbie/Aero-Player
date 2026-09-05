/**
 * FormatVersion.js
 * ------------------------------------------------------------------------
 * Explicit, table-driven Bedrock geometry `format_version` handling.
 *
 * We deliberately do NOT do `format !== "1.8.0"` style checks anywhere in
 * this project. Bedrock format_version strings are dotted version numbers
 * ("1.10.0", "1.21.120", ...) and must be compared numerically component
 * by component, otherwise something like "1.9.0" would incorrectly sort
 * above "1.10.0" as a string.
 *
 * Scope for this project (see project brief):
 *   - 1.8.x                -> NOT supported (out of scope on purpose)
 *   - 1.10.0 and above     -> supported (as long as the structural parser
 *                             for that generation exists, see GeometryParser.js)
 *
 * Two *structural* geometry generations exist in real Bedrock files
 * (verified against the resource pack supplied with this project):
 *
 *   "legacy"  – format_version 1.8.0 .. 1.10.0 (inclusive). Top-level keys
 *               are the geometry identifiers themselves
 *               (e.g. `"geometry.spider.v1.8"`), sizes are given as
 *               `texturewidth` / `textureheight` (no underscore), and the
 *               object holding `bones` has no `description` wrapper.
 *               Confirmed with `spider.geo.json` (format_version 1.10.0)
 *               and `shulker_bullet.geo.json` (1.10.0) from the supplied
 *               resource pack.
 *
 *   "modern"  – format_version 1.12.0 and above. Top-level key is the
 *               array `"minecraft:geometry"`; each entry has a
 *               `description` object (`identifier`, `texture_width`,
 *               `texture_height`, `visible_bounds_*`) and a `bones` array.
 *               Confirmed with `breeze.geo.json` (1.12.0),
 *               `baby_armor_helmet.geo.json` (1.26.10), etc.
 *
 * Both generations exist at 1.10.0 in the wild in this exact pack
 * (`spider.geo.json` is 1.10.0 but still legacy-structured), so structural
 * generation is detected from the JSON shape (see detectStructure below),
 * completely independently from whether the version is "supported".
 */

// Minimum supported version (inclusive). Anything below this is rejected.
const MIN_SUPPORTED = [1, 10, 0];

/**
 * Parses a Bedrock version string ("1.12.0", "1.21.120") into an array of
 * integer components. Missing/garbage components default to 0.
 * @param {string} versionString
 * @returns {number[]} e.g. "1.21.120" -> [1, 21, 120]
 */
export function parseVersion(versionString) {
  const parts = String(versionString ?? '')
    .trim()
    .split('.')
    .map((part) => {
      const n = parseInt(part, 10);
      return Number.isFinite(n) ? n : 0;
    });
  while (parts.length < 3) parts.push(0);
  return parts;
}

/**
 * Compares two parsed version arrays component by component.
 * @returns {number} negative if a < b, 0 if equal, positive if a > b
 */
export function compareVersions(a, b) {
  const len = Math.max(a.length, b.length);
  for (let i = 0; i < len; i++) {
    const diff = (a[i] || 0) - (b[i] || 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

/**
 * @param {string} versionString
 * @returns {boolean} true if this project supports the given format_version.
 */
export function isFormatSupported(versionString) {
  return compareVersions(parseVersion(versionString), MIN_SUPPORTED) >= 0;
}

/**
 * @param {string} versionString
 * @returns {'legacy'|'modern'|null} which structural generation a supported
 * version belongs to, purely from the version number. This is a fallback;
 * prefer detectStructure() in GeometryParser.js which looks at the actual
 * JSON shape, since 1.10.0 files can be legacy-structured in practice.
 */
export function generationForVersion(versionString) {
  if (!isFormatSupported(versionString)) return null;
  const v = parseVersion(versionString);
  return compareVersions(v, [1, 12, 0]) >= 0 ? 'modern' : 'legacy';
}

export const MIN_SUPPORTED_VERSION_STRING = MIN_SUPPORTED.join('.');
