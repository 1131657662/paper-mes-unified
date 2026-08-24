export type ProjectMemoryState = 'READY' | 'DEGRADED' | 'UNAVAILABLE'

export type ProjectMemoryVersionStatus = 'ACTIVE' | 'SUPERSEDED' | 'DRAFT'

export type ProjectMemoryPatchOperationType = 'add' | 'replace' | 'remove'

export type ProjectMemoryCandidateStatus =
  | 'CANDIDATE' | 'READY' | 'ACTIVE' | 'CONFLICT' | 'REJECTED' | 'EXPIRED'

export interface ProjectMemorySnapshot {
  memoryVersion: string
  schemaVersion: string
  checksum: string
  state: ProjectMemoryState
  document: unknown
}

export interface ProjectMemoryVersion {
  memoryVersion: string
  schemaVersion: string
  checksum: string
  status: ProjectMemoryVersionStatus
  patchNotes?: string
  createdBy: string
  approvedBy?: string
  createdAt: string
}

export interface ProjectMemoryPatchOperation {
  op: ProjectMemoryPatchOperationType
  path: string
  value?: unknown
}

export interface ProjectMemoryPatchPayload {
  expectedMemoryVersion: string
  operations: ProjectMemoryPatchOperation[]
  idempotencyKey: string
  reason: string
}

export interface ProjectMemoryRollbackPayload {
  expectedMemoryVersion: string
  targetMemoryVersion: string
  idempotencyKey: string
  reason: string
}

export interface ProjectMemoryCandidateDocument {
  type: 'TERM' | 'EXAMPLE' | 'RULE' | 'EXTERNAL_FACT' | 'EPISODE'
  scope: string
  status: 'ACTIVE'
  phrase?: string
  aliases?: string[]
  input?: string
  expected?: unknown
  intent?: string
  meaning?: string
  source?: string
  [key: string]: unknown
}

export interface ProjectMemoryCandidateEvidence {
  uuid: string
  phrase?: string
  sourceType: 'AI_CONFIRMED' | 'MANUAL_FINAL'
  proposedValue?: unknown
  finalValue?: unknown
  difference?: unknown
  previewReady?: boolean
  createdAt: string
}

export interface ProjectMemoryCandidateDetail {
  candidate: ProjectMemoryCandidate
  evidence: ProjectMemoryCandidateEvidence[]
}

export interface ProjectMemoryCandidate {
  uuid: string
  memoryId: string
  candidateType: ProjectMemoryCandidateDocument['type']
  candidate: ProjectMemoryCandidateDocument
  status: ProjectMemoryCandidateStatus
  distinctOrderCount: number
  firstSeenAt: string
  lastSeenAt: string
  expiresAt: string
  reviewedBy?: string
  reviewNotes?: string
  reviewedAt?: string
}

export interface ProjectMemoryCandidateApprovePayload {
  uuid: string
  expectedMemoryVersion: string
  idempotencyKey: string
  reason: string
  candidate?: ProjectMemoryCandidateDocument
}

export interface ProjectMemoryCandidateRejectPayload {
  uuid: string
  reason: string
}
