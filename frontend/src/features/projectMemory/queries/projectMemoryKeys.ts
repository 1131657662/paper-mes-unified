import { createQueryKeys } from '@lukemorales/query-key-factory'
import { projectMemoryService } from '../services/projectMemoryService'
import type { ProjectMemoryCandidateStatus } from '../types'

export const projectMemoryKeys = createQueryKeys('projectMemory', {
  current: {
    queryKey: null,
    queryFn: projectMemoryService.current,
  },
  versions: {
    queryKey: null,
    queryFn: projectMemoryService.versions,
  },
  candidates: (status?: ProjectMemoryCandidateStatus) => ({
    queryKey: [{ status: status ?? 'ALL' }],
    queryFn: () => projectMemoryService.candidates(status),
  }),
  candidate: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => projectMemoryService.candidate(uuid),
  }),
})
