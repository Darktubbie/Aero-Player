/**
 * SkinsManifestParser.js
 * ------------------------------------------------------------------------
 * Community "skinpack" archives (the .mcpack format used for player skin
 * collections, as opposed to a full resource pack with models/entity/...)
 * ship a `skins.json` that explicitly maps each skin to its geometry
 * identifier AND its texture filename:
 *
 *   { "skins": [ { "geometry": "geometry.shark_girl", "texture": "goomba.png", ... } ] }
 *
 * This is the AUTHORITATIVE association -- more reliable than
 * TextureManager's name-similarity heuristic, which fails whenever a
 * texture's filename doesn't resemble the geometry's identifier. Real
 * example that motivated this file: a supplied skinpack's own skins.json
 * maps `geometry.shark_girl` to `goomba.png` (an unrelated-looking
 * filename) -- the name heuristic scores that pair at 0 and reports
 * "Texture not found" even though the correct texture is right there.
 *
 * This module only PARSES the manifest into a lookup map; it doesn't
 * touch TextureManager or the DOM.
 * ------------------------------------------------------------------------
 */

/** @returns {boolean} true if `json` looks like a skins.json manifest. */
export function isSkinsManifest(json) {
  return Boolean(json && Array.isArray(json.skins));
}

export const SkinsManifestParser = {
  /**
   * @param {object} json - parsed skins.json content
   * @returns {Map<string,string>} geometry identifier -> texture filename
   */
  parse(json) {
    const map = new Map();
    if (!isSkinsManifest(json)) return map;
    for (const skin of json.skins) {
      if (skin && typeof skin.geometry === 'string' && typeof skin.texture === 'string') {
        map.set(skin.geometry, skin.texture);
      }
    }
    return map;
  },
};
