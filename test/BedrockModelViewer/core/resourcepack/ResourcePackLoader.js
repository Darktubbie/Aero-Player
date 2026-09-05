/**
 * ResourcePackLoader.js
 * ------------------------------------------------------------------------
 * Scans a .zip / .mcpack (or any archive JSZip can open) for Bedrock
 * geometry files and PNG textures, without eagerly decoding every texture
 * (project brief #16 / README performance note). JSZip itself is passed
 * in by the caller (dependency injection) so this module has zero direct
 * script-tag/global coupling -- easy to swap for any other zip library.
 * ------------------------------------------------------------------------
 */

import { BedrockModelParser } from '../parser/GeometryParser.js';
import { isSkinsManifest, SkinsManifestParser } from './SkinsManifestParser.js';

function isGeometryFile(name) {
  return /\.geo\.json$/i.test(name) || (/\.json$/i.test(name) && /geometry|models?\//i.test(name));
}
function isTextureFile(name) {
  return /\.png$/i.test(name);
}
function isSkinsManifestFile(name) {
  return /(^|\/)skins\.json$/i.test(name);
}

export const ResourcePackLoader = {
  /**
   * @param {ArrayBuffer|Blob} fileData
   * @param {any} JSZipLib - the JSZip constructor/module (injected)
   * @param {string} archiveName - for diagnostics only
   * @returns {Promise<{models: object[], textureManager: import('../textures/TextureManager.js').TextureManager}>}
   */
  async scan(fileData, JSZipLib, archiveName, textureManager) {
    const zip = await JSZipLib.loadAsync(fileData);
    const models = [];
    let skinsManifest = new Map();

    const entries = Object.values(zip.files).filter((e) => !e.dir);

    // skins.json (if present) is the AUTHORITATIVE geometry->texture map
    // for this archive -- parse it first so callers can prefer it over
    // name-heuristic matching (see SkinsManifestParser.js header).
    for (const entry of entries) {
      if (isSkinsManifestFile(entry.name)) {
        try {
          const json = JSON.parse(await entry.async('text'));
          if (isSkinsManifest(json)) skinsManifest = SkinsManifestParser.parse(json);
        } catch (err) {
          // Non-fatal -- fall back to name-heuristic matching entirely.
        }
        break;
      }
    }

    for (const entry of entries) {
      const name = entry.name;
      if (isTextureFile(name)) {
        textureManager.registerZipEntry(name, entry);
        continue;
      }
      if (isSkinsManifestFile(name)) continue;
      if (isGeometryFile(name)) {
        try {
          const text = await entry.async('text');
          const json = JSON.parse(text);
          const parsed = BedrockModelParser.parse(json, name);
          for (const model of parsed) {
            models.push({ ...model, archiveName, path: name });
          }
        } catch (err) {
          // Not fatal -- keep scanning the rest of the archive. The UI
          // layer can surface `errors` if it wants to.
          models.push({
            sourceFile: name,
            identifier: name,
            supported: false,
            reason: `Could not parse as JSON/geometry: ${err.message}`,
            bones: [],
            warnings: [],
            archiveName,
            path: name,
          });
        }
      }
    }

    return { models, textureCount: textureManager.list().length, skinsManifest };
  },
};
