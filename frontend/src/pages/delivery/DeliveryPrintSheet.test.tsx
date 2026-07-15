import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { DeliveryDetailVO } from '../../types/delivery'
import DeliveryPrintSheet from './DeliveryPrintSheet'

describe('司机单据预览', () => {
  it('只显示门幅和回录备注并隐藏生产内部字段', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail()} />)

    expect(markup).toContain('900 mm')
    expect(markup).toContain('边部轻微压痕')
    expect(markup).not.toContain('φ1000')
    expect(markup).not.toContain('芯 3')
    expect(markup).not.toContain('加工方式')
    expect(markup).not.toContain('工艺摘要')
    expect(markup).not.toContain('出库临时备注')
  })
})

function detail(): DeliveryDetailVO {
  return {
    order: {
      uuid: 'delivery-1',
      deliveryNo: 'CK001',
      customerUuid: 'customer-1',
      customerName: '测试客户',
      deliveryDate: '2026-07-15',
      totalCount: 1,
      totalWeight: 950,
      settleBlockAction: 1,
      deliveryStatus: 2,
    },
    details: [{
      uuid: 'detail-1',
      deliveryUuid: 'delivery-1',
      finishUuid: 'finish-1',
      orderUuid: 'order-1',
      orderNo: 'JG001',
      finishRollNo: 'A000001',
      paperName: '白卡',
      gramWeight: 300,
      finishWidth: 900,
      finishDiameter: 1000,
      finishCoreDiameter: 3,
      actualWeight: 950,
      outWeight: 950,
      processModeText: '标准加工',
      processSummary: '锯纸 6 刀',
      originalSummary: '母卷1 / 白卡 / 300 g / 2520 mm',
      remark: '出库临时备注',
      actualRemark: '边部轻微压痕',
    }],
  }
}
