const test = require("node:test");
const assert = require("node:assert");
const fs = require("fs");
const path = require("path");

const root = path.join(__dirname, "..");

test("canales_extra.template.json: estructura y esquema válidos para Secret Gist", () => {
  const file = path.join(root, "canales_extra.template.json");
  assert.strictEqual(fs.existsSync(file), true, "El archivo template debe existir");
  const data = JSON.parse(fs.readFileSync(file, "utf8"));
  assert.strictEqual(typeof data.version, "number");
  assert.strictEqual(Array.isArray(data.iptv_org_whitelist), true);
  assert.strictEqual(Array.isArray(data.canales), true);
  assert.strictEqual(data.canales.length >= 18, true);

  data.canales.forEach(ch => {
    assert.ok(ch.id, "Cada canal debe tener id");
    assert.ok(ch.nombre, "Cada canal debe tener nombre");
    assert.ok(ch.categoria, "Cada canal debe tener categoria");
    assert.ok(Array.isArray(ch.fuentes) && ch.fuentes.length > 0, "Cada canal debe tener fuentes");
    ch.fuentes.forEach(f => {
      assert.ok(f.etiqueta, "Fuente debe tener etiqueta");
      assert.ok(f.tipo, "Fuente debe tener tipo");
      assert.ok(f.url, "Fuente debe tener url");
    });
  });
});

test("web/manifest.json: PWA manifest válido", () => {
  const manifestFile = path.join(root, "web", "manifest.json");
  assert.strictEqual(fs.existsSync(manifestFile), true);
  const manifest = JSON.parse(fs.readFileSync(manifestFile, "utf8"));
  assert.strictEqual(manifest.name, "AndyTV Plus");
  assert.strictEqual(manifest.display, "fullscreen");
});
