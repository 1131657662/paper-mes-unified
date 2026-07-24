import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type {
  DraftOrderVO,
  ProcessConfigDraftVO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../../types/processOrder'
import { createOrderService } from '../services/createOrderService'

export function useSaveRouteBatch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createOrderService.saveRouteBatch,
    onSuccess: (previews, variables) => {
      queryClient.setQueryData(
        queries.createOrder.draft(variables.orderUuid).queryKey,
        (draft: DraftOrderVO | undefined) => updateDraft(
          draft,
          variables.dto.expectedVersion,
          variables.dto.routes,
          previews,
        ),
      )
      queryClient.invalidateQueries({ queryKey: queries.createOrder.draft(variables.orderUuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey })
    },
  })
}

function updateDraft(
  draft: DraftOrderVO | undefined,
  expectedVersion: number,
  routes: ProcessRoutePreviewDTO[],
  previews: ProcessRoutePreviewVO[],
): DraftOrderVO | undefined {
  if (!draft) return draft
  const configs = routes.reduce(
    (current, route, index) => upsertRoute(current, route, previews[index]),
    draft.configs ?? [],
  )
  const order = draft.order ? { ...draft.order, version: expectedVersion + 1 } : draft.order
  return { ...draft, configs, order }
}

function upsertRoute(
  configs: ProcessConfigDraftVO[],
  route: ProcessRoutePreviewDTO,
  preview?: ProcessRoutePreviewVO,
): ProcessConfigDraftVO[] {
  if (!preview) return configs
  const next: ProcessConfigDraftVO = {
    originalUuid: route.originalUuid,
    processMode: 1,
    mainStepType: route.stages[0]?.stepType,
    configStatus: 1,
    configType: 'routePlan',
    route,
    routePreview: preview,
  }
  if (!configs.some((config) => config.originalUuid === route.originalUuid)) return [...configs, next]
  return configs.map((config) => (config.originalUuid === route.originalUuid ? { ...config, ...next } : config))
}
