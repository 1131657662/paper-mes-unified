import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import type { ProcessStepDTO } from '../../api/processOrder'
import type { RollDraft } from '../processOrderCreate/types'

export interface ProcessAiProviderSettings {
  provider: string
  model: string
  baseUrl: string
  configured: boolean
  source: 'DATABASE' | 'ENVIRONMENT' | 'NONE'
  maskedApiKey?: string
  enabled: boolean
  databaseStorageReady: boolean
  updatedBy?: string
  updatedAt?: string
}

export interface ProcessAiStatus {
  enabled: boolean
  ready: boolean
  provider: string
  model: string
  providerConfigured: boolean
  fallbackProvider: string
  fallbackModel: string
  fallbackConfigured: boolean
  messageEncryptionReady: boolean
  projectMemoryState: string
  unavailableReason?: string
}

export interface ProcessAiSession {
  conversationId: string
  status: string
  currentStep: number
  draftVersion: number
  projectMemoryVersion: string
  memoryGeneration: number
  latestProjectMemoryVersion?: string
  memoryRefreshAvailable: boolean
  resumed: boolean
}

export interface ProcessAiMessage {
  sequenceNo: number
  role: 'USER' | 'ASSISTANT'
  status: 'PARTIAL' | 'FINAL' | 'FAILED'
  content: string
  structuredResult?: string
  createdAt: string
}

export interface ProcessAiEvidence {
  field: string
  text: string
}

export interface ProcessAiAssignment {
  sourceRollRefs: string[]
  ownerRollRef: string
  coveredRollRefs: string[]
  processType: 'REWIND' | 'SAW'
  rewindIntent?: Record<string, unknown>
  sawIntent?: Record<string, unknown>
  ancillaryRequirements?: Record<string, unknown>
  evidence: ProcessAiEvidence[]
}

export interface ProcessAiExtractionResult {
  parseId: string
  schemaVersion: string
  assignments: ProcessAiAssignment[]
  unmappedText: string[]
  conflicts: string[]
  needsClarification: boolean
  clarificationQuestions: string[]
}

export interface ProcessAiCompiledPlan {
  ownerRollRef: string
  originalUuid: string
  coveredOriginalUuids: string[]
  plan: ProcessPlanDTO
  preview: PlanPreviewVO
}

export interface ProcessAiPackagingCandidate {
  ownerRollRef: string
  originalUuid: string
  coveredOriginalUuids: string[]
  stepType: 4
  packagingType: 'FILM' | 'BOX' | 'OTHER'
  stepName: string
  billingBasis?: 'PIECE' | 'TON'
  serviceQuantity?: number
  billingMode: 2 | 3
  unitPrice?: number
  billingAmount?: number
  remark: string
}

export interface ProcessAiPackagingDraft {
  parseId: string
  ownerRollRef: string
  values: ProcessStepDTO
}

export interface ProcessAiPendingPackagingCandidate {
  parseId: string
  candidate: ProcessAiPackagingCandidate
}

export interface ProcessAiParseResult {
  conversationId: string
  parseId: string
  parseRevision: number
  expectedVersion: number
  nextVersion?: number
  status: string
  baseline?: ProcessAiReviewBaseline
  result: ProcessAiExtractionResult
  compiled: {
    eligible: boolean
    plans: ProcessAiCompiledPlan[]
    packagingCandidates: ProcessAiPackagingCandidate[]
    errors: string[]
    warnings: string[]
  }
  expiresAt: string
}

export interface ProcessAiConfirmResponse {
  conversationId: string
  parseId: string
  parseRevision: number
  expectedVersion: number
  nextVersion: number
  status: string
  acceptedFieldPaths: string[]
  plans: Record<string, ProcessAiCompiledPlan>
  packagingCandidates: ProcessAiPackagingCandidate[]
  warnings: string[]
  planHash: string
  remarkLong?: string
}

export interface ProcessAiBaselinePlan {
  ownerRollRef: string
  originalUuid: string
  processMode?: number
  mainStepType?: number
  route: boolean
  plan?: ProcessPlanDTO
}

export interface ProcessAiReviewBaseline {
  remarkLong?: string
  plans: ProcessAiBaselinePlan[]
}

export interface ProcessAiCurrentDraft {
  remarkLong?: string
  rolls: RollDraft[]
  plans: Record<string, ProcessPlanDTO>
}
