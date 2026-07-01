import { ArrowRightOutlined, CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { Empty } from 'antd'
import type { DashboardTodo } from '../../../types/dashboard'
import { formatMoney, formatTonFromKg } from '../../report/utils/reportFormatters'
import DashboardPanelHead from './DashboardPanelHead'

interface Props {
  items: DashboardTodo[]
  onOpen: (path?: string) => void
}

export default function DashboardTodoList({ items, onOpen }: Props) {
  return (
    <section className="dashboard-panel dashboard-todos">
      <DashboardPanelHead title="待办提醒" subtitle="直接关联下发、回录、出库、收款这些会阻塞闭环的事项。" />
      <div className="dashboard-todos__list">
        {items.length === 0 ? (
          <Empty description="暂无待办" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : items.map((item) => (
          <button className={`dashboard-todo dashboard-todo--${item.level ?? 'info'}`} key={item.key} onClick={() => onOpen(item.targetPath)}>
            <span className="dashboard-todo__icon">{todoIcon(item.level)}</span>
            <span className="dashboard-todo__body">
              <strong>{item.title}</strong>
              <em>{item.description}</em>
            </span>
            <span className="dashboard-todo__value">
              <b>{item.count ?? 0}</b>
              <small>{valueText(item)}</small>
            </span>
            <ArrowRightOutlined className="dashboard-todo__arrow" />
          </button>
        ))}
      </div>
    </section>
  )
}

function todoIcon(level?: string) {
  return level === 'success' ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />
}

function valueText(item: DashboardTodo) {
  if (item.key === 'stock') return formatTonFromKg(item.amount)
  if (item.key === 'receive') return formatMoney(item.amount)
  return '项'
}
