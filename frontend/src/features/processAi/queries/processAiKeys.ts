import { createQueryKeys } from '@lukemorales/query-key-factory'
import {
  processAiService,
  type MessageInput,
  type PendingPackagingInput,
} from '../services/processAiService'
import type { ProcessAiManagedProvider } from '../types'

export const processAiKeys = createQueryKeys('processAi', {
  status: {
    queryKey: null,
    queryFn: processAiService.status,
  },
  providerSettings: (provider: ProcessAiManagedProvider) => ({
    queryKey: [provider],
    queryFn: () => processAiService.providerSettings(provider),
  }),
  messages: (input: MessageInput) => ({
    queryKey: [input],
    queryFn: () => processAiService.messages(input),
  }),
  pendingPackaging: (input: PendingPackagingInput) => ({
    queryKey: [input],
    queryFn: () => processAiService.pendingPackaging(input),
  }),
})
