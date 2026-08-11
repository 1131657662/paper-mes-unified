import { buildReadOnlyOpenApiContract } from './build-readonly-openapi-contract.mjs'

await buildReadOnlyOpenApiContract({
  resourceName: 'Warehouse',
  title: 'Paper MES Warehouse Read API',
  outputFile: 'warehouse-readonly.json',
  paths: ['/api/warehouses', '/api/warehouses/{uuid}'],
})
