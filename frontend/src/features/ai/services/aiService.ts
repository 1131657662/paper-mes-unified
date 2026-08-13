import { request } from '../../../api/request'
import type { AiAssistRequest, AiAssistResponse, AiStatusResponse } from '../types'

export const aiService = {
  assist: (payload: AiAssistRequest, signal?: AbortSignal) => request<AiAssistResponse>({
    url: '/api/ai/assist',
    method: 'post',
    data: payload,
    signal,
    deferUncertainErrorNotification: true,
  }),
  status: () => request<AiStatusResponse>({
    url: '/api/ai/status',
    method: 'get',
    silentError: true,
  }),
}
