import { request } from '../../../api/request'
import type {
  ProcessAiConfirmResponse,
  ProcessAiCorrection,
  ProcessAiMessage,
  ProcessAiManagedProvider,
  ProcessAiProviderSettings,
  ProcessAiSession,
  ProcessAiStatus,
  ProcessAiParseResult,
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
  revise: (input: ReviseInput) => request<ProcessAiParseResult>({
    url: `${processUrl(input.orderUuid)}/revise`, method: 'post', data: input.request,
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

export interface ConfirmInput {
  orderUuid: string
  request: {
    conversationId: string
    parseId: string
    expectedVersion: number
    applyIdempotencyKey: string
    acceptedFieldPaths: string[]
    parseRevision?: number
    previewHash?: string
    acknowledgedDefaultIds?: string[]
  }
}

export interface ReviseInput {
  orderUuid: string
  request: {
    conversationId: string
    parseId: string
    expectedVersion: number
    parseRevision: number
    corrections: ProcessAiCorrection[]
  }
}
