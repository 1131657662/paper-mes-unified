import type { ReactNode } from 'react'

interface Props {
  action?: ReactNode
  extra?: ReactNode
  subtitle?: string
  title: string
}

export default function DashboardPanelHead({ action, extra, subtitle, title }: Props) {
  return (
    <div className="dashboard-panel__head">
      <div>
        <h2>{title}</h2>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {extra ?? action}
    </div>
  )
}
