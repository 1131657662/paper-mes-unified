export type BackupVerificationStatus = 'VERIFIED' | 'UNVERIFIED'

export interface BackupRecord {
  id: string
  createdAt: string
  sizeBytes: number
  databaseArchive: boolean
  uploadIncluded: boolean
  checksumAvailable: boolean
  verificationStatus: BackupVerificationStatus
  verifiedAt?: string
}

export interface BackupStatus {
  enabled: boolean
  configured: boolean
  running: boolean
  platform: 'WINDOWS' | 'LINUX'
  runner: 'POWERSHELL' | 'BASH'
  missingComponents: string[]
  runningOperation?: 'BACKUP' | 'VERIFY'
  latestBackupAt?: string
  latestVerifiedAt?: string
  totalSpaceBytes: number
  usableSpaceBytes: number
  retentionDays: number
  backupCount: number
  latestBackupAgeHours?: number
  automaticEnabled: boolean
  automaticExecutionTime: string
  lastAutomaticAt?: string
  lastAutomaticStatus?: BackupTaskStatus
  automaticConsecutiveFailures: number
  nextAutomaticAt?: string
  offsiteStatus: 'NOT_CONFIGURED' | 'SUCCESS' | 'FAILED' | 'INVALID'
  offsiteLastSyncAt?: string
  offsiteRemoteName?: string
  message: string
}

export interface BackupOperation {
  accepted: boolean
  message: string
}

export type BackupTaskStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface BackupTask {
  uuid: string
  taskType: 'BACKUP' | 'AUTO_BACKUP' | 'VERIFY'
  backupId?: string
  taskStatus: BackupTaskStatus
  startedAt: string
  finishedAt?: string
  durationMs?: number
  operator: string
  message?: string
}
