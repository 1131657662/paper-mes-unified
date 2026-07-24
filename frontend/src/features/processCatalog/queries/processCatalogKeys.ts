import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getActiveProcessCatalog } from '../../../api/processCatalog'

export const processCatalogKeys = createQueryKeys('processCatalog', {
  active: {
    queryKey: null,
    queryFn: getActiveProcessCatalog,
  },
})
