import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { MessageInput } from '../services/processAiService'

const disabledInput: MessageInput = { orderUuid: '', conversationId: '', expectedVersion: 0 }

export function useProcessAiMessages(input?: MessageInput) {
  return useQuery({
    ...queries.processAi.messages(input ?? disabledInput),
    enabled: Boolean(input?.orderUuid && input.conversationId),
  })
}
