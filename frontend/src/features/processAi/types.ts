import type { PlanPreviewVO, ProcessPlanDTO } from '../../types/processOrder'
import type { RollDraft } from '../processOrderCreate/types'

export type ProcessAiManagedProvider = 'deepseek' | 'zhipu'

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

export type ProcessAiProcessType = 'REWIND' | 'SAW' | 'DIRECT_SHIP' | 'SERVICE_ONLY' | 'ANCILLARY_ONLY'
export type ProcessAiProcessMode = 'STANDARD' | 'ON_SITE' | 'DIRECT_SHIP' | 'SERVICE_ONLY'

export interface ProcessAiUnderstandingEvidence extends ProcessAiEvidence {
  sourceType: 'CUSTOMER_TEXT' | 'DB_FACT' | 'APPROVED_MEMORY' | 'DEFAULT' | 'MODEL_INFERENCE'
  sourceRef: string
  normalizedRange?: string
}

export interface ProcessAiAssignment {
  sourceRollRefs: string[]
  ownerRollRef: string
  coveredRollRefs: string[]
  processType: ProcessAiProcessType
  processMode?: ProcessAiProcessMode
  rewindIntent?: Record<string, unknown>
  sawIntent?: Record<string, unknown>
  ancillaryRequirements?: Record<string, unknown>
  evidence: ProcessAiEvidence[]
  customerSpecs?: ProcessAiCustomerSpec[]
}

export interface ProcessAiCustomerSpec {
  outputIndex: number
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  overrideReason?: string
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

export interface ProcessAiClarificationQuestion {
  questionId: string
  field: string
  parseRevision: number
  question: string
  options: ProcessAiClarificationOption[]
  allowUnknown: boolean
}

export interface ProcessAiClarificationOption {
  code: string
  label: string
}

export interface ProcessAiUnderstandingResult {
  parseId: string
  schemaVersion: '2.0'
  conclusion: string
  evidence: ProcessAiUnderstandingEvidence[]
  assumptions: string[]
  risks: string[]
  clarificationQuestions: ProcessAiClarificationQuestion[]
  needsClarification: boolean
}

export interface ProcessAiCompiledPlan {
  ownerRollRef: string
  originalUuid: string
  coveredOriginalUuids: string[]
  plan: ProcessPlanDTO
  preview: PlanPreviewVO
}

export interface ProcessAiRollConfiguration {
  ownerRollRef: string
  originalUuids: string[]
  processMode: number
  mainStepType?: number
}

export interface ProcessAiPackagingCandidate {
  ownerRollRef: string
  originalUuid: string
  coveredOriginalUuids: string[]
  stepType: 3 | 4
  packagingType: 'STRIP_SORT' | 'REPACKAGE' | 'FILM' | 'BOX' | 'OTHER'
  stepName: string
  billingBasis?: 'PIECE' | 'TON'
  serviceQuantity?: number
  billingMode: 1 | 2 | 3
  unitPrice?: number
  billingAmount?: number
  remark: string
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
    rollConfigurations?: ProcessAiRollConfiguration[]
    plans: ProcessAiCompiledPlan[]
    packagingCandidates: ProcessAiPackagingCandidate[]
    errors: string[]
    warnings: string[]
  }
  expiresAt: string
  resultKind?: 'EXTRACTION' | 'UNDERSTANDING' | 'FAILURE'
  dialogueState?: string
  understanding?: ProcessAiUnderstandingResult
  clarificationQuestions?: ProcessAiClarificationQuestion[]
  requiredDefaultIds?: string[]
  previewHash?: string
}

export interface ProcessAiCorrection {
  assignmentRef: string
  field: 'finishCoreDiameter' | 'widthMm' | 'quantityScope' | 'customerPaperName'
    | 'customerGramWeight' | 'customerFinishWidth' | 'customerSpecOverrideReason'
  value?: number
  textValue?: string
  unit?: 'inch' | 'mm'
  outputIndex?: number
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
  previewHash?: string
  acknowledgedDefaultIds?: string[]
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
