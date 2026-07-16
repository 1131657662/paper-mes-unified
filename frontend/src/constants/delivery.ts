export const DELIVERY_STATUS: Record<number, { text: string; color: string }> = {
  1: { text: '待出库', color: 'warning' },
  2: { text: '已出库', color: 'success' },
  3: { text: '已作废', color: 'default' },
}

export const SOURCE_TYPE: Record<number, { text: string; color: string }> = {
  1: { text: '加工产出', color: 'blue' },
  2: { text: '直发原纸', color: 'green' },
}

export const SETTLE_BLOCK_ACTION: Record<number, string> = {
  0: '无拦截',
  1: '警告放行',
  2: '已拦截',
}
