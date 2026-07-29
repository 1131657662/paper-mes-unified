import { message } from 'antd'
import { isUncertainRequestError, notifyErrorOnce } from '../../api/request'

const CONFIRM_DELAYS_MS = [0, 500, 1_000, 2_000, 4_000, 8_000] as const

interface MutationCommitOptions<T> {
  clearVersionSyncRequired: () => void
  ensureVersionReady: () => Promise<void>
  isAppliedAfterSync?: () => boolean
  markVersionSyncRequired: () => void
  mutate: () => Promise<T>
  recoverResult?: () => T
  synchronizeVersion: () => Promise<unknown>
}

export async function runVersionSynchronizedMutation<T>(
  options: MutationCommitOptions<T>,
): Promise<T> {
  await options.ensureVersionReady()
  options.markVersionSyncRequired()
  let result: T
  try {
    result = await options.mutate()
  } catch (error) {
    if (!isUncertainRequestError(error)) {
      options.clearVersionSyncRequired()
      throw error
    }
    return recoverUncertainMutation(options, error)
  }
  await options.synchronizeVersion()
  options.clearVersionSyncRequired()
  return result
}

async function recoverUncertainMutation<T>(
  options: MutationCommitOptions<T>,
  originalError: unknown,
): Promise<T> {
  for (const delayMs of CONFIRM_DELAYS_MS) {
    await waitForConfirmation(delayMs)
    if (await synchronizeAndConfirm(options)) {
      options.clearVersionSyncRequired()
      message.success('响应异常，但已从服务器确认附加工艺保存成功')
      return options.recoverResult?.() as T
    }
  }
  notifyErrorOnce(originalError, '附加工艺保存结果无法确认，请检查网络后重试')
  throw originalError
}

async function synchronizeAndConfirm<T>(options: MutationCommitOptions<T>): Promise<boolean> {
  try {
    await options.synchronizeVersion()
    return options.isAppliedAfterSync?.() === true
  } catch {
    return false
  }
}

async function waitForConfirmation(delayMs: number): Promise<void> {
  if (delayMs === 0) return
  await new Promise((resolve) => globalThis.setTimeout(resolve, delayMs))
}
