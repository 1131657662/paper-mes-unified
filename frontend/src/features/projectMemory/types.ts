export type ProjectMemoryState = 'READY' | 'DEGRADED' | 'UNAVAILABLE'

export type ProjectMemoryVersionStatus = 'ACTIVE' | 'SUPERSEDED' | 'DRAFT'

export type ProjectMemoryPatchOperationType = 'add' | 'replace' | 'remove'

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
