import { spawn } from 'node:child_process';

const run = (command, args) =>
  new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      stdio: 'inherit',
    });
    child.on('close', (code) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`${command} ${args.join(' ')} exited with code ${code}`));
    });
  });

const runCommandText = async (commandText) => {
  if (process.platform === 'win32') {
    await run('cmd.exe', ['/d', '/s', '/c', commandText]);
    return;
  }
  await run('sh', ['-lc', commandText]);
};

try {
  await runCommandText('npx vue-tsc --noEmit');
  await runCommandText('npx vite build');
} catch (error) {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
}
