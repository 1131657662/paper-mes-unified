import { createQueryKeys } from '@lukemorales/query-key-factory'
import {
  processAiService,
  type MessageInput,
  type PendingPackagingInput,
} from '../services/processAiService'

export const processAiKeys = createQueryKeys('processAi', {
  status: {
    queryKey: null,
    queryFn: processAiService.status,
  },
  providerSettings: {
    queryKey: null,
    queryFn: processAiService.providerSettings,
  },
  messages: (input: MessageInput) => ({
    queryKey: [input],
    queryFn: () => processAiService.messages(input),
  }),
  pendingPackaging: (input: PendingPackagingInput) => ({
    queryKey: [input],
    queryFn: () => processAiService.pendingPackaging(input),
  }),
})
