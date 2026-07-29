import { Modal } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { confirmAutoFinishConfigs, type AutoFinishConfigItem } from './AutoFinishConfigConfirm'

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>()
  return { ...actual, Modal: { ...actual.Modal, confirm: vi.fn() } }
})

describe('待处理加工方案确认', () => {
  beforeEach(() => vi.mocked(Modal.confirm).mockClear())

  it('说明保存当前工作台参数而不是声称重新生成默认配置', () => {
    confirmAutoFinishConfigs([configItem()])

    const options = vi.mocked(Modal.confirm).mock.calls[0]?.[0]
    const markup = renderToStaticMarkup(<>{options?.content}</>)

    expect(options?.title).toBe('确认保存待处理方案')
    expect(options?.okText).toBe('保存并进入预览确认')
    expect(options?.cancelText).toBe('返回继续检查')
    expect(markup).toContain('当前工作台参数（含默认值）')
    expect(markup).not.toContain('系统默认配置')
  })
})

function configItem(): AutoFinishConfigItem {
  return {
    roll: {
      localId: 'roll-1',
      paperName: '测试纸',
      gramWeight: 80,
      originalWidth: 1000,
      rollWeight: 500,
      processMode: 1,
      mainStepType: 2,
    },
    plan: {
      processMode: 1,
      mainStepType: 2,
      unitPrice: 151,
      finishSpecs: [{ itemType: 'FINISH', finishWidth: 1000, count: 1 }],
    },
  }
}
