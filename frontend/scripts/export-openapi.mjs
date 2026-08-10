import { existsSync } from 'node:fs'
import { spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repositoryDir = resolve(frontendDir, '..')
const wrapper = join(
  repositoryDir,
  process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw',
)
const output = join(repositoryDir, 'target', 'openapi', 'paper-mes.json')
const mavenArgs = ['-q', '-Dtest=OpenApiEnabledContractTest', 'test']
const isWindows = process.platform === 'win32'
const command = isWindows ? (process.env.ComSpec ?? 'cmd.exe') : 'sh'
const args = isWindows
  ? ['/d', '/s', '/c', wrapper, ...mavenArgs]
  : [wrapper, ...mavenArgs]

const result = spawnSync(command, args, {
  cwd: repositoryDir,
  stdio: 'inherit',
})

if (result.status !== 0) {
  process.exit(result.status ?? 1)
}
if (!existsSync(output)) {
  throw new Error(`Springdoc schema was not generated: ${output}`)
}
