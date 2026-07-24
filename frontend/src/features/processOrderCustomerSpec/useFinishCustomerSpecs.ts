import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  previewFinishCustomerSpecRevision,
  publishFinishCustomerSpecRevision,
} from '../../api/customerSpecification'
import { queries } from '../../queries'
import { invalidateProcessOrderReadModels } from '../processOrderDetail/hooks/invalidateProcessOrderReadModels'
import type { FinishCustomerRevisionRequest } from './customerSpecTypes'

export function useFinishCustomerSpecs(orderUuid?: string) {
  return useQuery({
    ...queries.finishCustomerSpec.current(orderUuid ?? ''),
    enabled: Boolean(orderUuid),
  })
}

export function useFinishCustomerSpecRevisions(orderUuid?: string, enabled = false) {
  return useQuery({
    ...queries.finishCustomerSpec.revisions(orderUuid ?? ''),
    enabled: Boolean(orderUuid) && enabled,
  })
}

export function useFinishCustomerSpecRevisionDetail(
  orderUuid?: string, revisionUuid?: string,
) {
  return useQuery({
    ...queries.finishCustomerSpec.revisionDetail(orderUuid ?? '', revisionUuid ?? ''),
    enabled: Boolean(orderUuid && revisionUuid),
  })
}

export function usePreviewFinishCustomerSpecs() {
  return useMutation({
    mutationFn: ({ orderUuid, values }: MutationParams) => (
      previewFinishCustomerSpecRevision(orderUuid, values)
    ),
  })
}

export function usePublishFinishCustomerSpecs() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ orderUuid, values }: MutationParams) => (
      publishFinishCustomerSpecRevision(orderUuid, values)
    ),
    onSuccess: async (_, { orderUuid }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queries.finishCustomerSpec.revisions(orderUuid).queryKey }),
        invalidateProcessOrderReadModels(queryClient, orderUuid),
      ])
    },
  })
}

interface MutationParams {
  orderUuid: string
  values: FinishCustomerRevisionRequest
}
