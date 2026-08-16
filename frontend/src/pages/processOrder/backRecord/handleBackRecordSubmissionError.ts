import { message } from 'antd'
import { BizError, notifyErrorOnce } from '../../../api/request'
import type { ConflictReloadResult } from './reloadBackRecordConflict'

interface Options {
  error: unknown
  onAuthorizationRequired: () => void
  onVarianceRequired: () => void
  reloadConflict: () => Promise<ConflictReloadResult>
}

export async function handleBackRecordSubmissionError(
  options: Options,
): Promise<void> {
  if (options.error instanceof BizError && options.error.errorCode === 'E005') {
    options.onAuthorizationRequired()
    return
  }
  if (options.error instanceof BizError && options.error.errorCode === 'E007') {
    options.onVarianceRequired()
    return
  }
  if (
    !(options.error instanceof BizError) ||
    options.error.errorCode !== 'E006'
  ) {
    notifyErrorOnce(options.error, '回录失败，请检查数据后重试')
    return
  }
  const reload = await options.reloadConflict()
  if (!reload.reloaded) {
    notifyErrorOnce(
      reload.error,
      '数据已被他人修改，但服务端最新内容加载失败，请保留当前页面并重试',
    )
    return
  }
  message.warning(
    '数据已被他人修改，已合并服务端最新内容并保留本地草稿，请重新核对后提交',
  )
}
