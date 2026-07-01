import type { ReactNode } from 'react'
import { TableToolbarPortal } from './TableToolbarPortal'

export function renderTableToolbarPortal(_: unknown, defaultDom: ReactNode[]) {
  return [<TableToolbarPortal key="table-toolbar-options">{defaultDom}</TableToolbarPortal>]
}
