import { useMutation, useQueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DraftOrderVO, ProcessConfigDraftVO, ProcessRoutePreviewDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import { createOrderService } from '../services/createOrderService'
import {
  readLatestDraft,
  recoverRoutePreviews,
  runReconciledDraftWrite,
  singleRouteMatches,
} from '../draftWriteReconciliation'

export function useSaveRoute() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (variables: Parameters<typeof createOrderService.saveRoute>[0]) => runReconciledDraftWrite({
      expectedVersion: Number(variables.request.expectedVersion),
      isApplied: (draft) => singleRouteMatches(draft, variables.request),
      readLatest: () => readLatestDraft(queryClient, variables.orderUuid),
      recoverData: (draft) => recoverRoutePreviews(draft, [variables.request.originalUuid])[0]!,
      write: () => createOrderService.saveRoute(variables),
    }),
    onSuccess: (result, variables) => {
      queryClient.setQueryData(
        queries.createOrder.draft(variables.orderUuid).queryKey,
        (draft: DraftOrderVO | undefined) => updateDraftRoute(
          draft,
          variables.request,
          result.data,
          result.version,
        ),
      )
      queryClient.invalidateQueries({ queryKey: queries.createOrder.draft(variables.orderUuid).queryKey })
      queryClient.invalidateQueries({ queryKey: queries.createOrder.drafts.queryKey })
    },
  })
}

function updateDraftRoute(
  draft: DraftOrderVO | undefined,
  route: ProcessRoutePreviewDTO,
  preview: ProcessRoutePreviewVO,
  version: number,
): DraftOrderVO | undefined {
  if (!draft || !route.originalUuid) return draft
  const configs = upsertRouteConfig(draft.configs ?? [], route, preview)
  const order = draft.order ? { ...draft.order, version } : draft.order
  return { ...draft, configs, order }
}

function upsertRouteConfig(
  configs: ProcessConfigDraftVO[],
  route: ProcessRoutePreviewDTO,
  preview: ProcessRoutePreviewVO,
): ProcessConfigDraftVO[] {
  const next = routeConfig(route, preview)
  const exists = configs.some((config) => config.originalUuid === route.originalUuid)
  if (!exists) return [...configs, next]
  return configs.map((config) => (config.originalUuid === route.originalUuid ? { ...config, ...next } : config))
}

function routeConfig(route: ProcessRoutePreviewDTO, preview: ProcessRoutePreviewVO): ProcessConfigDraftVO {
  return {
    originalUuid: route.originalUuid,
    processMode: 1,
    mainStepType: route.stages?.[0]?.stepType,
    configStatus: 1,
    configType: 'routePlan',
    route,
    routePreview: preview,
  }
}
