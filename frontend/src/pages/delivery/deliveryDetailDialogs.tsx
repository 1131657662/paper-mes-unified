import { Input, Modal, message } from 'antd'

export function askDeliverySignUser(deliveryNo: string) {
  let signUser = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `确认签收 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>确认后将扣减本单成品库存，出库状态变为已出库。</p>
          <Input placeholder="签收人姓名（可选）" onChange={(event) => { signUser = event.target.value }} />
        </div>
      ),
      okText: '确认签收',
      cancelText: '取消',
      onOk: () => resolve(signUser.trim()),
      onCancel: () => resolve(null),
    })
  })
}

export function askDeliveryRollbackReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string>((resolve, reject) => {
    Modal.confirm({
      title: `回退出库 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>回退后成品卷恢复为已入库，出库单回到待出库状态，可移出装不下的明细后重新签收。</p>
          <Input.TextArea
            rows={3}
            placeholder="请输入回退原因，例如：车辆装不下，需要减少本次装车卷数"
            onChange={(event) => { reason = event.target.value }}
          />
        </div>
      ),
      okText: '确认回退',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolveRequiredReason(reason, resolve),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}

export function confirmRemoveDeliveryDetail(finishRollNo: string) {
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '移出出库明细',
      content: `确认将 ${finishRollNo || '该成品卷'} 从本张待出库单中移出？移出后可重新勾选出库。`,
      okText: '移出',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

export function askDeliveryCancelReason(deliveryNo: string) {
  let reason = ''
  return new Promise<string | null>((resolve) => {
    Modal.confirm({
      title: `作废待出库单 ${deliveryNo}`,
      content: (
        <div className="delivery-sign-modal">
          <p>作废后本单全部成品将释放，可重新创建出库单。已签收单不能在此作废。</p>
          <Input.TextArea
            rows={3}
            placeholder="请输入作废原因，例如：选错成品，需要重新开单"
            onChange={(event) => { reason = event.target.value }}
          />
        </div>
      ),
      okText: '确认作废',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolveCancelReason(reason, resolve),
      onCancel: () => resolve(null),
    })
  })
}

function resolveRequiredReason(reason: string, resolve: (value: string) => void) {
  const value = reason.trim()
  if (!value) {
    message.warning('请填写回退原因')
    return Promise.reject(new Error('reason required'))
  }
  resolve(value)
}

function resolveCancelReason(reason: string, resolve: (value: string) => void) {
  const value = reason.trim()
  if (!value) {
    message.warning('请填写作废原因')
    return Promise.reject(new Error('reason required'))
  }
  resolve(value)
}
