import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function useProcessCatalog() {
  return useQuery({
    ...queries.processCatalog.active,
    staleTime: 5 * 60 * 1000,
  })
}
