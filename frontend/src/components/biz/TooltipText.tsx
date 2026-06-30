import type { ReactNode } from 'react'
import MesTooltip from './MesTooltip'

interface TooltipTextProps {
  value?: ReactNode
  title?: ReactNode
  className?: string
}

export default function TooltipText({ className, title, value }: TooltipTextProps) {
  const content = normalizeContent(value)
  const tooltipTitle = title ?? content
  const tooltip = tooltipTitle === '' ? undefined : tooltipTitle
  const textClassName = className ? `mes-tooltip-text ${className}` : 'mes-tooltip-text'

  if (content === '') {
    return <span className={textClassName}>-</span>
  }

  return (
    <MesTooltip title={tooltip}>
      <span className={textClassName}>{content}</span>
    </MesTooltip>
  )
}

function normalizeContent(value?: ReactNode) {
  if (value == null || value === '') return ''
  return value
}
