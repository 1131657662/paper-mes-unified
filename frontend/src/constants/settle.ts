export const SETTLE_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '待收款', color: 'warning' },
  2: { text: '部分收款', color: 'processing' },
  3: { text: '已结清', color: 'success' },
  4: { text: '已作废', color: 'default' },
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

export const RECEIVE_TYPE: Record<number, { text: string; color: string }> = {
  1: { text: '普通收款', color: 'blue' },
  2: { text: '废纸抵扣', color: 'orange' },
  3: { text: '混合结清', color: 'purple' },
  4: { text: '优惠核销', color: 'cyan' },
}

export const INVOICE_TYPE: Record<number, string> = {
  1: '开票',
  2: '不开票',
}
