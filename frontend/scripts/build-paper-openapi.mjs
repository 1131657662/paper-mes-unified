import { buildReadOnlyOpenApiContract } from './build-readonly-openapi-contract.mjs'

await buildReadOnlyOpenApiContract({
  resourceName: 'Paper',
  title: 'Paper MES Paper Read API',
  outputFile: 'paper-readonly.json',
  paths: ['/api/papers', '/api/papers/{uuid}'],
})
