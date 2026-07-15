import { Alert, Tag } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { ORDER_STATUS } from '../../../constants/processOrder'
import { buildExecutionSummary } from '../orderExecutionUtils'
import ExecutionActions, { type ExecutionActionHandlers, type ExecutionCapabilities, type ExecutionLoading } from './OrderExecutionActions'
import OrderMetricStrip from './OrderMetricStrip'
import OrderStatusProgress from './OrderStatusProgress'

interface Props {
  detail?: ProcessOrderDetailVO
  actions: ExecutionActionHandlers
  capabilities: ExecutionCapabilities
  loading?: ExecutionLoading
}

export default function OrderExecutionPanel({ detail, actions, capabilities, loading = {} }: Props) {
  const order = detail?.order
  const status = order?.orderStatus ?? 0
  const hasPrinted = (order?.printCount ?? 0) > 0 || order?.printStatus === 1
  const summary = buildExecutionSummary(detail)
  const statusText = ORDER_STATUS[status]?.text ?? '未知状态'

  return (
    <section className="order-detail-section order-execution">
      <div className="order-detail-section__header">
        <div>
          <h2 className="order-detail-section__title">生产执行</h2>
          <div className="order-execution__hint">{summary.statusHint}</div>
        </div>
        <Tag color={ORDER_STATUS[status]?.color}>{statusText}</Tag>
      </div>
      <div className="order-detail-section__body order-execution__body">
        <OrderStatusProgress order={order} />
        <div className="order-execution__command-row">
          <div className="order-execution__actions">
            <ExecutionActions
              status={status}
              hasPrinted={hasPrinted}
              actions={actions}
              capabilities={capabilities}
              loading={loading}
            />
          </div>
          <ExecutionFacts summary={summary} />
        </div>
        <OrderMetricStrip detail={detail} />
      </div>
    </section>
  )
}

function ExecutionFacts({ summary }: { summary: ReturnType<typeof buildExecutionSummary> }) {
  return (
    <div className="order-execution__facts">
      <span>正式号 {summary.officialCount} 个</span>
      <span>备用号 {summary.spareCount} 个</span>
      {summary.printableWarnings.length > 0 && (
        <Alert
          type="warning"
          showIcon
          message={summary.printableWarnings.join('；')}
          className="order-execution__warning"
        />
      )}
    </div>
  )
}
