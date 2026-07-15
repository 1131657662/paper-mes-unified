import { describe, expect, it } from 'vitest'

import type { BackupRecord } from '../../types/dataBackup'
import { isDeleteProtected } from './backupRecordProtection'

describe('backup delete protection', () => {
  it('protects the last restore-verified backup', () => {
    const verified = backup('verified', 'VERIFIED')
    const unverified = backup('unverified', 'UNVERIFIED')

    expect(isDeleteProtected(verified, [unverified, verified])).toBe(true)
    expect(isDeleteProtected(unverified, [unverified, verified])).toBe(false)
  })

  it('allows deleting one verified backup when another verified backup remains', () => {
    const first = backup('first', 'VERIFIED')
    const second = backup('second', 'VERIFIED')

    expect(isDeleteProtected(first, [first, second])).toBe(false)
  })

  it('protects the last complete backup before any restore verification exists', () => {
    const complete = backup('complete', 'UNVERIFIED')
    const damaged = { ...backup('damaged', 'UNVERIFIED'), checksumAvailable: false }

    expect(isDeleteProtected(complete, [complete, damaged])).toBe(true)
  })
})

function backup(id: string, verificationStatus: BackupRecord['verificationStatus']): BackupRecord {
  return {
    id,
    createdAt: '2026-07-15 02:00:00',
    sizeBytes: 1024,
    databaseArchive: true,
    uploadIncluded: false,
    checksumAvailable: true,
    verificationStatus,
  }
}
