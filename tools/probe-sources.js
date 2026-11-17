// Auditoría de fuentes desde la línea de comandos (usa el mismo motor que la app).
// Uso: node tools/probe-sources.js
const fs = require("fs");
const path = require("path");
const engine = require("../source-health.js");

const mem = {}; // almacenamiento en memoria
const storage = { getItem: k => (k in mem ? mem[k] : null), setItem: (k, v) => { mem[k] = v; } };
const channels = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "channels.json"), "utf8"));

engine.init({ getChannels: () => channels, storage, proxyUrl: "", isBusy: () => false });
engine.scan().then(report => {
  console.table(report);
  console.log("\nOrden resultante:");
  channels.forEach(ch => console.log(`${String(ch.number).padStart(2)} ${ch.name}${ch.pinSources ? " (fijado)" : ""}:`,
    ch.sources.map(s => s.label || s.ytVideoId).join("  >  ")));
  console.log("\nFallos registrados:", engine.getFailLog().length);
});
