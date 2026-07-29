import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { DeliveryCustomerRevisionPreview } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetailVO } from '../../types/delivery'
import DeliveryPrintSheet from './DeliveryPrintSheet'

describe('司机单据预览', () => {
  it('区分货主和货主告知的客户', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail()} />)

    expect(markup).toContain('<dt>货主</dt><dd>测试客户</dd>')
    expect(markup).toContain('<dt>客户</dt><dd>永丰包装</dd>')
  })

  it('未填写客户时保持空白且不以货主兜底', () => {
    const value = detail()
    value.order.receiverCustomerName = undefined

    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={value} />)

    expect(markup).toContain('<dt>客户</dt><dd></dd>')
    expect(markup.match(/测试客户/g)).toHaveLength(1)
  })

  it('使用结构化运输信息和合并规格', () => {
    const markup = renderToStaticMarkup(
      <DeliveryPrintSheet detail={detail()} customerSpecs={customerSpecs()} variant="customer" />,
    )

    expect(markup).toContain('成品仓')
    expect(markup).toContain('<dt>车牌号</dt><dd>浙A12345</dd>')
    expect(markup).toContain('<dt>柜号</dt><dd>GX-08</dd>')
    expect(markup).toContain('75 g × 1000 mm')
  })

  it('表头不显示内部状态或提货人', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail()} />)

    expect(markup).not.toContain('<dt>单据状态</dt>')
    expect(markup).not.toContain('<dt>提货人</dt>')
    expect(markup).not.toContain('李师傅')
  })

  it('合计仅显示卷数和吨位', () => {
    const markup = renderToStaticMarkup(
      <DeliveryPrintSheet detail={detail()} customerSpecs={customerSpecs()} variant="customer" />,
    )

    expect(markup).toContain('合计：1 卷</td><td>1.05 t</td>')
  })

  it('显示签字区和三段式页脚', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail()} />)

    expect(markup).toContain('出库备注')
    expect(markup).toContain('仓库复核')
    expect(markup).toContain('司机签字')
    expect(markup).toContain('打印时间：')
  })

  it('提货人和仓库复核保留为空白手签位', () => {
    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail()} />)

    expect(markup).toContain('<span>提货人</span><strong></strong>')
    expect(markup).toContain('<span>仓库复核</span><strong></strong>')
    expect(markup).not.toContain('仓管员')
  })

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

  it('客户更正版显示版本标题和客户规格', () => {
    const markup = renderToStaticMarkup(
      <DeliveryPrintSheet detail={detail()} customerSpecs={customerSpecs()} variant="customer" />,
    )

    expect(markup).toContain('出库单（客户更正版 V2）')
    expect(markup).toContain('食品卡')
    expect(markup).toContain('75 g')
    expect(markup).toContain('1000 mm')
    expect(markup).toContain('客户改单')
    expect(markup).not.toContain('实物：白卡')
  })

  it('追溯打印同时显示客户值和不可变的实物值', () => {
    const markup = renderToStaticMarkup(
      <DeliveryPrintSheet detail={detail()} customerSpecs={customerSpecs()} variant="trace" />,
    )

    expect(markup).toContain('出库单（追溯对照）')
    expect(markup).toContain('<strong>食品卡</strong><span>实物：白卡</span>')
    expect(markup).toContain('<strong>75 g × 1000 mm</strong><span>实物：300 g × 900 mm</span>')
    expect(markup).toContain('<strong>1050 kg</strong><span>实物：950 kg</span>')
  })
})

function customerSpecs(): DeliveryCustomerRevisionPreview {
  return {
    deliveryUuid: 'delivery-1', deliveryNo: 'CK001', deliveryVersion: 3, deliveryStatus: 2,
    currentRevisionNo: 2, currentRevisionKind: 'USER_REVISION', nextRevisionNo: 3,
    itemCount: 1, validItemCount: 1,
    physicalTotalWeight: 950, customerTotalWeight: 1050, differenceWeight: 100, hasErrors: false,
    items: [{
      deliveryDetailUuid: 'detail-1', detailVersion: 1, finishUuid: 'finish-1', finishRollNo: 'A000001',
      physicalPaperName: '白卡', physicalGramWeight: 300, physicalFinishWidth: 900,
      physicalDeliveryWeight: 950, customerPaperName: '食品卡', customerGramWeight: 75,
      customerFinishWidth: 1000, customerDisplayWeight: 1050, customerRemark: '客户改单',
      calculationMode: 'FORMULA', valueSource: 'DELIVERY_REVISION', specificationChanged: true,
      weightChanged: true, valid: true,
    }],
  }
}

function detail(): DeliveryDetailVO {
  return {
    order: {
      uuid: 'delivery-1',
      deliveryNo: 'CK001',
      customerUuid: 'customer-1',
      customerName: '测试客户',
      receiverCustomerName: '永丰包装',
      warehouseName: '成品仓',
      deliveryDate: '2026-07-15',
      totalCount: 1,
      totalWeight: 950,
      pickerName: '李师傅',
      carNo: '浙A12345',
      containerNo: 'GX-08',
      signUser: '仓管员',
      signTime: '2026-07-15T15:30:00',
      remark: '装车前核对卷号',
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
