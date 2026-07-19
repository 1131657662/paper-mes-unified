import { describe, expect, it } from 'vitest'
import type { ExportTask } from '../../types/exportTask'
import { exportTaskActions, exportTaskModuleLabel } from './exportTaskDisplay'

describe('导出任务展示模型', () => {
  it.each([
    { status: 1, expected: ['cancel'] },
    { status: 2, expected: [] },
    { status: 3, expected: ['download'] },
    { status: 4, expected: ['retry', 'acknowledge'] },
    { status: 5, expected: [] },
    { status: 6, expected: ['retry', 'acknowledge'] },
  ])('状态 $status 显示正确操作', ({ status, expected }) => {
    expect(exportTaskActions(task(status))).toEqual(expected)
  })

  it('已确认失败任务只保留重试操作', () => {
    expect(exportTaskActions(task(4, true))).toEqual(['retry'])
  })

  it.each([4, 6])('状态 %s 达到重试上限后不再显示重试操作', (status) => {
    expect(exportTaskActions(task(status, false, 3, 3))).toEqual(['acknowledge'])
  })

  it('缺少次数信息时保留重试操作以兼容旧数据', () => {
    expect(exportTaskActions(task(4))).toEqual(['retry', 'acknowledge'])
  })

  it('未知模块使用系统标签', () => {
    expect(exportTaskModuleLabel('unknown')).toBe('系统')
  })

  it('业务权限撤销后仅保留失败提醒确认操作', () => {
    expect(exportTaskActions({ ...task(4), resourceAccessible: false })).toEqual(['acknowledge'])
  })

  it('业务权限撤销后不再提供文件下载', () => {
    expect(exportTaskActions({ ...task(3), resourceAccessible: false })).toEqual([])
  })
})

function task(taskStatus: number, acknowledged = false, attemptCount?: number, maxAttempts?: number): ExportTask {
  return {
    uuid: 'task-1', taskType: 'TEST', taskName: '测试导出', sourceUuid: 'source-1',
    taskStatus, progress: 0, createTime: '2026-07-19T08:00:00', acknowledged,
    resourceAccessible: true, attemptCount, maxAttempts,
  }
}
