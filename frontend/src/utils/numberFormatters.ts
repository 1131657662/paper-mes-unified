export function formatNumber(value?: number | null, digits = 0): string {
  const numeric = Number(value ?? 0)
  return Number.isFinite(numeric) ? numeric.toFixed(digits) : Number(0).toFixed(digits)
}

export function formatOptionalNumber(value?: number | null, digits = 0): string {
  if (value == null) return '-'
  return formatNumber(value, digits)
}

export function formatTrimmedNumber(value?: number | null, digits = 0): string {
  if (value == null) return '-'
  if (digits <= 0) return formatNumber(value, 0)
  return formatNumber(value, digits).replace(/\.?0+$/, '')
}

export function formatMoney(value?: number | null): string {
  return `¥${formatNumber(value, 2)}`
}

export function formatOptionalMoney(value?: number | null): string {
  if (value == null) return '-'
  return formatMoney(value)
}

export function formatKg(value?: number | null): string {
  return `${formatNumber(value, 3)}kg`
}

export function formatOptionalKg(value?: number | null): string {
  if (value == null) return '-'
  return formatKg(value)
}

export function formatTon(value?: number | null): string {
  return `${formatNumber(value, 3)}t`
}

export function formatOptionalTon(value?: number | null): string {
  if (value == null) return '-'
  return formatTon(value)
}

export function formatTonFromKg(value?: number | null): string {
  return formatTon(Number(value ?? 0) / 1000)
}

export function formatOptionalTonFromKg(value?: number | null): string {
  if (value == null) return '-'
  return formatTonFromKg(value)
}

export function formatPercent(value?: number | null): string {
  return `${formatNumber(value, 2)}%`
}
