import { buildReadOnlyOpenApiContract } from './build-readonly-openapi-contract.mjs'

await buildReadOnlyOpenApiContract({
  resourceName: 'Customer',
  title: 'Paper MES Customer Read API',
  outputFile: 'customer-readonly.json',
  paths: ['/api/customers', '/api/customers/{uuid}'],
})
