import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import CustomerFinishedProductDetailTable from './CustomerFinishedProductDetailTable'

describe('客户成品逐件明细', () => {
  it('展示完整来源母卷和回录备注但隐藏生产内部参数', () => {
    const markup = renderToStaticMarkup(<CustomerFinishedProductDetailTable rows={[row()]} />)

    expect(markup).toContain('母卷2')
    expect(markup).toContain('编号 NO-2')
    expect(markup).toContain('白卡 / 302 g / 2510 mm / 2190 kg')
    expect(markup).toContain('边部轻微压痕')
    expect(markup).not.toContain('直径')
    expect(markup).not.toContain('纸芯')
    expect(markup).not.toContain('分摊比例')
    expect(markup).not.toContain('分摊重量')
  })
})

function row() {
  return {
    key: 'finish-1',
    finish: {
      uuid: 'finish-1',
      finishRollNo: 'A000001',
      paperName: '白卡',
      gramWeight: 300,
      finishWidth: 900,
      finishDiameter: 1000,
      finishCoreDiameter: 3,
      actualWeight: 950,
      actualRemark: '边部轻微压痕',
    },
    sources: [{
      originalUuid: 'roll-2',
      rowSort: 2,
      extraNo: 'NO-2',
      paperName: '白卡',
      gramWeight: 300,
      actualGramWeight: 302,
      originalWidth: 2520,
      actualWidth: 2510,
      totalWeight: 2200,
      actualWeight: 2190,
      shareRatio: 100,
      shareWeight: 950,
    }],
  }
}
