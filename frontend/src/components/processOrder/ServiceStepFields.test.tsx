import { Form } from 'antd'
import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ProcessCatalog } from '../../types/processCatalog'
import ServiceStepFields from './ServiceStepFields'

const catalog: ProcessCatalog = {
  uuid: 'strip-sort',
  stepType: 3,
  code: 'STRIP_SORT',
  name: '剥损整理',
  category: 'SERVICE',
  pricingStrategy: 'SERVICE_QUANTITY',
  producesInventoryOutput: false,
  allowsLossRecording: true,
  allowsMainProcess: false,
  units: [{ code: 'PIECE', name: '件', defaultUnit: true }],
  billingModes: [1, 3, 4],
}

describe('服务工序计费字段', () => {
  it('首次挂载时默认暂不定价并说明数量自动计算', () => {
    const markup = renderToStaticMarkup(
      <Form><ServiceStepFields catalog={catalog} /></Form>,
    )

    expect(markup).toContain('暂不定价')
    expect(markup).toContain('预计计费单位')
    expect(markup).toContain('计费数量由系统自动计算')
    expect(markup).not.toContain('服务数量（件）')
  })

  it('目录首选固定金额时显示金额字段', () => {
    const markup = renderToStaticMarkup(
      <Form><ServiceStepFields catalog={{ ...catalog, billingModes: [3, 4] }} billingMode={3} /></Form>,
    )

    expect(markup).toContain('本卷固定金额（元）')
    expect(markup).not.toContain('服务数量（件）')
  })

  it('紧凑工作台中明确区分固定总额和每卷金额', () => {
    const markup = renderToStaticMarkup(
      <Form><ServiceStepFields catalog={catalog} billingMode={3} compact /></Form>,
    )

    expect(markup).toContain('批量金额口径')
    expect(markup).toContain('所选合计')
    expect(markup).toContain('每卷金额')
  })
})
