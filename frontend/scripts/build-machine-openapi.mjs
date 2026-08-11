import { buildReadOnlyOpenApiContract } from './build-readonly-openapi-contract.mjs'

await buildReadOnlyOpenApiContract({
  resourceName: 'Machine',
  title: 'Paper MES Machine Read API',
  outputFile: 'machine-readonly.json',
  paths: ['/api/machines', '/api/machines/{uuid}'],
})
