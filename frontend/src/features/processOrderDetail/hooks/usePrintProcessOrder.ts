import { useMutation, useQueryClient } from '@tanstack/react-query'
import { issueProcessOrder, physicalReprintProcessOrder, printProcessOrder } from '../../../api/processOrder'
import type { PhysicalReprintDTO, PrintDTO } from '../../../types/processOrder'
import { invalidateProcessOrderLocalReadModels } from './invalidateProcessOrderReadModels'

export function usePrintProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dto?: PrintDTO) => printProcessOrder(orderUuid!, dto),
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderLocalReadModels(queryClient, orderUuid)
    },
  })
}

export function useIssueProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => issueProcessOrder(orderUuid!),
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderLocalReadModels(queryClient, orderUuid)
    },
  })
}

export function usePhysicalReprintProcessOrder(orderUuid?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dto: PhysicalReprintDTO) => physicalReprintProcessOrder(orderUuid!, dto),
    onSuccess: async () => {
      if (!orderUuid) return
      await invalidateProcessOrderLocalReadModels(queryClient, orderUuid)
    },
  })
}
