export const SETTLE_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '待结算', color: 'warning' },
  2: { text: '部分收款', color: 'processing' },
  3: { text: '全部结清', color: 'success' },
}

export const SETTLE_TYPE: Record<number, string> = {
  1: '按单生成',
  2: '按月批量',
}

export const PAY_METHOD: Record<number, string> = {
  1: '现金',
  2: '转账',
  3: '微信',
  4: '支付宝',
}

export const INVOICE_TYPE: Record<number, string> = {
  1: '开票',
  2: '不开票',
}
