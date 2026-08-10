import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { gzipSync } from 'node:zlib'
import { join } from 'node:path'

const distDir = join(process.cwd(), 'dist')
const assetsDir = join(distDir, 'assets')
const initialGzipBudget = 260 * 1024
const asyncChunkGzipBudget = 190 * 1024

if (!existsSync(join(distDir, 'index.html')) || !existsSync(assetsDir)) {
  console.error('Bundle budget check requires a completed dist build.')
  process.exit(1)
}

const html = readFileSync(join(distDir, 'index.html'), 'utf8')
const initialAssets = new Set([...html.matchAll(/(?:src|href)="\/assets\/([^"]+)"/g)].map((match) => match[1]))
const assetFiles = readdirSync(assetsDir).filter((file) => file.endsWith('.js') || file.endsWith('.css'))
const initialFiles = assetFiles.filter((file) => initialAssets.has(file))
const asyncFiles = assetFiles.filter((file) => !initialAssets.has(file) && file.endsWith('.js'))
const initialGzipBytes = initialFiles.reduce((total, file) => total + gzipBytes(file), 0)
const largestAsync = asyncFiles
  .map((file) => ({ file, gzipBytes: gzipBytes(file) }))
  .sort((left, right) => right.gzipBytes - left.gzipBytes)[0]

console.log(`Initial assets: ${initialFiles.length}, gzip: ${formatBytes(initialGzipBytes)}`)
console.log(`Largest async JS: ${largestAsync?.file ?? 'none'}, gzip: ${formatBytes(largestAsync?.gzipBytes ?? 0)}`)

const failures = []
if (initialGzipBytes > initialGzipBudget) {
  failures.push(`initial gzip ${formatBytes(initialGzipBytes)} > ${formatBytes(initialGzipBudget)}`)
}
if (largestAsync && largestAsync.gzipBytes > asyncChunkGzipBudget) {
  failures.push(`largest async gzip ${formatBytes(largestAsync.gzipBytes)} > ${formatBytes(asyncChunkGzipBudget)}`)
}
if (failures.length) {
  console.error(`Bundle budget exceeded: ${failures.join('; ')}`)
  process.exit(1)
}

function gzipBytes(file) {
  return gzipSync(readFileSync(join(assetsDir, file)), { level: 9 }).byteLength
}

function formatBytes(bytes) {
  return `${(bytes / 1024).toFixed(1)} KiB`
}
