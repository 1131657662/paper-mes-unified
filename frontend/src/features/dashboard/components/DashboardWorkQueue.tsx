import type { ReactNode } from 'react'
import {
  ClockCircleOutlined,
  EditOutlined,
  FieldTimeOutlined,
  PrinterOutlined,
} from '@ant-design/icons'
import type { DashboardStatus } from '../../../types/dashboard'
import { formatKg } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  items: DashboardStatus[]
  onOpenOrders: (status: number) => void
}

const statusIcons: Record<number, ReactNode> = {
  0: <EditOutlined />,
  1: <PrinterOutlined />,
  2: <ClockCircleOutlined />,
  3: <FieldTimeOutlined />,
}

export default function DashboardWorkQueue({ items, onOpenOrders }: Props) {
  const displayItems = normalizedQueue(items)
  const max = Math.max(...displayItems.map((item) => item.orderCount ?? 0), 1)

  return (
    <section className="dashboard-panel dashboard-work-queue">
      <DashboardPanelHead title="生产队列" subtitle="当前未闭环加工单状态，重点看是否卡在下发或回录。" />
      <div className="dashboard-work-queue__grid">
        {displayItems.map((item) => (
          <button className="dashboard-work-queue__item" key={item.status} onClick={() => onOpenOrders(item.status ?? 0)}>
            <div className="dashboard-work-queue__icon">{statusIcons[item.status ?? 0]}</div>
            <div className="dashboard-work-queue__body">
              <span>{item.statusName ?? '未知状态'}</span>
              <strong>{item.orderCount ?? 0} 单</strong>
              <em>{formatKg(item.originalWeight)} 原卷</em>
              <i style={{ width: `${((item.orderCount ?? 0) / max) * 100}%` }} />
            </div>
          </button>
        ))}
      </div>
    </section>
  )
}

function normalizedQueue(items: DashboardStatus[]) {
  const metas = [
    { status: 0, statusName: '草稿' },
    { status: 1, statusName: '待下发' },
    { status: 2, statusName: '加工中' },
    { status: 3, statusName: '待回录' },
  ]
  return metas.map((meta) => ({ ...meta, ...items.find((item) => item.status === meta.status) }))
}
