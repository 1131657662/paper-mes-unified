type ColumnWidths = Record<string, number>

export const resizableColumnMinWidth = 60
export const resizableColumnMaxWidth = 520

export function loadResizableTableWidths(storageKey: string): ColumnWidths {
  const raw = localStorage.getItem(widthStorageKey(storageKey))
  if (!raw) return {}
  try {
    return parseColumnWidths(JSON.parse(raw))
  } catch {
    return {}
  }
}

export function saveResizableTableWidths(storageKey: string, widths: ColumnWidths) {
  localStorage.setItem(widthStorageKey(storageKey), JSON.stringify(widths))
}

export function resetResizableTableWidths(storageKey: string) {
  localStorage.removeItem(widthStorageKey(storageKey))
  window.dispatchEvent(new CustomEvent('resizable-table-reset', { detail: { storageKey } }))
}

function widthStorageKey(storageKey: string) {
  return `table_cols_${storageKey}`
}
