import { Alert, Button, Space, Tag } from 'antd'
import {
  CalculatorOutlined,
  DiffOutlined,
  FileDoneOutlined,
  PrinterOutlined,
  RollbackOutlined,
  SendOutlined,
  StopOutlined,
} from '@ant-design/icons'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { ORDER_STATUS } from '../../../constants/processOrder'
import { buildExecutionSummary } from '../orderExecutionUtils'

interface ExecutionActions {
  onPrint: () => void
  onBackRecord: () => void
  onSnapshotDiff: () => void
  onManageRolls: () => void
  onEditDraft: () => void
  onChangeStatus: (targetStatus: number, title: string) => void
  onCalcFee: () => void
  onGoDelivery: () => void
  onGoSettle: () => void
  onVoidOrder: () => void
}

interface ExecutionLoading {
  changingStatus?: boolean
  calculatingFee?: boolean
  voidingOrder?: boolean
}

interface Props {
  detail?: ProcessOrderDetailVO
  actions: ExecutionActions
  loading?: ExecutionLoading
}

export default function OrderExecutionPanel({ detail, actions, loading = {} }: Props) {
  const order = detail?.order
  const status = order?.orderStatus ?? 0
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
        <PrimaryAction status={status} actions={actions} loading={loading} />
        <SecondaryActions status={status} actions={actions} loading={loading} />
        <ExecutionFacts summary={summary} />
      </div>
    </section>
  )
}

function PrimaryAction({
  status,
  actions,
  loading,
}: {
  status: number
  actions: ExecutionActions
  loading: ExecutionLoading
}) {
  if (status === 1) {
    return (
      <Button type="primary" icon={<PrinterOutlined />} onClick={actions.onPrint}>
        打印预览并下发
      </Button>
    )
  }
  if (status === 2) {
    return (
      <Button
        type="primary"
        icon={<SendOutlined />}
        loading={loading.changingStatus}
        onClick={() => actions.onChangeStatus(3, '确认车间已完成加工，转入待回录？')}
      >
        转待回录
      </Button>
    )
  }
  if (status === 3) {
    return (
      <Button type="primary" icon={<FileDoneOutlined />} onClick={actions.onBackRecord}>
        进入回录工作台
      </Button>
    )
  }
  if (status === 4) {
    return (
      <Space wrap>
        <Button type="primary" onClick={actions.onGoDelivery}>创建出库</Button>
        <Button icon={<FileDoneOutlined />} onClick={actions.onGoSettle}>生成结算</Button>
      </Space>
    )
  }
  if (status === 0) {
    return (
      <Button type="primary" onClick={actions.onEditDraft}>
        继续编辑草稿
      </Button>
    )
  }
  return <Button disabled>暂无可执行动作</Button>
}

function SecondaryActions({
  status,
  actions,
  loading,
}: {
  status: number
  actions: ExecutionActions
  loading: ExecutionLoading
}) {
  return (
    <Space wrap size={[8, 8]}>
      {status === 2 && (
        <Button icon={<PrinterOutlined />} onClick={actions.onPrint}>补打</Button>
      )}
      {status >= 1 && status <= 3 && (
        <Button onClick={actions.onManageRolls}>管理成品号</Button>
      )}
      {status >= 2 && status <= 4 && (
        <Button
          icon={<CalculatorOutlined />}
          loading={loading.calculatingFee}
          onClick={actions.onCalcFee}
        >
          重算计费
        </Button>
      )}
      {(status === 4 || status === 5) && (
        <Button icon={<DiffOutlined />} onClick={actions.onSnapshotDiff}>快照差异</Button>
      )}
      {status === 3 && (
        <Button
          danger
          icon={<RollbackOutlined />}
          loading={loading.changingStatus}
          onClick={() => actions.onChangeStatus(1, '确认回退到待下发？会清理完成快照和回录信息。')}
        >
          回退待下发
        </Button>
      )}
      {status === 4 && (
        <Button
          danger
          icon={<RollbackOutlined />}
          loading={loading.changingStatus}
          onClick={() => actions.onChangeStatus(3, '确认回退到待回录？')}
        >
          回退待回录
        </Button>
      )}
      {status === 1 && (
        <Button
          danger
          icon={<RollbackOutlined />}
          loading={loading.changingStatus}
          onClick={() => actions.onChangeStatus(0, '确认回退到草稿继续编辑？已生成的工序、成品号和打印快照会失效。')}
        >
          回退编辑
        </Button>
      )}
      {status === 2 && (
        <Button
          danger
          icon={<RollbackOutlined />}
          loading={loading.changingStatus}
          onClick={() => actions.onChangeStatus(1, '确认回退到待下发？已打印快照会失效，需要重新打印下发。')}
        >
          回退待下发
        </Button>
      )}
      {(status === 0 || status === 1 || status === 2) && (
        <Button
          danger
          icon={<StopOutlined />}
          loading={loading.voidingOrder}
          onClick={actions.onVoidOrder}
        >
          作废加工单
        </Button>
      )}
    </Space>
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
