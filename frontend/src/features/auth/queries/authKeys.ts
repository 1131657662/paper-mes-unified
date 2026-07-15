import { createQueryKeys } from '@lukemorales/query-key-factory'
import { getCurrentUser } from '../../../api/auth'

export const authKeys = createQueryKeys('auth', {
  currentUser: {
    queryKey: null,
    queryFn: getCurrentUser,
  },
})
