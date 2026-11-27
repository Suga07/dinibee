import vinext from "vinext";
import { defineConfig } from "vite";

const isCodexSeatbeltSandbox = process.env.CODEX_SANDBOX === "seatbelt";

const localBindingConfig = {
  main: "./worker/index.ts",
  compatibility_flags: ["nodejs_compat"],
  d1_databases: [],
  r2_buckets: [],
};

export default defineConfig(async () => {
  process.env.WRANGLER_WRITE_LOGS ??= "false";
  process.env.WRANGLER_LOG_PATH ??= ".wrangler/logs";
  process.env.MINIFLARE_REGISTRY_PATH ??= ".wrangler/registry";

  const { cloudflare } = await import("@cloudflare/vite-plugin");

  return {
    resolve: {
      dedupe: ["react", "react-dom"],
    },
    server: {
      host: "0.0.0.0",
      allowedHosts: [
        "localhost",
        "127.0.0.1",
        "terminal.local",
      ],
      ...(isCodexSeatbeltSandbox
        ? {
            watch: {
              useFsEvents: false,
              usePolling: true,
            },
          }
        : {}),
    },

    plugins: [
      vinext(),
      cloudflare({
        viteEnvironment: {
          name: "rsc",
          childEnvironments: ["ssr"],
        },
        inspectorPort: false,
        config: localBindingConfig,
      }),
    ],
  };
});