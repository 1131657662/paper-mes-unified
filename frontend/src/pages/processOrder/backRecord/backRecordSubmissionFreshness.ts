import type { ProcessOrderDetailVO } from '../../../types/processOrder'

interface Options {
  expectedVersion?: number
  onBeforeRefetch?: () => void
  onRefetch: () => Promise<{ data?: ProcessOrderDetailVO; error?: unknown; isSuccess: boolean }>
  onConflictReloaded: (detail: ProcessOrderDetailVO) => void
}

type RefreshResult =
  | { status: 'current'; detail: ProcessOrderDetailVO }
  | { status: 'changed'; detail: ProcessOrderDetailVO }
  | { status: 'failed'; error?: unknown }

export async function refreshBackRecordBeforeSubmit(options: Options): Promise<RefreshResult> {
  try {
    options.onBeforeRefetch?.()
    const result = await options.onRefetch()
    if (!result.isSuccess || !result.data) return { status: 'failed', error: result.error }
    if (result.data.order.version !== options.expectedVersion) {
      options.onConflictReloaded(result.data)
      return { status: 'changed', detail: result.data }
    }
    return { status: 'current', detail: result.data }
  } catch (error) {
    return { status: 'failed', error }
  }
}
