import { describe, expect, it } from 'vitest'
import type { DeliveryCustomerRevisionPreview } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetail, DeliveryDetailVO } from '../../types/delivery'
import type { DeliveryExportSortChains } from '../../types/deliverySort'
import { buildDeliveryPrintProjection } from './deliveryPrintProjection'

describe('buildDeliveryPrintProjection', () => {
  it('applies the physical multi-column chain to complete rows', () => {
    const value = delivery([detail('d1', 'A010', '白卡'), detail('d2', 'A002', '牛卡')])
    const sortChains = chains({ physical: [{ field: 'finishRollNo', direction: 'asc' }] })

    const result = buildDeliveryPrintProjection({ detail: value, variant: 'physical', sortChains })

    expect(result.status).toBe('ready')
    if (result.status === 'ready') expect(result.rows.map((row) => row.key)).toEqual(['d2', 'd1'])
  })

  it('uses the customer chain for customer documents', () => {
    const value = delivery([detail('d1', 'A001', '实物一'), detail('d2', 'A002', '实物二')])
    const customerSpecs = revision(value.details, ['B类', 'A类'])
    const sortChains = chains({ customer: [{ field: 'customerPaperName', direction: 'asc' }] })

    const result = buildDeliveryPrintProjection({
      detail: value, customerSpecs, variant: 'customer', sortChains,
    })

    expect(result.status).toBe('ready')
    if (result.status === 'ready') expect(result.rows.map((row) => row.key)).toEqual(['d2', 'd1'])
  })

  it('uses the trace chain independently from the customer chain', () => {
    const value = delivery([
      { ...detail('d1', 'A001', '实物一'), originalSummary: 'M-2' },
      { ...detail('d2', 'A002', '实物二'), originalSummary: 'M-10' },
    ])
    const sortChains = chains({
      customer: [{ field: 'finishRollNo', direction: 'asc' }],
      trace: [{ field: 'sourceMotherRoll', direction: 'desc' }],
    })

    const result = buildDeliveryPrintProjection({
      detail: value, customerSpecs: revision(value.details), variant: 'trace', sortChains,
    })

    expect(result.status).toBe('ready')
    if (result.status === 'ready') expect(result.rows.map((row) => row.key)).toEqual(['d2', 'd1'])
  })

  it('blocks customer printing when a physical detail has no customer row', () => {
    const value = delivery([detail('d1', 'A001', '实物一'), detail('d2', 'A002', '实物二')])
    const customerSpecs = revision(value.details.slice(0, 1))

    const result = buildDeliveryPrintProjection({
      detail: value, customerSpecs, variant: 'customer', sortChains: chains(),
    })

    expect(result).toMatchObject({ status: 'invalid' })
    if (result.status === 'invalid') expect(result.message).toContain('件数与出库明细不一致')
  })
})

function chains(values: Partial<DeliveryExportSortChains> = {}): DeliveryExportSortChains {
  return { physical: [], customer: [], trace: [], ...values }
}

function delivery(details: DeliveryDetail[]): DeliveryDetailVO {
  return {
    order: {
      uuid: 'delivery', deliveryNo: 'CK001', customerUuid: 'customer', customerName: '客户',
      deliveryDate: '2026-08-06', totalCount: details.length,
      totalWeight: details.reduce((sum, item) => sum + item.outWeight, 0),
      settleBlockAction: 1, deliveryStatus: 1,
    },
    details,
  }
}

function detail(uuid: string, finishRollNo: string, paperName: string): DeliveryDetail {
  return {
    uuid, deliveryUuid: 'delivery', finishUuid: `finish-${uuid}`, orderUuid: 'order',
    orderNo: 'JG001', finishRollNo, paperName, gramWeight: 100, finishWidth: 900, outWeight: 100,
  }
}

function revision(details: DeliveryDetail[], names = details.map((item) => item.paperName)):
DeliveryCustomerRevisionPreview {
  const items = details.map((item, index) => ({
    deliveryDetailUuid: item.uuid, detailVersion: 1, finishUuid: item.finishUuid,
    finishRollNo: item.finishRollNo, orderNo: item.orderNo, customerPaperName: names[index],
    customerGramWeight: item.gramWeight, customerFinishWidth: item.finishWidth,
    customerDisplayWeight: item.outWeight, calculationMode: 'KEEP' as const,
    valueSource: 'PHYSICAL' as const, specificationChanged: false, weightChanged: false, valid: true,
  }))
  return {
    deliveryUuid: 'delivery', deliveryNo: 'CK001', deliveryVersion: 1, deliveryStatus: 1,
    currentRevisionNo: 0, currentRevisionKind: 'SYSTEM_BASELINE', nextRevisionNo: 1,
    itemCount: items.length, validItemCount: items.length, physicalTotalWeight: items.length * 100,
    customerTotalWeight: items.length * 100, differenceWeight: 0, hasErrors: false, items,
  }
}
