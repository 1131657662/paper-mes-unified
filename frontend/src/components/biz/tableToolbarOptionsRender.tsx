import DensityIcon from '@ant-design/pro-table/es/components/ToolBar/DensityIcon'
import type { ReactNode } from 'react'

export function renderCompatibleTableOptions(_: unknown, defaultDom: ReactNode[]): ReactNode[] {
  return [<DensityIcon key="density" />, ...defaultDom]
}
