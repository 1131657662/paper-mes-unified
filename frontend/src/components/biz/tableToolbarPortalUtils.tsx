import type { ReactNode } from 'react'
import { TableToolbarPortal } from './TableToolbarPortal'
import { renderCompatibleTableOptions } from './tableToolbarOptionsRender'

export function renderTableToolbarPortal(_: unknown, defaultDom: ReactNode[]) {
  const options = renderCompatibleTableOptions(undefined, defaultDom)
  return [<TableToolbarPortal key="table-toolbar-options">{options}</TableToolbarPortal>]
}
