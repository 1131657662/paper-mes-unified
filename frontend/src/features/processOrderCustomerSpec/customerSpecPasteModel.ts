import type { CustomerSpecDraft } from './customerSpecDraftModel'

export interface PastedCustomerSpec {
  finishRollNo?: string
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  displayWeight?: number
}

export function applyPastedCustomerSpecs(rows: CustomerSpecDraft[], text: string) {
  const pasted = parsePastedCustomerSpecs(text)
  const byRollNo = new Map(pasted.filter((item) => item.finishRollNo).map((item) => [item.finishRollNo, item]))
  let sequentialIndex = 0
  return rows.map((row) => {
    const item = row.finishRollNo ? byRollNo.get(row.finishRollNo) : undefined
    const fallback = byRollNo.size === 0 ? pasted[sequentialIndex++] : undefined
    return applyPastedRow(row, item ?? fallback)
  })
}

export function parsePastedCustomerSpecs(text: string): PastedCustomerSpec[] {
  return text.trim().split(/\r?\n/).map(splitLine).filter(isDataLine).map(toPastedSpec)
}

function splitLine(line: string) {
  return line.trim().split(/\t|,/).map((cell) => cell.trim())
}

function isDataLine(cells: string[]) {
  return cells.some(Boolean) && !cells.some((cell) => /卷号|品名|克重|门幅|重量/.test(cell))
}

function toPastedSpec(cells: string[]): PastedCustomerSpec {
  const withRollNo = cells.length >= 5
  const offset = withRollNo ? 1 : 0
  return {
    finishRollNo: withRollNo ? cells[0] : undefined,
    paperName: cells[offset] || undefined,
    gramWeight: positiveNumber(cells[offset + 1]),
    finishWidth: positiveNumber(cells[offset + 2]),
    displayWeight: positiveNumber(cells[offset + 3]),
  }
}

function applyPastedRow(row: CustomerSpecDraft, item?: PastedCustomerSpec): CustomerSpecDraft {
  if (!item) return row
  return {
    ...row,
    customerPaperName: item.paperName ?? row.customerPaperName,
    customerGramWeight: item.gramWeight ?? row.customerGramWeight,
    customerFinishWidth: item.finishWidth ?? row.customerFinishWidth,
    customerDisplayWeight: item.displayWeight ?? row.customerDisplayWeight,
    calculationMode: item.displayWeight == null ? row.calculationMode : 'MANUAL',
  }
}

function positiveNumber(value?: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined
}
