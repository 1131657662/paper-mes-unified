import { useQuery } from '@tanstack/react-query'
import { customerKeys } from '../queries/customerKeys'

export function useCustomerDetail(uuid?: string) {
  return useQuery({
    ...customerKeys.detail(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
