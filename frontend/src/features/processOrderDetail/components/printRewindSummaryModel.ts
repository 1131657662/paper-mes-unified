import type { PrintRouteOutput } from './printPreviewModel'

interface OutputGroup {
  count: number
  spec: string
  totalWeight: number
  width?: number
}

export function buildRewindGroupDetail(outputs: PrintRouteOutput[]): string {
  const products = outputs.filter((output) => output.status !== 'trim')
  const trims = outputs.filter((output) => output.status === 'trim')
  return `产出规格：${groupedProductText(products) || '按产物表执行'}；切边：${groupedTrimText(trims)}。`
}

function groupedProductText(outputs: PrintRouteOutput[]): string {
  return groupedOutputs(outputs, (output) => output.spec || `${output.width ?? 'unknown'}`)
    .sort(compareOutputGroup)
    .map((item) => `${item.spec} ×${item.count} 件`)
    .join(' + ')
}

function groupedTrimText(outputs: PrintRouteOutput[]): string {
  if (!outputs.length) return '无'
  return groupedOutputs(outputs, (output) => output.spec)
    .map((item) => trimGroupText(item))
    .join(' + ')
}

function groupedOutputs(
  outputs: PrintRouteOutput[],
  groupKey: (output: PrintRouteOutput) => string,
): OutputGroup[] {
  const groups = new Map<string, OutputGroup>()
  for (const output of outputs) {
    const key = groupKey(output)
    const existing = groups.get(key)
    groups.set(key, {
      spec: output.spec || '未填写规格',
      totalWeight: (existing?.totalWeight ?? 0) + (output.weightValue ?? 0),
      width: output.width,
      count: (existing?.count ?? 0) + 1,
    })
  }
  return Array.from(groups.values())
}

function trimGroupText(item: OutputGroup): string {
  const weight = item.totalWeight > 0 ? `，总重 ${formatKg(item.totalWeight)}` : ''
  return `${item.count} 条，规格 ${item.spec}${weight}`
}

function formatKg(value: number): string {
  return `${Number(value.toFixed(3)).toLocaleString('zh-CN')} kg`
}

function compareOutputGroup(left: OutputGroup, right: OutputGroup): number {
  return (left.width ?? 0) - (right.width ?? 0) || left.spec.localeCompare(right.spec)
}
