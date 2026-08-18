import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProjectMemoryCandidate(uuid?: string) {
  return useQuery({
    ...queries.projectMemory.candidate(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
