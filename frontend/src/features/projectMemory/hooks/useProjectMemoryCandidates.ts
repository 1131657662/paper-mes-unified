import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ProjectMemoryCandidateStatus } from '../types'

export function useProjectMemoryCandidates(status?: ProjectMemoryCandidateStatus) {
  return useQuery(queries.projectMemory.candidates(status))
}
