import { describe, expect, it } from 'vitest'
import type { ProcessOrderPrintViewVO } from '../../types/processOrder'
import { printVersionMetadata, printVersionProps, printVersionWarning } from './printVersionModel'

describe('打印版本元数据', () => {
  it('快照时间统一精确到秒', () => {
    const props = printVersionProps('FINISHED', snapshotView({
      snapshotTime: '2026-07-13T21:51:57.899742300',
      snapshotUser: '张三',
    }))

    expect(props.snapshotTime).toBe('2026-07-13 21:51:57')
    expect(props.snapshotUser).toBe('张三')
  })

  it('历史快照缺少操作人时明确显示未记录', () => {
    const metadata = printVersionMetadata(snapshotView({ snapshotUser: undefined }))

    expect(metadata.snapshotUser).toBe('未记录')
  })

  it('待下发预览不虚构快照操作人', () => {
    const metadata = printVersionMetadata(snapshotView({ source: 'LIVE_PREVIEW' }))

    expect(metadata.snapshotUser).toBeUndefined()
  })

  it('历史兼容提示合并为一条完整警示', () => {
    const warning = printVersionWarning(snapshotView({ warning: '缺失结构已使用受保护数据补齐。' }))

    expect(warning).toBe('历史快照兼容模式：缺失结构已使用受保护数据补齐。')
  })
})

function snapshotView(overrides: Partial<ProcessOrderPrintViewVO>): ProcessOrderPrintViewVO {
  return {
    version: 'FINISHED',
    source: 'SNAPSHOT',
    availableVersions: ['ISSUED', 'FINISHED'],
    detail: {
      order: { uuid: 'order-1' },
      originalRolls: [],
      rolls: [],
      finishRolls: [],
      steps: [],
      rollProductions: [],
    },
    ...overrides,
  }
}
