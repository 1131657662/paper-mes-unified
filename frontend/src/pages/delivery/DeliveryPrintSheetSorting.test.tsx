import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { DeliveryDetail, DeliveryDetailVO } from '../../types/delivery'
import { buildDeliveryPrintProjection } from './deliveryPrintProjection'
import DeliveryPrintSheet from './DeliveryPrintSheet'

describe('DeliveryPrintSheet sorting', () => {
  it('renumbers rows after applying the active sort chain', () => {
    const detail = delivery([row('d1', 'A000010'), row('d2', 'A000002')])
    const projection = buildDeliveryPrintProjection({
      detail,
      variant: 'physical',
      sortChains: {
        physical: [{ field: 'finishRollNo', direction: 'asc' }], customer: [], trace: [],
      },
    })
    if (projection.status !== 'ready') throw new Error(projection.message)

    const markup = renderToStaticMarkup(<DeliveryPrintSheet detail={detail} projection={projection} />)

    expect(markup).toContain('<tr><td>1</td><td>JG001</td><td>A000002</td>')
    expect(markup).toContain('<tr><td>2</td><td>JG001</td><td>A000010</td>')
    expect(markup).toContain('合计：2 卷')
  })
})

function delivery(details: DeliveryDetail[]): DeliveryDetailVO {
  return {
    order: {
      uuid: 'delivery', deliveryNo: 'CK001', customerUuid: 'customer', customerName: '客户',
      deliveryDate: '2026-08-06', totalCount: details.length, totalWeight: 200,
      settleBlockAction: 1, deliveryStatus: 1,
    },
    details,
  }
}

function row(uuid: string, finishRollNo: string): DeliveryDetail {
  return {
    uuid, finishRollNo, deliveryUuid: 'delivery', finishUuid: `finish-${uuid}`, orderUuid: 'order',
    orderNo: 'JG001', paperName: '白卡纸', gramWeight: 100, finishWidth: 900, outWeight: 100,
  }
}
