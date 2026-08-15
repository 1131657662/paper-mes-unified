import { Button, Dropdown, Space, type MenuProps } from 'antd'
import {
  CalculatorOutlined,
  DiffOutlined,
  EditOutlined,
  FileDoneOutlined,
  MoreOutlined,
  PlusOutlined,
  PrinterOutlined,
  RollbackOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { hasHistoricalUnconfirmedPrint } from '../processOrderPrintStage'

export interface ExecutionActionHandlers {
  onPrint: () => void
  onConfirmPrintAndToRecord: () => void
  onPrepareReissue: () => void
  onBackRecord: () => void
  onSnapshotDiff: () => void
  onManageRolls: () => void
  onAppendRolls?: () => void
  onEditDraft: () => void
  onChangeStatus: (targetStatus: number, title: string) => void
  onCalcFee: () => void
  onGoDelivery: () => void
  onGoSettle: () => void
  onVoidOrder: () => void
  onRollDisposition: () => void
}

export interface ExecutionLoading {
  changingStatus?: boolean
  confirmingPrint?: boolean
  rollingBackDraft?: boolean
  preparingReissue?: boolean
  calculatingFee?: boolean
  voidingOrder?: boolean
  startingAppend?: boolean
}

export interface ExecutionCapabilities {
  canManageOrder: boolean
  canCreateOrder: boolean
  canBackRecord: boolean
  canManageDelivery: boolean
  canManageSettlement: boolean
  canManageRollDisposition: boolean
}

interface Props {
  actions: ExecutionActionHandlers
  capabilities: ExecutionCapabilities
  hasPrinted: boolean
  loading: ExecutionLoading
  status: number
}

export default function ExecutionActions(props: Props) {
  return (
    <>
      <PrimaryAction {...props} />
      <SecondaryActions {...props} />
    </>
  )
}

function PrimaryAction({ actions, capabilities, hasPrinted, loading, status }: Props) {
  if (hasHistoricalUnconfirmedPrint(status, hasPrinted)) {
    if (!capabilities.canManageOrder) return null
    return <Button type="primary" icon={<PrinterOutlined />} onClick={actions.onPrint}>补确认历史打印</Button>
  }
  if (status === 0 && !capabilities.canCreateOrder) return null
  if (status === 1 && !capabilities.canManageOrder) return null
  if (status === 2 && !capabilities.canManageOrder) return null
  if (status === 3 && !capabilities.canBackRecord) return null
  if (status === 4 && !capabilities.canManageDelivery && !capabilities.canManageSettlement) return null
  if (status === 5 && !capabilities.canManageDelivery) return null
  if (status === 0) return <Button type="primary" onClick={actions.onEditDraft}>继续编辑草稿</Button>
  if (status === 1) return <Button type="primary" icon={<PrinterOutlined />} onClick={actions.onPrint}>{hasPrinted ? '打印预览' : '下发并打印'}</Button>
  if (status === 2) return (
    <Button
      type="primary"
      icon={<PrinterOutlined />}
      loading={loading.confirmingPrint}
      onClick={actions.onConfirmPrintAndToRecord}
    >
      确认打印并转待回录
    </Button>
  )
  if (status === 3) return <Button type="primary" icon={<FileDoneOutlined />} onClick={actions.onBackRecord}>进入回录工作台</Button>
  if (status === 4 || status === 5) {
    return <CompletedActions actions={actions} capabilities={capabilities} status={status} />
  }
  return <Button disabled>暂无可执行动作</Button>
}

function CompletedActions({ actions, capabilities, status }: Pick<Props, 'actions' | 'capabilities' | 'status'>) {
  return (
    <Space wrap>
      {capabilities.canManageDelivery && <Button type="primary" onClick={actions.onGoDelivery}>创建出库</Button>}
      {status === 4 && capabilities.canManageSettlement && <Button icon={<FileDoneOutlined />} onClick={actions.onGoSettle}>生成结算</Button>}
    </Space>
  )
}

function SecondaryActions({ actions, capabilities, hasPrinted, loading, status }: Props) {
  const moreItems = buildMoreItems(status, actions, capabilities)
  return (
    <Space wrap size={[8, 8]}>
      {capabilities.canCreateOrder && actions.onAppendRolls && (status === 0 || status === 1) && (
        <Button icon={<PlusOutlined />} loading={loading.startingAppend} onClick={actions.onAppendRolls}>
          追加母卷
        </Button>
      )}
      {status >= 2 && status <= 5 && capabilities.canManageOrder && !hasHistoricalUnconfirmedPrint(status, hasPrinted)
        && <Button icon={<PrinterOutlined />} onClick={actions.onPrint}>{status === 2 && !hasPrinted ? '重新打开打印' : '打印预览'}</Button>}
      {capabilities.canManageOrder && status >= 1 && status <= 3 && <Button onClick={actions.onManageRolls}>管理成品号</Button>}
      {capabilities.canManageOrder && status >= 2 && status <= 4 && <Button icon={<CalculatorOutlined />} loading={loading.calculatingFee} onClick={actions.onCalcFee}>重算计费</Button>}
      {capabilities.canManageRollDisposition && status >= 2 && status <= 3 && (
        <Button icon={<StopOutlined />} onClick={actions.onRollDisposition}>处置未加工母卷</Button>
      )}
      {(status === 4 || status === 5) && <Button icon={<DiffOutlined />} onClick={actions.onSnapshotDiff}>快照差异</Button>}
      {moreItems.length > 0 && (
        <Dropdown menu={{ items: moreItems }} placement="bottomRight" trigger={['click']}>
          <Button icon={<MoreOutlined />} loading={isMoreLoading(loading)}>更多操作</Button>
        </Dropdown>
      )}
    </Space>
  )
}

// oxlint-disable-next-line react/only-export-components
export function buildMoreItems(status: number, actions: ExecutionActionHandlers, capabilities: ExecutionCapabilities): NonNullable<MenuProps['items']> {
  const items: NonNullable<MenuProps['items']> = []
  if (!capabilities.canManageOrder) return items
  if (status === 1) items.push(rollbackItem('回退编辑', () => actions.onChangeStatus(0, '确认回退到草稿继续编辑？已生成的工序、成品号和打印快照会失效。')))
  if (status === 2) items.push(rollbackItem('回退待下发', () => actions.onChangeStatus(1, '确认回退到待下发？已打印快照会失效，需要重新打印下发。')))
  if (status === 3) items.push(rollbackItem('回退待下发', () => actions.onChangeStatus(1, '确认回退到待下发？会清理完成快照和回录信息。')))
  if (status === 4) items.push(rollbackItem('回退待回录', () => actions.onChangeStatus(3, '确认回退到待回录？')))
  if (status === 3 || status === 4) items.push(rollbackItem('回退编辑', () => actions.onChangeStatus(0, '确认回退到草稿更换母卷？会清理下发、回录、成品号和工序产物数据。')))
  if (status === 2) items.push({ icon: <EditOutlined />, key: 'reissue', label: '申请变更并重新下发', onClick: actions.onPrepareReissue })
  if (status >= 0 && status <= 2) items.push({ danger: true, icon: <StopOutlined />, key: 'void', label: '作废加工单', onClick: actions.onVoidOrder })
  return items
}

function rollbackItem(label: string, onClick: () => void) {
  return { danger: true, icon: <RollbackOutlined />, key: label, label, onClick }
}

function isMoreLoading(loading: ExecutionLoading): boolean {
  return Boolean(loading.changingStatus || loading.rollingBackDraft || loading.preparingReissue
    || loading.voidingOrder || loading.startingAppend)
}
