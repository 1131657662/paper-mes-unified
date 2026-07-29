import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type {
  DraftOrderVO,
  ProcessConfigDraftVO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../../types/processOrder'
import { createOrderService } from '../services/createOrderService'
import {
  batchRoutesMatch,
  readLatestDraft,
  recoverRoutePreviews,
  runReconciledDraftWrite,
} from '../draftWriteReconciliation'

export function useSaveRouteBatch() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.saveRouteBatch>[0]) => runReconciledDraftWrite({
      expectedVersion: variables.dto.expectedVersion,
      isApplied: (draft) => batchRoutesMatch(draft, variables.dto),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: (draft) => recoverRoutePreviews(
        draft,
        variables.dto.routes.map((route) => route.originalUuid),
      ),
      write: () => createOrderService.saveRouteBatch(variables),
    }),
    onSuccess: (result, variables) => {
      queryClient.setQueryData(
        queries.createOrder.draft(variables.orderUuid).queryKey,
        (draft: DraftOrderVO | undefined) => updateDraft(
          draft,
          result.version,
          variables.dto.routes,
          result.data,
        ),
      )
      queryClient.invalidateQueries({ queryKey: queries.createOrder.draft(variables.orderUuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey })
    },
  })
}

function updateDraft(
  draft: DraftOrderVO | undefined,
  version: number,
  routes: ProcessRoutePreviewDTO[],
  previews: ProcessRoutePreviewVO[],
): DraftOrderVO | undefined {
  if (!draft) return draft
  const configs = routes.reduce(
    (current, route, index) => upsertRoute(current, route, previews[index]),
    draft.configs ?? [],
  )
  const order = draft.order ? { ...draft.order, version } : draft.order
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
