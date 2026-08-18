import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { PendingPackagingInput } from '../services/processAiService'

const disabledInput: PendingPackagingInput = { orderUuid: '', expectedVersion: 0 }

export function usePendingProcessAiPackaging(input?: PendingPackagingInput) {
  return useQuery({
    ...queries.processAi.pendingPackaging(input ?? disabledInput),
    enabled: Boolean(input?.orderUuid),
  })
}
