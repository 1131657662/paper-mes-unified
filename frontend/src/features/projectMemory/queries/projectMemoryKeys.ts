import { createQueryKeys } from '@lukemorales/query-key-factory'
import { projectMemoryService } from '../services/projectMemoryService'

export const projectMemoryKeys = createQueryKeys('projectMemory', {
  current: {
    queryKey: null,
    queryFn: projectMemoryService.current,
  },
  versions: {
    queryKey: null,
    queryFn: projectMemoryService.versions,
  },
})
