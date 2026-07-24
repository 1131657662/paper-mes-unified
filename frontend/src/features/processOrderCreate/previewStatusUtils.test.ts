import { describe, expect, it } from 'vitest'
import type { PlanPreviewVO } from '../../types/processOrder'
import { rollPreviewStatus } from './previewStatusUtils'
import type { RollDraft } from './types'

describe('仅附加工艺预览状态', () => {
  it('预览确认使用后端已就绪结果', () => {
    const status = rollPreviewStatus({
      roll: serviceOnlyRoll(),
      preview: servicePreview({ ready: true, summary: '已配置重新包装' }),
    })

    expect(status).toMatchObject({ kind: 'ready', blocking: false, detail: '已配置重新包装' })
  })

  it('后端校验未通过时阻断提交并显示原因', () => {
    const status = rollPreviewStatus({
      roll: serviceOnlyRoll(),
      preview: servicePreview({ ready: false, errors: ['至少添加一条附加工艺'] }),
    })

    expect(status).toMatchObject({
      kind: 'blocked',
      blocking: true,
      detail: '至少添加一条附加工艺',
    })
  })

  it('工艺明细明确未配置时不使用旧预览放行', () => {
    const status = rollPreviewStatus({
      roll: serviceOnlyRoll(),
      preview: servicePreview({ ready: true }),
      serviceConfigured: false,
    })

    expect(status).toMatchObject({ kind: 'pending', blocking: true })
  })
})

function serviceOnlyRoll(): RollDraft {
  return {
    localId: 'local-1',
    uuid: 'roll-1',
    paperName: '白卡',
    gramWeight: 250,
    originalWidth: 1200,
    rollWeight: 1000,
    processMode: 4,
  }
}

function servicePreview(overrides: Partial<PlanPreviewVO>): PlanPreviewVO {
  return { processMode: 4, originalUuid: 'roll-1', ...overrides }
}
