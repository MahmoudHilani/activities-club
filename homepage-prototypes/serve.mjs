import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

const PROTOTYPES = [
  { port: 5180, file: "01-editorial.html", name: "Editorial / Magazine" },
  { port: 5181, file: "02-brutalist.html", name: "Brutalist / Raw outdoor" },
  { port: 5182, file: "03-playful.html", name: "Playful / Hand-drawn" },
  { port: 5183, file: "04-cinematic.html", name: "Cinematic / Dark" },
];

const INDEX_HTML = `<!doctype html>
<html><head><meta charset="utf-8"><title>Homepage prototypes</title>
<style>
  body{font-family:ui-sans-serif,system-ui,sans-serif;background:#0b1020;color:#f4f1ea;margin:0;padding:48px;max-width:880px;margin:0 auto}
  h1{font-size:32px;letter-spacing:-0.01em;margin:0 0 8px}
  p{color:#a8b0c4;margin:0 0 32px}
  ul{list-style:none;padding:0;margin:0;display:grid;gap:14px}
  a.card{display:block;padding:22px 26px;background:#1a2440;border:1px solid rgba(255,255,255,.08);border-radius:14px;text-decoration:none;color:inherit;transition:transform .15s,border-color .15s}
  a.card:hover{transform:translateY(-2px);border-color:#e8b768}
  .name{font-size:20px;font-weight:600;margin-bottom:4px}
  .port{font-family:ui-monospace,monospace;font-size:13px;color:#e8b768}
</style></head><body>
<h1>Griffith Activities · Homepage prototypes</h1>
<p>Four design directions running concurrently. Pick one and the winner gets ported into the Vue app.</p>
<ul>
${PROTOTYPES.map((p) => `<li><a class="card" href="http://localhost:${p.port}/"><div class="name">${p.name}</div><div class="port">localhost:${p.port}</div></a></li>`).join("")}
</ul></body></html>`;

function startServer({ port, file, name }) {
  const server = createServer(async (req, res) => {
    try {
      const url = new URL(req.url, `http://localhost:${port}`);
      let path = url.pathname;
      // Always serve the prototype HTML at root
      if (path === "/" || path === "") {
        const html = await readFile(join(__dirname, file), "utf8");
        res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
        res.end(html);
        return;
      }
      // Allow loading sibling files (e.g. shared images later)
      const rel = path.replace(/^\/+/, "");
      const fullPath = join(__dirname, rel);
      if (!fullPath.startsWith(__dirname) || !existsSync(fullPath)) {
        res.writeHead(404);
        res.end("Not found");
        return;
      }
      const body = await readFile(fullPath);
      const ext = rel.split(".").pop().toLowerCase();
      const types = {
        html: "text/html; charset=utf-8",
        css: "text/css; charset=utf-8",
        js: "application/javascript; charset=utf-8",
        svg: "image/svg+xml",
        png: "image/png",
        jpg: "image/jpeg",
        webp: "image/webp",
      };
      res.writeHead(200, { "content-type": types[ext] || "application/octet-stream" });
      res.end(body);
    } catch (err) {
      res.writeHead(500);
      res.end(`Error: ${err.message}`);
    }
  });
  server.listen(port, () => {
    console.log(`  ${String(port).padEnd(6)} → ${name.padEnd(28)}  http://localhost:${port}/`);
  });
}

// Index server on 5179 listing all four
const indexServer = createServer((req, res) => {
  res.writeHead(200, { "content-type": "text/html; charset=utf-8" });
  res.end(INDEX_HTML);
});
indexServer.listen(5179, () => {
  console.log("");
  console.log("──────────────────────────────────────────────────────────────────");
  console.log("Griffith Activities · Homepage prototypes");
  console.log("──────────────────────────────────────────────────────────────────");
  console.log(`  5179   → Index (all prototypes)            http://localhost:5179/`);
});

for (const proto of PROTOTYPES) startServer(proto);

console.log("──────────────────────────────────────────────────────────────────");
console.log("Open any URL above. Ctrl+C to stop.\n");
