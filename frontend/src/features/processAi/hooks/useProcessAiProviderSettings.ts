import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProcessAiProviderSettings() {
  return useQuery(queries.processAi.providerSettings)
}
