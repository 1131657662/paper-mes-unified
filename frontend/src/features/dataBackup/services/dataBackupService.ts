import {
  cleanupExpiredBackups, createBackup, deleteBackup, getBackupStatus, listBackups,
  listBackupTasks, updateBackupEnabled, updateBackupRetention, verifyBackup,
  updateAutomaticBackup,
} from '../../../api/dataBackup'

export const dataBackupService = {
  create: createBackup,
  cleanup: cleanupExpiredBackups,
  delete: deleteBackup,
  list: listBackups,
  tasks: listBackupTasks,
  status: getBackupStatus,
  updateEnabled: updateBackupEnabled,
  updateAutomatic: updateAutomaticBackup,
  updateRetention: updateBackupRetention,
  verify: verifyBackup,
}
