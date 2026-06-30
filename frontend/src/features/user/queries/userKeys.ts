import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { UserQuery } from '../../../types/user'
import { userService } from '../services/userService'

export const userKeys = createQueryKeys('user', {
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => userService.detail(uuid),
  }),
  list: (query: UserQuery) => ({
    queryKey: [query],
    queryFn: () => userService.list(query),
  }),
})
