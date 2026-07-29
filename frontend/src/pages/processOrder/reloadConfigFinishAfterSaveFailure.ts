interface RefetchResult {
  error?: unknown
  isSuccess: boolean
}

interface ReloadResult {
  error?: unknown
  reloaded: boolean
}

export async function reloadConfigFinishAfterSaveFailure(
  refetch: () => Promise<RefetchResult>,
): Promise<ReloadResult> {
  try {
    const result = await refetch()
    return result.isSuccess ? { reloaded: true } : { error: result.error, reloaded: false }
  } catch (error) {
    return { error, reloaded: false }
  }
}
