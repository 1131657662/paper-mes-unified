import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProjectMemory() {
  return useQuery(queries.projectMemory.current)
}
