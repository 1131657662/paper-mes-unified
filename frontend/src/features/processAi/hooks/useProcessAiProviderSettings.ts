import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ProcessAiManagedProvider } from '../types'

export function useProcessAiProviderSettings(provider: ProcessAiManagedProvider) {
  return useQuery(queries.processAi.providerSettings(provider))
}
