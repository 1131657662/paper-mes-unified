export type AiDecision = 'ANSWER' | 'CLARIFY' | 'REFUSE'

export type AiDataMode = 'DISABLED' | 'FAQ_ONLY' | 'CONTEXT_ALLOWLIST'

export interface AiAssistRequest {
  question: string
  pageTemplate: string
  contextEpoch: string
}

export interface AiCitation {
  ruleId: string
  title: string
  version: string
}

export interface AiAssistResponse {
  requestId: string
  decision: AiDecision
  confidence: string
  answer: string
  safeNextSteps: string[]
  citations: AiCitation[]
  dataMode: AiDataMode
  provider: string
}

export interface AiStatusResponse {
  enabled: boolean
  dataMode: AiDataMode
  rulesVersion: string
  rulesReady: boolean
  provider: 'LOCAL_RULES' | 'ZHIPU'
}
