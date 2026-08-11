import { mkdir, readFile, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const sourcePath = resolve(
  frontendDir,
  '..',
  'target',
  'openapi',
  'paper-mes.json',
)
const outputPath = resolve(frontendDir, 'openapi', 'paper-readonly.json')
const paperPaths = ['/api/papers', '/api/papers/{uuid}']

const source = JSON.parse(await readFile(sourcePath, 'utf8'))
const paths = selectReadPaths(source)
unwrapSuccessResponses(source, paths)
const schemas = selectReferencedSchemas(source, paths)
const contract = sortObject({
  openapi: source.openapi,
  info: {
    title: 'Paper MES Paper Read API',
    version: source.info.version,
    description:
      'Generated read-only client contract. Responses are unwrapped by request.ts.',
  },
  paths,
  components: { schemas },
})

await mkdir(dirname(outputPath), { recursive: true })
await writeFile(outputPath, `${JSON.stringify(contract, null, 2)}\n`)

function selectReadPaths(document) {
  return Object.fromEntries(
    paperPaths.map((path) => {
      const operation = document.paths?.[path]?.get
      if (!operation) throw new Error(`Missing Paper GET operation: ${path}`)
      return [path, { get: structuredClone(operation) }]
    }),
  )
}

function unwrapSuccessResponses(document, paths) {
  for (const pathItem of Object.values(paths)) {
    const content = pathItem.get.responses?.['200']?.content
    for (const media of Object.values(content ?? {})) {
      media.schema = responseDataSchema(document, media.schema)
    }
  }
}

function responseDataSchema(document, responseSchema) {
  const wrapper = referencedSchema(document, responseSchema?.$ref)
  const data = wrapper?.properties?.data
  if (!data)
    throw new Error('Expected the Paper response to use the R<T> envelope')
  return structuredClone(data)
}

function referencedSchema(document, reference) {
  const name = componentName(reference)
  return name ? document.components?.schemas?.[name] : undefined
}

function selectReferencedSchemas(document, paths) {
  const selected = {}
  const pending = [...collectReferences(paths)]
  while (pending.length > 0) {
    const name = pending.shift()
    if (!name || selected[name]) continue
    const schema = document.components?.schemas?.[name]
    if (!schema) throw new Error(`Missing referenced schema: ${name}`)
    selected[name] = structuredClone(schema)
    pending.push(...collectReferences(schema))
  }
  return selected
}

function collectReferences(value, names = new Set()) {
  if (Array.isArray(value))
    value.forEach((item) => collectReferences(item, names))
  else if (value && typeof value === 'object') {
    const name = componentName(value.$ref)
    if (name) names.add(name)
    Object.values(value).forEach((item) => collectReferences(item, names))
  }
  return names
}

function componentName(reference) {
  const prefix = '#/components/schemas/'
  return typeof reference === 'string' && reference.startsWith(prefix)
    ? reference.slice(prefix.length)
    : undefined
}

function sortObject(value) {
  if (Array.isArray(value)) return value.map(sortObject)
  if (!value || typeof value !== 'object') return value
  return Object.fromEntries(
    Object.keys(value)
      .sort()
      .map((key) => [key, sortObject(value[key])]),
  )
}
