import { Button, Tag } from 'antd'
import { BarChartOutlined, ReloadOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import type { DashboardMetrics, DashboardStatus, DashboardTodo } from '../../../types/dashboard'
import { formatMoney, formatTonFromKg } from '../../report/utils/reportFormatters'

interface Props {
  loading: boolean
  metrics?: DashboardMetrics
  onOpenReports: () => void
  onRefresh: () => void
  statusQueue: DashboardStatus[]
  todos: DashboardTodo[]
  userName: string
}

export default function DashboardHeader({ loading, metrics, onOpenReports, onRefresh, statusQueue, todos, userName }: Props) {
  return (
    <header className="dashboard-header">
      <div className="dashboard-header__main">
        <Tag color="blue">首页</Tag>
        <h1>{greeting()}，{userName}</h1>
        <p>{dayjs().format('YYYY年M月D日')} · 生产、出库、结算和库存的闭环数据看板</p>
      </div>
      <div className="dashboard-header__facts">
        <Fact label="待处理" value={`${sumTodoCount(todos)} 项`} />
        <Fact label="未闭环" value={`${sumQueueCount(statusQueue)} 单`} />
        <Fact label="本月原卷" value={formatTonFromKg(metrics?.monthOriginalWeight)} />
        <Fact label="已结算未收" value={formatMoney(metrics?.receivableAmount)} />
      </div>
      <div className="dashboard-header__actions">
        <Button icon={<ReloadOutlined />} loading={loading} onClick={onRefresh}>刷新</Button>
        <Button icon={<BarChartOutlined />} onClick={onOpenReports} type="primary">统计报表</Button>
      </div>
    </header>
  )
}

function Fact({ label, value }: FactProps) {
  return (
    <div className="dashboard-header__fact">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function greeting() {
  const hour = dayjs().hour()
  if (hour < 6) return '夜班辛苦'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

function sumQueueCount(items: DashboardStatus[]) {
  return items.reduce((total, item) => total + Number(item.orderCount ?? 0), 0)
}

function sumTodoCount(items: DashboardTodo[]) {
  return items.reduce((total, item) => total + Number(item.count ?? 0), 0)
}

interface FactProps {
  label: string
  value: string
}
