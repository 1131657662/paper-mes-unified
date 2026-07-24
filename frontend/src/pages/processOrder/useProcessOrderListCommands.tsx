import type { RefObject } from 'react'
import { Input, Modal, message } from 'antd'
import { useLocation, useNavigate, type NavigateFunction } from 'react-router-dom'
import type { ActionType } from '@ant-design/pro-components'
import {
  calcProcessOrderFee,
  changeOrderStatus,
  getProcessOrder,
  completeProcessOrder,
  rollbackProcessOrderToDraft,
  voidProcessOrder,
} from '../../api/processOrder'
import type { ProcessOrder } from '../../types/processOrder'
import {
  confirmOrderStatusChange,
  isRollbackStatusChange,
} from '../../features/processOrderDetail/confirmOrderStatusChange'
import type { BatchActions } from './ProcessOrderBatchToolbar'
import type { ProcessOrderColumnOptions } from './processOrderListColumns'
import type { useProcessOrderListDialogs } from './useProcessOrderListDialogs'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'
import { processOrderListLocation } from './processOrderNavigation'

interface Params {
  actionRef: RefObject<ActionType | undefined>
  capabilities: ProcessOrderListCapabilities
  clearSelection: () => void
  customerEnum: Record<string, { text: string }>
  dialogs: ReturnType<typeof useProcessOrderListDialogs>
}

interface CommandContext extends Params {
  navigate: NavigateFunction
}

export function useProcessOrderListCommands(params: Params) {
  const navigate = useNavigate()
  const location = useLocation()
  const context = { ...params, navigate }
  const navigation = navigationCommands(navigate, processOrderListLocation(location.pathname, location.search))
  const batchActions: BatchActions = {
    onBackRecord: navigation.openRecord,
    onCalcFee: (record) => calculateFee(context, record),
    onChangeStatus: (record, target, title) => requestTransition(context, record, target, title),
    onGoDelivery: navigation.goDelivery,
    onGoSettle: navigation.goSettle,
    onManageRolls: params.dialogs.openManageRoll,
    onPrint: (record) => openPrint(context, record),
    onSnapshotDiff: params.dialogs.openDiff,
    onVoidOrder: (record) => requestVoid(context, record),
  }
  const columnOptions: ProcessOrderColumnOptions = {
    ...batchActions,
    capabilities: params.capabilities,
    customerEnum: params.customerEnum,
    onDetail: navigation.openDetail,
    onEditDraft: navigation.editDraft,
  }
  return { batchActions, columnOptions, onCreate: navigation.create }
}

function navigationCommands(navigate: NavigateFunction, returnTo: string) {
  return {
    create: () => navigate('/process-orders/create'),
    editDraft: (uuid: string) => navigate(`/process-orders/create?draft=${uuid}`),
    goDelivery: async (record: ProcessOrder) => {
      const detail = await getProcessOrder(record.uuid)
      const finishUuids = (detail.finishRolls ?? []).map((finish) => finish.uuid)
      navigate(`/delivery-orders/create?customerUuid=${encodeURIComponent(record.customerUuid ?? '')}`, {
        state: { finishUuids, from: returnTo },
      })
    },
    goSettle: (record: ProcessOrder) => navigate('/settle-orders/create', {
      state: { initialOrderUuids: [record.uuid], from: returnTo },
    }),
    openDetail: (uuid: string) => navigate(`/process-orders/${uuid}`, { state: { from: returnTo } }),
    openRecord: (uuid: string) => navigate(`/process-orders/${uuid}/back-record`, { state: { from: returnTo } }),
  }
}

async function executeTransition(
  context: CommandContext,
  record: ProcessOrder,
  target: number,
  reason?: string,
) {
  if (target === 0) await rollbackProcessOrderToDraft(record.uuid, { reason: reason ?? '' })
  else if (record.orderStatus === 2 && target === 3) await completeProcessOrder(record.uuid, reason)
  else await changeOrderStatus(record.uuid, { reason, targetStatus: target })
  message.success('状态已更新')
  context.clearSelection()
  void context.actionRef.current?.reload()
  if (target === 0) context.navigate(`/process-orders/create?draft=${record.uuid}`)
}

function requestTransition(context: CommandContext, record: ProcessOrder, target: number, title: string) {
  const requireReason = isRollbackStatusChange(record.orderStatus ?? 0, target)
  confirmOrderStatusChange({
    title,
    orderNo: record.orderNo,
    okText: requireReason ? '确认回退' : '确认',
    danger: requireReason,
    requireReason,
    reasonPlaceholder: '请填写回退原因，例如：客户改单、现场方案调整、下发前补充信息',
    onConfirm: (reason) => executeTransition(context, record, target, reason),
  })
}

function openPrint(context: CommandContext, record: ProcessOrder) {
  context.dialogs.openPrint({
    uuid: record.uuid,
    orderNo: record.orderNo,
    printCount: record.printCount,
  })
}

async function calculateFee(context: CommandContext, record: ProcessOrder) {
  const result = await calcProcessOrderFee(record.uuid)
  message.success(`计费完成，总额 ¥${result.totalAmount ?? 0}`)
  void context.actionRef.current?.reload()
}

async function requestVoid(context: CommandContext, record: ProcessOrder) {
  let reason = ''
  Modal.confirm({
    title: '作废加工单',
    content: <Input.TextArea autoSize={{ minRows: 3, maxRows: 5 }} maxLength={255} placeholder="请填写作废原因" showCount onChange={(event) => { reason = event.target.value }} />,
    okButtonProps: { danger: true },
    okText: '确认作废',
    cancelText: '取消',
    onOk: () => voidOrder(context, record, reason),
  })
}

async function voidOrder(context: CommandContext, record: ProcessOrder, reason: string) {
  const trimmed = reason.trim()
  if (!trimmed) {
    message.warning('请填写作废原因')
    throw new Error('作废原因不能为空')
  }
  await voidProcessOrder(record.uuid, { reason: trimmed })
  message.success('加工单已作废')
  context.clearSelection()
  void context.actionRef.current?.reload()
}
