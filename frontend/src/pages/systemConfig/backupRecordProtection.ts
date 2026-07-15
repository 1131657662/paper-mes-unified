import type { BackupRecord } from '../../types/dataBackup'

export function isDeleteProtected(record: BackupRecord, records: BackupRecord[]): boolean {
  const verifiedCount = records.filter(isVerifiedBackup).length
  if (isVerifiedBackup(record)) return verifiedCount <= 1
  const completeCount = records.filter(isCompleteBackup).length
  return verifiedCount === 0 && isCompleteBackup(record) && completeCount <= 1
}

function isCompleteBackup(record: BackupRecord) {
  return record.databaseArchive && record.checksumAvailable
}

function isVerifiedBackup(record: BackupRecord) {
  return isCompleteBackup(record) && record.verificationStatus === 'VERIFIED'
}
