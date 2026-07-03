import { Input, Modal, message } from 'antd'

interface ConfirmStatusChangeParams {
  orderNo?: string
  title: string
  reasonPlaceholder?: string
  requireReason?: boolean
  okText?: string
  danger?: boolean
  onConfirm: (reason?: string) => Promise<void> | void
}

export function confirmOrderStatusChange({
  danger,
  okText = '确认',
  onConfirm,
  orderNo,
  reasonPlaceholder = '请填写原因',
  requireReason,
  title,
}: ConfirmStatusChangeParams) {
  let reason = ''
  Modal.confirm({
    title,
    content: requireReason ? (
      <div className="mes-status-confirm">
        {orderNo && <div className="mes-status-confirm__order">{orderNo}</div>}
        <Input.TextArea
          autoSize={{ minRows: 3, maxRows: 5 }}
          maxLength={255}
          placeholder={reasonPlaceholder}
          showCount
          onChange={(event) => {
            reason = event.target.value
          }}
        />
      </div>
    ) : orderNo,
    okButtonProps: { danger },
    okText,
    cancelText: '取消',
    onOk: async () => {
      const trimmed = reason.trim()
      if (requireReason && !trimmed) {
        message.warning('请填写原因')
        throw new Error('状态变更原因不能为空')
      }
      await onConfirm(trimmed || undefined)
    },
  })
}

export function isRollbackStatusChange(currentStatus: number, targetStatus: number) {
  return (currentStatus === 1 && targetStatus === 0)
    || (currentStatus === 2 && targetStatus === 1)
    || (currentStatus === 3 && targetStatus === 1)
    || (currentStatus === 4 && targetStatus === 3)
}
