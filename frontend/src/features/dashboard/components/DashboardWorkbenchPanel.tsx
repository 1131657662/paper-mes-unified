import type { ReactNode } from 'react'
import {
  AccountBookOutlined,
  BarChartOutlined,
  ContainerOutlined,
  ExportOutlined,
  FieldTimeOutlined,
  ProfileOutlined,
} from '@ant-design/icons'
import type { DashboardStatus, DashboardTodo } from '../../../types/dashboard'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  onNavigate: (path: string) => void
  statusQueue: DashboardStatus[]
  todos: DashboardTodo[]
}

const shortcuts: Shortcut[] = [
  { icon: <ContainerOutlined />, label: '加工单', path: '/process-orders' },
  { icon: <FieldTimeOutlined />, label: '待回录', path: '/process-orders?orderStatus=3' },
  { icon: <ExportOutlined />, label: '出库', path: '/delivery-orders' },
  { icon: <AccountBookOutlined />, label: '结算', path: '/settle-orders' },
  { icon: <BarChartOutlined />, label: '报表', path: '/reports' },
  { icon: <ProfileOutlined />, label: '档案', path: '/customers' },
]

export default function DashboardWorkbenchPanel({ onNavigate, statusQueue, todos }: Props) {
  const totalQueue = statusQueue.reduce((sum, item) => sum + Number(item.orderCount ?? 0), 0)

  return (
    <section className="dashboard-panel dashboard-workbench-panel">
      <DashboardPanelHead title="生产工作台" subtitle="把日常入口、未闭环结构和关键待办放在一个区域。" />
      <div className="dashboard-shortcuts">
        {shortcuts.map((item) => (
          <button className="dashboard-shortcut" key={item.path} onClick={() => onNavigate(item.path)}>
            <span>{item.icon}</span>
            <strong>{item.label}</strong>
          </button>
        ))}
      </div>
      <div className="dashboard-workbench-panel__section">
        <div className="dashboard-workbench-panel__title">未闭环结构</div>
        <div className="dashboard-progress-list">
          {normalizedQueue(statusQueue).map((item) => (
            <ProgressRow
              key={item.status}
              label={item.statusName ?? '未知'}
              percent={ratio(item.orderCount, totalQueue)}
              tone={queueTone(item.status)}
              value={`${item.orderCount ?? 0} 单`}
            />
          ))}
        </div>
      </div>
      <div className="dashboard-workbench-panel__section">
        <div className="dashboard-workbench-panel__title">今日关注</div>
        <div className="dashboard-focus-list">
          {todos.slice(0, 3).map((item) => (
            <button className="dashboard-focus-item" key={item.key} onClick={() => onNavigate(item.targetPath || '/process-orders')}>
              <span>{item.title}</span>
              <strong>{item.count ?? 0}</strong>
            </button>
          ))}
        </div>
      </div>
    </section>
  )
}

function ProgressRow({ label, percent, tone, value }: ProgressRowProps) {
  return (
    <div className="dashboard-progress-row">
      <div className="dashboard-progress-row__meta">
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
      <div className="dashboard-progress-row__track">
        <i className={`dashboard-progress-row__bar dashboard-progress-row__bar--${tone}`} style={{ width: `${percent}%` }} />
      </div>
    </div>
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

function ratio(value?: number, total = 0) {
  if (total <= 0) return 0
  return Math.max(4, (Number(value ?? 0) / total) * 100)
}

function queueTone(status?: number) {
  if (status === 1) return 'blue'
  if (status === 2) return 'orange'
  if (status === 3) return 'red'
  return 'gray'
}

interface Shortcut {
  icon: ReactNode
  label: string
  path: string
}

interface ProgressRowProps {
  label: string
  percent: number
  tone: string
  value: string
}
