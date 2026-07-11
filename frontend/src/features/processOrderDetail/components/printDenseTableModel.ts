import type {
  PrintRollBlock,
  PrintRouteOutput,
  PrintRouteStage,
} from './printPreviewModel'

export type DenseRow = DenseGroupRow | DenseItemRow

export interface DenseGroupRow {
  kind: 'group'
  key: string
  processTitle: string
  processDetail: string
}

export interface DenseItemRow {
  kind: 'item'
  key: string
  block: PrintRollBlock
  output?: PrintRouteOutput
  blockRowSpan?: number
}

interface DenseEntry {
  key: string
  block: PrintRollBlock
  outputs: Array<PrintRouteOutput | undefined>
  processKey: string
  processTitle: string
  processDetail: string
  widthSort: number
  titleSort: number
  sourceIndex: number
}

export function buildDenseRows(blocks: PrintRollBlock[]): DenseRow[] {
  const entries = sortedEntries(blocks)
  return groupedRows(entries)
}

function sortedEntries(blocks: PrintRollBlock[]): DenseEntry[] {
  const entries = buildEntries(blocks)
  const groupOrder = processGroupOrder(entries)
  return entries.sort((a, b) => {
    return compareNumber(groupOrder.get(a.processKey), groupOrder.get(b.processKey))
      || compareNumber(a.widthSort, b.widthSort)
      || compareNumber(a.titleSort, b.titleSort)
      || compareNumber(a.sourceIndex, b.sourceIndex)
  })
}

function buildEntries(blocks: PrintRollBlock[]): DenseEntry[] {
  return blocks.flatMap((block, blockIndex) => {
    return stagesForBlock(block).map((stage, stageIndex) => {
      const process = processInfo(stage)
      return {
        key: [block.key, stage?.key ?? `stage-${stageIndex}`].join('__'),
        block,
        outputs: outputsForStage(stage),
        ...process,
        widthSort: sourceWidth(block),
        titleSort: titleNumber(block.title),
        sourceIndex: blockIndex,
      }
    })
  })
}

function groupedRows(entries: DenseEntry[]): DenseRow[] {
  const rows: DenseRow[] = []
  let previousProcessKey = ''
  for (const entry of entries) {
    if (entry.processKey !== previousProcessKey) {
      rows.push(groupRow(entry))
      previousProcessKey = entry.processKey
    }
    rows.push(...entryRows(entry))
  }
  return rows
}

function groupRow(entry: DenseEntry): DenseGroupRow {
  return {
    kind: 'group',
    key: `process__${entry.processKey}`,
    processTitle: entry.processTitle,
    processDetail: entry.processDetail,
  }
}

function entryRows(entry: DenseEntry): DenseItemRow[] {
  return entry.outputs.map((output, index) => ({
    kind: 'item',
    key: [entry.key, output?.key ?? `output-${index}`].join('__'),
    block: entry.block,
    output,
    blockRowSpan: index === 0 ? entry.outputs.length : undefined,
  }))
}

function processGroupOrder(entries: DenseEntry[]): Map<string, number> {
  const order = new Map<string, number>()
  for (const entry of entries) {
    if (!order.has(entry.processKey)) order.set(entry.processKey, order.size)
  }
  return order
}

function processInfo(stage?: PrintRouteStage): {
  processKey: string
  processTitle: string
  processDetail: string
} {
  if (!stage) {
    return {
      processKey: 'no-stage',
      processTitle: '未配置工序',
      processDetail: '未配置加工路线',
    }
  }
  const detail = normalizedRequirement(stage.requirement)
  return {
    processKey: `${stage.title}__${detail}`,
    processTitle: stage.title,
    processDetail: detail,
  }
}

function normalizedRequirement(text: string): string {
  return text
    .replace(/，来源重量\s*[\d,.]+t/g, '')
    .replace(/\s*\/\s*[\d,.]+\s*kg/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function stagesForBlock(block: PrintRollBlock): Array<PrintRouteStage | undefined> {
  return block.routeStages.length ? block.routeStages : [undefined]
}

function outputsForStage(stage?: PrintRouteStage): Array<PrintRouteOutput | undefined> {
  return stage?.outputs.length ? stage.outputs : [undefined]
}

function sourceWidth(block: PrintRollBlock): number {
  const value = sourceValue(block, '克重/门幅')
  const match = /(\d+(?:\.\d+)?)mm/.exec(value)
  return match ? Number(match[1]) : Number.MAX_SAFE_INTEGER
}

function titleNumber(title: string): number {
  const match = /(\d+)/.exec(title)
  return match ? Number(match[1]) : Number.MAX_SAFE_INTEGER
}

function sourceValue(block: PrintRollBlock, label: string): string {
  return block.sourceItems.find((item) => item.label === label)?.value ?? '-'
}

function compareNumber(a?: number, b?: number): number {
  return (a ?? 0) - (b ?? 0)
}
