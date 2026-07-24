export function formatNumber(value?: number | null, digits = 0): string {
  const numeric = Number(value ?? 0)
  return Number.isFinite(numeric) ? numeric.toFixed(digits) : Number(0).toFixed(digits)
}

export function formatFixedNumberInput(value: string | number | undefined, digits = 0): string {
  if (value == null || value === '') return ''
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(Math.max(0, digits)) : String(value)
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

export function decimalPlaces(value?: number | null, maxDigits = 3): number {
  if (value == null || !Number.isFinite(Number(value))) return 0
  const text = Number(value).toFixed(maxDigits).replace(/\.?0+$/, '')
  const fraction = text.split('.')[1]
  return fraction?.length ?? 0
}

export function formatMoney(value?: number | null): string {
  return `¥${formatNumber(value, 2)}`
}

export function formatOptionalMoney(value?: number | null): string {
  if (value == null) return '-'
  return formatMoney(value)
}

export function formatKg(value?: number | null): string {
  return `${formatTrimmedNumber(value ?? 0, 3)} kg`
}

export function formatWholeKg(value?: number | null): string {
  return `${formatNumber(value, 0)} kg`
}

export function formatKgWithMaxDecimals(value?: number | null, maxDigits = 3): string {
  return `${formatTrimmedNumber(value ?? 0, Math.max(0, Math.min(maxDigits, 3)))} kg`
}

export function formatOptionalKg(value?: number | null): string {
  if (value == null) return '-'
  return formatKg(value)
}

export function formatTon(value?: number | null): string {
  return `${formatTrimmedNumber(value ?? 0, 3)} t`
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

export function formatUnit(value: number | null | undefined, unit: string): string {
  if (value == null) return '-'
  return `${value} ${unit}`
}

export function formatMm(value?: number | null): string {
  return formatUnit(value, 'mm')
}

export function formatGram(value?: number | null): string {
  return formatUnit(value, 'g')
}
