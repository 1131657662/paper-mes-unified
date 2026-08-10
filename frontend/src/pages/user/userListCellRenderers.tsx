import type { ReactNode } from 'react'
import TooltipText from '../../components/biz/TooltipText'
import { formatDateTime } from '../../utils/dateTime'

export function userTextCell(value?: ReactNode): ReactNode {
  return <TooltipText value={value} />
}

export function userDateCell(value?: string): ReactNode {
  return <span>{formatDateTime(value)}</span>
}
