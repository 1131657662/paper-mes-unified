import type { ProcessAiStatus } from './types'

export interface ProcessAiAvailability {
  unavailable?: string
  hint: string
}

export function processAiAvailability(
  orderUuid: string | undefined,
  status: ProcessAiStatus | undefined,
  statusError = false,
): ProcessAiAvailability {
  if (!orderUuid) return { unavailable: '请先完成母卷录入', hint: '请先完成母卷录入' }
  if (statusError) {
    return {
      hint: 'AI 状态检查失败，点击后将重试 AI 会话；如仍失败请检查本地后端连接',
    }
  }
  if (!status) return { hint: '打开 AI 工艺助手' }
  if (!status.enabled) return { unavailable: 'AI 工艺解析尚未启用', hint: 'AI 工艺解析尚未启用' }
  if (!status.ready && status.unavailableReason !== 'AI_MEMORY_UNAVAILABLE') {
    const reason = status.unavailableReason || 'AI_PROCESS_UNAVAILABLE'
    return { unavailable: reason, hint: processAiReasonMessage(reason) }
  }
  return { hint: '打开 AI 工艺助手' }
}

function processAiReasonMessage(reason: string): string {
  const messages: Record<string, string> = {
    AI_PROVIDER_NOT_CONFIGURED: '请先配置 DeepSeek 或智谱 API Key',
    AI_MESSAGE_KEY_UNAVAILABLE: 'AI 会话加密密钥未配置，请通过 dev.ps1 重启本地服务',
    AI_MEMORY_REFERENCE_HMAC_UNAVAILABLE: '共享记忆保护密钥未配置，请通过 dev.ps1 重启本地服务',
    AI_MEMORY_UNAVAILABLE: '共享记忆暂不可用，请稍后重试',
  }
  return messages[reason] || 'AI 工艺解析尚未就绪'
}
