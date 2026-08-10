import { defineConfig } from 'orval'

export default defineConfig({
  customerReadOnly: {
    input: './openapi/customer-readonly.json',
    output: {
      target: './src/api/generated/customerReadOnly.ts',
      client: 'axios-functions',
      clean: true,
      formatter: 'prettier',
      override: {
        header: false,
        mutator: {
          path: './src/api/orvalRequest.ts',
          name: 'orvalRequest',
        },
      },
    },
  },
})
