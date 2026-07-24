import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  previewDeliveryCustomerSpecRevision,
  publishDeliveryCustomerSpecRevision,
} from '../../api/deliveryCustomerSpecification'
import { queries } from '../../queries'
import type { DeliveryCustomerRevisionRequest } from './deliveryCustomerSpecTypes'

export function useDeliveryCustomerSpecs(uuid?: string) {
  return useQuery({ ...queries.deliveryCustomerSpec.current(uuid ?? ''), enabled: Boolean(uuid) })
}

export function useDeliveryCustomerSpecRevisions(uuid?: string, enabled = false) {
  return useQuery({
    ...queries.deliveryCustomerSpec.revisions(uuid ?? ''),
    enabled: Boolean(uuid) && enabled,
  })
}

export function useDeliveryCustomerSpecRevisionDetail(uuid?: string, revisionUuid?: string) {
  return useQuery({
    ...queries.deliveryCustomerSpec.revisionDetail(uuid ?? '', revisionUuid ?? ''),
    enabled: Boolean(uuid && revisionUuid),
  })
}

export function usePreviewDeliveryCustomerSpecs() {
  return useMutation({
    mutationFn: ({ uuid, values }: MutationParams) => previewDeliveryCustomerSpecRevision(uuid, values),
  })
}

export function usePublishDeliveryCustomerSpecs() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ uuid, values }: MutationParams) => publishDeliveryCustomerSpecRevision(uuid, values),
    onSuccess: async (_, { uuid }) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queries.deliveryCustomerSpec.current(uuid).queryKey }),
        queryClient.invalidateQueries({ queryKey: queries.deliveryCustomerSpec.revisions(uuid).queryKey }),
        queryClient.invalidateQueries({ queryKey: queries.delivery.detail(uuid).queryKey }),
      ])
    },
  })
}

interface MutationParams { uuid: string; values: DeliveryCustomerRevisionRequest }
