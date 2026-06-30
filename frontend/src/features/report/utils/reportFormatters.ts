export function formatMoney(value?: number) {
  return `¥${formatNumber(value, 2)}`
}

export function formatNumber(value?: number, digits = 0) {
  return Number(value ?? 0).toLocaleString('zh-CN', {
    maximumFractionDigits: digits,
    minimumFractionDigits: digits,
  })
}

export function formatKg(value?: number) {
  return `${formatNumber(value, 3)}kg`
}

export function formatPercent(value?: number) {
  return `${formatNumber(value, 2)}%`
}

export function formatTon(value?: number) {
  return `${formatNumber(value, 3)}t`
}

export function formatTonFromKg(value?: number) {
  return formatTon(Number(value ?? 0) / 1000)
}
