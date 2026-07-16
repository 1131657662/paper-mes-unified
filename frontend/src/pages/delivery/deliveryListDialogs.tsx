import { Modal, message } from 'antd'
import type { DeliveryOrder } from '../../types/delivery'
import { RollbackReasonInput, SignUserInput } from './DeliveryListDialogContent'

export function askSignUser(record: DeliveryOrder) {
  let signUser = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `确认签收 ${record.deliveryNo}`,
      content: <SignUserInput onChange={(value) => { signUser = value }} />,
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(signUser.trim()),
      onCancel: () => resolve(null),
    })
  })
}

export function confirmBatchSign(count: number, ignoredCount = 0) {
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: `批量签收 ${count} 张出库单`,
      content: ignoredCount > 0
        ? `将签收 ${count} 张待出库单，另有 ${ignoredCount} 张非待出库单不会处理。`
        : '确认后将逐张扣减所选出库单对应的成品库存。请确认这些单据均已完成司机签收。',
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

export function askRollbackReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `回退出库 ${deliveryNo}`,
      content: <RollbackReasonInput onChange={(value) => { reason = value }} />,
      okText: '确认回退',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => validateRollbackReason(reason, resolve),
      onCancel: () => resolve(null),
    })
  })
}

function validateRollbackReason(reason: string, resolve: (value: string) => void) {
  const value = reason.trim()
  if (value) return resolve(value)
  message.warning('请填写回退原因')
  return Promise.reject(new Error('reason required'))
}
