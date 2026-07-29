import { message } from 'antd'
import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { ProcessStep } from '../../../types/processOrder'
import { invalidateProcessOrderReadModels } from '../../processOrderDetail/hooks/invalidateProcessOrderReadModels'

export interface DraftServiceMutationOptions {
  draftVersion: number
  onSynchronizeVersion: () => Promise<number>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  orderUuid?: string
  versionSyncBlocked: boolean
}

export function draftServiceMutationLifecycle(options: DraftServiceMutationOptions) {
  const synchronizeLatest = async (queryClient: QueryClient, orderUuid: string) => {
    await invalidateProcessOrderReadModels(queryClient, orderUuid)
    return options.onSynchronizeVersion()
  }
  return {
    ensureVersionReady: async (queryClient: QueryClient, orderUuid: string) => {
      if (!options.versionSyncBlocked) return options.draftVersion
      const version = await synchronizeLatest(queryClient, orderUuid)
      options.onVersionSyncBlockedChange(false)
      return version
    },
    clearVersionSyncRequired: () => options.onVersionSyncBlockedChange(false),
    markVersionSyncRequired: () => options.onVersionSyncBlockedChange(true),
    synchronizeLatest,
  }
}

export function requiredDraftOrderUuid(orderUuid?: string): string {
  if (orderUuid) return orderUuid
  message.warning('请先保存原纸明细')
  throw new Error('missing order uuid')
}

export function currentProcessOrderSteps(
  queryClient: QueryClient,
  orderUuid: string,
  fallback?: ProcessStep[],
): ProcessStep[] | undefined {
  const detail = queryClient.getQueryData<{ steps?: ProcessStep[] }>(
    queries.processOrderDetail.detail(orderUuid).queryKey,
  )
  return detail ? detail.steps ?? [] : fallback
}
