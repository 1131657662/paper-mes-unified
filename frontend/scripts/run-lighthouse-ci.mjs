import { spawn, spawnSync } from 'node:child_process'
import { once } from 'node:events'
import { mkdirSync, mkdtempSync, rmSync } from 'node:fs'
import { createServer } from 'node:net'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { launch } from 'chrome-launcher'

const frontendDir = fileURLToPath(new URL('..', import.meta.url))
const lighthouseCli = fileURLToPath(new URL('../node_modules/@lhci/cli/src/cli.js', import.meta.url))
const viteCli = fileURLToPath(new URL('../node_modules/vite/bin/vite.js', import.meta.url))
const workspaceTempRoot = path.join(frontendDir, 'artifacts', 'lighthouse-temp')

const exitCode = await runAudit()
process.exitCode = exitCode

async function runAudit() {
  const runTemp = createRunTemp()
  const previewPort = await getAvailablePort()
  const url = `http://127.0.0.1:${previewPort}/login`
  const preview = startPreview(previewPort)
  let chrome

  try {
    await waitForServer(preview, url)
    chrome = await startChrome(runTemp)
    return await runLighthouse({ chromePort: chrome.port, runTemp, url })
  } finally {
    if (chrome) await chrome.kill()
    await stopChild(preview)
    await delay(500)
    cleanupRunTemp(runTemp)
  }
}

function createRunTemp() {
  mkdirSync(workspaceTempRoot, { recursive: true })
  return mkdtempSync(path.join(workspaceTempRoot, 'run-'))
}

function startPreview(port) {
  return spawn(process.execPath, [viteCli, 'preview', '--host', '127.0.0.1', '--port', String(port), '--strictPort'], {
    cwd: frontendDir,
    stdio: 'inherit',
  })
}

function startChrome(runTemp) {
  const userDataDir = path.join(runTemp, 'chrome-profile')
  mkdirSync(userDataDir)
  return launch({
    chromeFlags: ['--headless=new', '--no-sandbox', '--disable-gpu'],
    userDataDir,
  })
}

function runLighthouse({ chromePort, runTemp, url }) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [lighthouseCli, 'autorun'], {
      cwd: frontendDir,
      env: {
        ...process.env,
        PAPER_MES_LHCI_CHROME_PORT: String(chromePort),
        PAPER_MES_LHCI_URL: url,
        TEMP: runTemp,
        TMP: runTemp,
      },
      stdio: 'inherit',
    })
    child.once('error', reject)
    child.once('exit', (code) => resolve(code ?? 1))
  })
}

async function waitForServer(preview, url) {
  const deadline = Date.now() + 30_000
  while (Date.now() < deadline) {
    if (preview.exitCode !== null) throw new Error(`Vite preview exited with ${preview.exitCode}`)
    if (await isReachable(url)) return
    await delay(250)
  }
  throw new Error(`Timed out waiting for ${url}`)
}

async function isReachable(url) {
  try {
    return (await fetch(url)).ok
  } catch {
    return false
  }
}

function getAvailablePort() {
  return new Promise((resolve, reject) => {
    const server = createServer()
    server.once('error', reject)
    server.listen(0, '127.0.0.1', () => closePortProbe(server, resolve, reject))
  })
}

function closePortProbe(server, resolve, reject) {
  const address = server.address()
  if (!address || typeof address === 'string') return reject(new Error('Could not allocate a port'))
  server.close((error) => error ? reject(error) : resolve(address.port))
}

async function stopChild(child) {
  if (child.exitCode !== null) return
  const exited = once(child, 'exit')
  child.kill()
  await Promise.race([exited, delay(5_000)])
  if (child.exitCode === null && child.pid) forceStopProcess(child.pid)
}

function forceStopProcess(pid) {
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/pid', String(pid), '/T', '/F'], { stdio: 'ignore' })
    return
  }
  process.kill(pid, 'SIGKILL')
}

function cleanupRunTemp(runTemp) {
  rmSync(runTemp, { force: true, maxRetries: 5, recursive: true, retryDelay: 500 })
}

function delay(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}
