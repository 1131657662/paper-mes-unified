import { request } from '../../../api/request'
import type {
  ProcessAiConfirmResponse,
  ProcessAiMessage,
  ProcessAiPendingPackagingCandidate,
  ProcessAiManagedProvider,
  ProcessAiProviderSettings,
  ProcessAiSession,
  ProcessAiStatus,
} from '../types'

const processUrl = (orderUuid: string) => `/api/process-orders/${orderUuid}/ai/process-parse`

export const processAiService = {
  status: () => request<ProcessAiStatus>({
    url: '/api/ai/process-status', method: 'get', silentError: true,
  }),
  providerSettings: (provider: ProcessAiManagedProvider) => request<ProcessAiProviderSettings>({
    url: `/api/ai/provider-settings/${provider}`, method: 'get',
  }),
  updateProviderKey: (provider: ProcessAiManagedProvider, apiKey: string) => request<ProcessAiProviderSettings>({
    url: `/api/ai/provider-settings/${provider}`, method: 'put', data: { apiKey },
  }),
  deleteProviderKey: (provider: ProcessAiManagedProvider) => request<ProcessAiProviderSettings>({
    url: `/api/ai/provider-settings/${provider}`, method: 'delete',
  }),
  openSession: (input: OpenSessionInput) => request<ProcessAiSession>({
    url: `${processUrl(input.orderUuid)}/session`, method: 'post',
    data: { expectedVersion: input.expectedVersion, currentStep: input.currentStep },
  }),
  refreshMemory: (input: RefreshMemoryInput) => request<ProcessAiSession>({
    url: `${processUrl(input.orderUuid)}/session/${input.conversationId}/refresh-memory`,
    method: 'post', data: { expectedVersion: input.expectedVersion },
  }),
  messages: (input: MessageInput) => request<ProcessAiMessage[]>({
    url: `${processUrl(input.orderUuid)}/session/${input.conversationId}/messages`,
    method: 'get', params: { expectedVersion: input.expectedVersion },
  }),
  confirm: (input: ConfirmInput) => request<ProcessAiConfirmResponse>({
    url: `${processUrl(input.orderUuid)}/confirm`, method: 'post', data: input.request,
  }),
  pendingPackaging: (input: PendingPackagingInput) => request<ProcessAiPendingPackagingCandidate[]>({
    url: `${processUrl(input.orderUuid)}/packaging-candidates`, method: 'get',
    params: { expectedVersion: input.expectedVersion },
  }),
  dismissPackaging: (input: DismissPackagingInput) => request<void>({
    url: `${processUrl(input.orderUuid)}/packaging-candidates/${input.parseId}/${input.ownerRollRef}/dismiss`,
    method: 'post', params: { expectedVersion: input.expectedVersion },
  }),
}

export interface OpenSessionInput {
  orderUuid: string
  expectedVersion: number
  currentStep: 3 | 4
}

export interface MessageInput {
  orderUuid: string
  conversationId: string
  expectedVersion: number
}

export interface RefreshMemoryInput {
  orderUuid: string
  conversationId: string
  expectedVersion: number
}

export interface PendingPackagingInput {
  orderUuid: string
  expectedVersion: number
}

export interface DismissPackagingInput extends PendingPackagingInput {
  parseId: string
  ownerRollRef: string
}

export interface ConfirmInput {
  orderUuid: string
  request: {
    conversationId: string
    parseId: string
    expectedVersion: number
    applyIdempotencyKey: string
    acceptedFieldPaths: string[]
  }
}
