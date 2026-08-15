import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProjectMemoryVersions() {
  return useQuery(queries.projectMemory.versions)
}
