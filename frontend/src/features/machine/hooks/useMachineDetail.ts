import { useQuery } from '@tanstack/react-query'
import { machineKeys } from '../queries/machineKeys'

export function useMachineDetail(uuid?: string) {
  return useQuery({
    ...machineKeys.detail(uuid ?? ''),
    enabled: Boolean(uuid),
  })
}
