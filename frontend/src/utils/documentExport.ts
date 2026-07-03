export interface DocumentExportTarget {
  uuid: string
  documentNo?: string
}

export type DocumentExportInput = string | DocumentExportTarget

export function normalizeDocumentExportInput(input: DocumentExportInput): DocumentExportTarget {
  return typeof input === 'string' ? { uuid: input } : input
}

export function readableExportFilename(prefix: string, documentNo?: string, extension = 'xlsx') {
  const suffix = safeFilenamePart(documentNo) || timestamp()
  return `${prefix}_${suffix}.${extension}`
}

function safeFilenamePart(value?: string) {
  return value?.trim().replace(/[\\/:*?"<>|]/g, '-')
}

function timestamp(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}${month}${day}_${hour}${minute}${second}`
}
