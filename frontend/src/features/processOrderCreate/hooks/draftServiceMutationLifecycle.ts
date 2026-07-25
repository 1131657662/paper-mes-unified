import { message } from 'antd'
import type { QueryClient } from '@tanstack/react-query'
import { invalidateProcessOrderReadModels } from '../../processOrderDetail/hooks/invalidateProcessOrderReadModels'

export interface DraftServiceMutationOptions {
  onSynchronizeVersion: () => Promise<void>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  orderUuid?: string
  versionSyncBlocked: boolean
}

export function draftServiceMutationLifecycle(options: DraftServiceMutationOptions) {
  const synchronizeLatest = async (queryClient: QueryClient, orderUuid: string) => {
    await invalidateProcessOrderReadModels(queryClient, orderUuid)
    await options.onSynchronizeVersion()
    options.onVersionSyncBlockedChange(false)
  }
  return {
    ensureVersionReady: async (queryClient: QueryClient, orderUuid: string) => {
      if (options.versionSyncBlocked) await synchronizeLatest(queryClient, orderUuid)
    },
    markVersionSyncRequired: () => options.onVersionSyncBlockedChange(true),
    synchronizeLatest,
  }
}

export function requiredDraftOrderUuid(orderUuid?: string): string {
  if (orderUuid) return orderUuid
  message.warning('请先保存原纸明细')
  throw new Error('missing order uuid')
}
