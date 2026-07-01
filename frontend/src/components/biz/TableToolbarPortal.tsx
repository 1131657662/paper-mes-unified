import { createContext, useContext, type ReactNode } from 'react'
import { createPortal } from 'react-dom'

const TableToolbarHostContext = createContext<HTMLElement | null>(null)

export function TableToolbarHostProvider({
  children,
  host,
}: {
  children: ReactNode
  host: HTMLElement | null
}) {
  return (
    <TableToolbarHostContext.Provider value={host}>
      {children}
    </TableToolbarHostContext.Provider>
  )
}

export function TableToolbarPortal({ children }: { children: ReactNode }) {
  const host = useContext(TableToolbarHostContext)
  if (!host) return <>{children}</>
  return createPortal(<div className="mes-table-options-portal">{children}</div>, host)
}
