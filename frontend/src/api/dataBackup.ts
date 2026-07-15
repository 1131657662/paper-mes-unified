import request from './request'
import type { BackupOperation, BackupRecord, BackupStatus, BackupTask } from '../types/dataBackup'

export function listBackups() {
  return request<BackupRecord[]>({ url: '/api/system/backups', method: 'get' })
}

export function getBackupStatus() {
  return request<BackupStatus>({ url: '/api/system/backups/status', method: 'get' })
}

export function createBackup() {
  return request<BackupOperation>({ url: '/api/system/backups', method: 'post' })
}

export function updateBackupEnabled(enabled: boolean) {
  return request<BackupStatus>({
    url: '/api/system/backups/enabled',
    method: 'put',
    data: { enabled },
  })
}

export function updateAutomaticBackup(enabled: boolean, executionTime: string) {
  return request<BackupStatus>({
    url: '/api/system/backups/automatic',
    method: 'put',
    data: { enabled, executionTime },
  })
}

export function verifyBackup(backupId: string) {
  return request<BackupOperation>({
    url: `/api/system/backups/${backupId}/verify`,
    method: 'post',
  })
}

export function listBackupTasks() {
  return request<BackupTask[]>({ url: '/api/system/backups/tasks', method: 'get' })
}

export function updateBackupRetention(retentionDays: number) {
  return request<BackupStatus>({
    url: '/api/system/backups/retention', method: 'put', data: { retentionDays },
  })
}

export function cleanupExpiredBackups() {
  return request<BackupOperation>({ url: '/api/system/backups/cleanup', method: 'post' })
}

export function deleteBackup(backupId: string) {
  return request<void>({ url: `/api/system/backups/${backupId}`, method: 'delete' })
}
