interface RefetchResult<TData> {
  data?: TData
  error?: unknown
  isSuccess: boolean
}

interface Options<TData> {
  onPersisted?: () => void
  onRefetch: () => Promise<RefetchResult<TData>>
  onReloaded: (data: TData) => void
  onResetInitialization: () => void
}

export interface ConflictReloadResult {
  error?: unknown
  reloaded: boolean
}

export async function reloadBackRecordConflict<TData>(options: Options<TData>): Promise<ConflictReloadResult> {
  try {
    const result = await options.onRefetch()
    if (!result.isSuccess || !result.data) return { error: result.error, reloaded: false }
    options.onResetInitialization()
    options.onReloaded(result.data)
    options.onPersisted?.()
    return { reloaded: true }
  } catch (error) {
    return { error, reloaded: false }
  }
}
