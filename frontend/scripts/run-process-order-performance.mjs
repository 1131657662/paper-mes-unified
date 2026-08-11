import { spawn, spawnSync } from 'node:child_process'
import { once } from 'node:events'
import { createServer } from 'node:net'
import { fileURLToPath } from 'node:url'

const frontendDir = fileURLToPath(new URL('..', import.meta.url))
const typescriptCli = fileURLToPath(new URL('../node_modules/typescript/bin/tsc', import.meta.url))
const viteCli = fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url))
const playwrightCli = fileURLToPath(new URL('../node_modules/@playwright/test/cli.js', import.meta.url))
const configFile = fileURLToPath(new URL('../playwright.performance.config.ts', import.meta.url))

const exitCode = await runPerformanceTest()
process.exitCode = exitCode

async function runPerformanceTest() {
  const port = await getAvailablePort()
  const env = { ...process.env, VITE_PERF_PROFILER_ENABLED: 'true', PAPER_MES_PERF_BASE_URL: `http://127.0.0.1:${port}` }
  const typecheck = runNodeCli(typescriptCli, ['-b'], env)
  if (typecheck !== 0) return typecheck
  const build = runNodeCli(viteCli, ['build'], env)
  if (build !== 0) return build

  const preview = spawn(process.execPath, [viteCli, 'preview', '--host', '127.0.0.1', '--port', String(port), '--strictPort'], { cwd: frontendDir, env, stdio: 'inherit' })
  try {
    await waitForServer(preview, `http://127.0.0.1:${port}/process-orders`)
    return await runPlaywright(env)
  } finally {
    await stopChild(preview)
  }
}

function runNodeCli(cli, args, env) {
  const result = spawnSync(process.execPath, [cli, ...args], { cwd: frontendDir, env, stdio: 'inherit' })
  if (result.error) {
    console.error(`Could not start ${cli}:`, result.error)
    return 1
  }
  return result.status ?? 1
}

function runPlaywright(env) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [playwrightCli, 'test', '--config', configFile], { cwd: frontendDir, env, stdio: 'inherit' })
    child.once('error', reject)
    child.once('exit', (code) => resolve(code ?? 1))
  })
}

async function waitForServer(preview, url) {
  const deadline = Date.now() + 30_000
  while (Date.now() < deadline) {
    if (preview.exitCode !== null) throw new Error(`Vite preview exited with ${preview.exitCode}`)
    try {
      if ((await fetch(url)).ok) return
    } catch {
      // Preview may need another startup interval.
    }
    await delay(250)
  }
  throw new Error(`Timed out waiting for ${url}`)
}

async function stopChild(child) {
  if (child.exitCode !== null || child.signalCode !== null) return
  const exited = once(child, 'exit')
  child.kill()
  await Promise.race([exited, delay(5_000)])
  if (child.exitCode !== null || child.signalCode !== null || !child.pid) return
  try {
    process.kill(child.pid, 'SIGKILL')
  } catch (error) {
    if (error?.code !== 'ESRCH') throw error
  }
}

function getAvailablePort() {
  return new Promise((resolve, reject) => {
    const server = createServer()
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => {
      const address = server.address()
      if (!address || typeof address === 'string') return reject(new Error('Could not allocate a port'))
      server.close((error) => error ? reject(error) : resolve(address.port))
    })
  })
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}
