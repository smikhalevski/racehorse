import esbuild from 'esbuild';
import path from 'path';
import fs from 'fs';
import { execSync } from 'child_process';

const outputPath = path.resolve('dist');
const archivePath = path.join(outputPath, 'bundle.zip');

fs.mkdirSync(outputPath, { recursive: true });

fs.writeFileSync(
  path.join(outputPath, 'index.html'),
  `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
  <script src="/index.js"></script>
</body>
</html>`
);

const zipPlugin = {
  name: 'zip',

  setup(build) {
    build.onEnd(() => {
      fs.rmSync(archivePath, { force: true });

      execSync(`zip -r ${archivePath} .`, { cwd: outputPath });
    });
  },
};

const ctx = await esbuild.context({
  entryPoints: ['./src/index.tsx'],
  bundle: true,
  outdir: outputPath,
  plugins: [zipPlugin],
});

if (!process.argv.includes('--watch')) {
  await ctx.rebuild();
  process.exit();
}

await ctx.watch();

await ctx.serve({
  port: 10001,
  servedir: outputPath,
});

console.log(`Serving on http://localhost:10001`);
