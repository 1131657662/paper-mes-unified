import { downloadBlob, type DownloadResult } from './downloadFile'

export interface CsvColumn<T> {
  header: string
  value: (row: T) => string | number | null | undefined
}

export function exportRowsToCsv<T>({
  columns,
  filename,
  rows,
}: {
  columns: CsvColumn<T>[]
  filename: string
  rows: T[]
}): DownloadResult {
  const header = columns.map((column) => csvCell(column.header)).join(',')
  const body = rows.map((row) => columns.map((column) => csvCell(column.value(row))).join(','))
  const content = `\uFEFF${[header, ...body].join('\n')}`
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8' })
  downloadBlob(blob, filename)
  return { filename, size: blob.size }
}

export function datedCsvFilename(prefix: string, date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${prefix}_${year}${month}${day}.csv`
}

function csvCell(value: string | number | null | undefined) {
  const text = String(value ?? '')
  return `"${text.replace(/"/g, '""')}"`
}
