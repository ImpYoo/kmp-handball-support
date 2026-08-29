import { serve } from "bun";

serve({
  port: 8080,
  async fetch(req) {
    const url = new URL(req.url);
    const path = url.pathname === "/" ? "./index.html" : `.${url.pathname}`;
    const file = Bun.file(path);
    if (await file.exists()) return new Response(file);
    return new Response(Bun.file("./index.html"));
  },
});
