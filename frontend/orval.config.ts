import { defineConfig } from 'orval'

export default defineConfig({
  customerReadOnly: {
    input: './openapi/customer-readonly.json',
    output: {
      target: './src/api/generated/customerReadOnly.ts',
      client: 'axios-functions',
      clean: false,
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
  paperReadOnly: {
    input: './openapi/paper-readonly.json',
    output: {
      target: './src/api/generated/paperReadOnly.ts',
      client: 'axios-functions',
      clean: false,
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
