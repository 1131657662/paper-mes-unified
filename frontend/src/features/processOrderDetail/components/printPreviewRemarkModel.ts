import type { ProcessOrderDetailVO } from '../../../types/processOrder'

export interface PrintPreviewRemarks {
  workshopInstructions: string[]
  customerRemark?: string
}

export function printPreviewRemarks(detail: ProcessOrderDetailVO): PrintPreviewRemarks {
  return {
    workshopInstructions: (detail.workshopInstructions ?? [])
      .map((item) => item.text?.trim())
      .filter((item): item is string => Boolean(item)),
    customerRemark: distinctRemarks(detail.order.remark, detail.order.remarkLong).join('；') || undefined,
  }
}

function distinctRemarks(...values: Array<string | undefined>): string[] {
  return [...new Set(values
    .map((item) => item?.trim())
    .filter((item): item is string => Boolean(item)))]
}
