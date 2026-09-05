/**
 * TextureManager.js
 * ------------------------------------------------------------------------
 * Independent texture cache. Geometry and textures can be loaded in any
 * order (project requirement #15) -- this manager just stores whatever
 * arrives and lets callers ask for a texture by name later. It does not
 * know anything about bones/cubes/geometry.
 *
 * A "texture record" is { name, source } where source is one of:
 *   - a Blob (from a raw file input)
 *   - a JSZip file entry (from an opened .zip/.mcpack, lazily read)
 * plus a decoded { image, canvas, width, height } once loaded.
 * ------------------------------------------------------------------------
 */

export class TextureManager {
  constructor() {
    /** @type {Map<string, {name:string, blob?:Blob, zipEntry?:any, image?:HTMLImageElement}>} */
    this.records = new Map();
  }

  /** Registers a texture from a raw File/Blob (e.g. drag-and-drop PNG). */
  registerBlob(name, blob) {
    this.records.set(name, { name, blob });
  }

  /** Registers a texture from an opened JSZip entry (lazy -- not read yet). */
  registerZipEntry(name, zipEntry) {
    this.records.set(name, { name, zipEntry });
  }

  has(name) {
    return this.records.has(name);
  }

  list() {
    return Array.from(this.records.values());
  }

  clear() {
    this.records.clear();
  }

  /**
   * Decodes (if needed) and returns an HTMLImageElement for the named
   * texture. Cached after first decode.
   * @param {string} name
   * @returns {Promise<HTMLImageElement>}
   */
  async getImage(name) {
    const record = this.records.get(name);
    if (!record) throw new Error(`Unknown texture "${name}"`);
    if (record.image) return record.image;

    let blob = record.blob;
    if (!blob && record.zipEntry) {
      blob = await record.zipEntry.async('blob');
    }
    if (!blob) throw new Error(`Texture "${name}" has no data source`);

    const image = await new Promise((resolve, reject) => {
      const url = URL.createObjectURL(blob);
      const img = new Image();
      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve(img);
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error(`Could not decode texture "${name}"`));
      };
      img.src = url;
    });

    record.image = image;
    return image;
  }

  /**
   * Very small heuristic used for resource-pack auto-association
   * (project requirement #17). Scores a candidate texture name against a
   * model identifier/source path. Higher = more likely match. This is
   * intentionally simple and explainable rather than "clever":
   *   +10 exact basename match
   *   +6  one name contains the other
   *   +2  per shared underscore/dash/space/dot-separated token
   *   +1  texture lives under textures/entity/ (where entity textures live
   *       in every stock Bedrock resource pack)
   */
  static scoreMatch(modelIdentifierOrPath, textureName) {
    const base = (p) =>
      String(p || '')
        .replace(/\\/g, '/')
        .split('/')
        .pop()
        .replace(/\.(geo\.json|json|png)$/i, '')
        .toLowerCase();
    const a = base(modelIdentifierOrPath);
    const b = base(textureName);
    let score = 0;
    if (a === b) score += 10;
    if (a && b && (a.includes(b) || b.includes(a))) score += 6;
    const tokensA = a.split(/[_.\-\s]+/).filter(Boolean);
    const tokensB = b.split(/[_.\-\s]+/).filter(Boolean);
    for (const t of tokensA) if (tokensB.includes(t)) score += 2;
    if (/textures\/entity\//i.test(textureName)) score += 1;
    return score;
  }

  /**
   * Returns candidate textures for a model, best match first.
   * @param {string} modelIdentifierOrPath
   * @returns {{name:string, score:number}[]}
   */
  findCandidates(modelIdentifierOrPath) {
    return this.list()
      .map((r) => ({ name: r.name, score: TextureManager.scoreMatch(modelIdentifierOrPath, r.name) }))
      .sort((a, b) => b.score - a.score);
  }
}
