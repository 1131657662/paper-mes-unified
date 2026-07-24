import { message, Modal } from 'antd'
import { BizError } from '../../api/request'

export async function authorizeDeliveryConfirmation(
  submit: (forceRelease: boolean) => Promise<unknown>,
  count = 1,
  canRelease = true,
): Promise<boolean> {
  try {
    await submit(false)
    return true
  } catch (error) {
    if (!(error instanceof BizError) || error.errorCode !== 'E010') throw error
  }

  if (!canRelease) {
    message.warning('该出库单存在未结清现结款项，请由财务或管理员账号签收放行')
    return false
  }
  if (!await confirmCashRelease(count)) return false
  await submit(true)
  return true
}

function confirmCashRelease(count: number): Promise<boolean> {
  const subject = count > 1 ? `所选 ${count} 张出库单中` : '该出库单中'
  return new Promise((resolve) => {
    Modal.confirm({
      title: '授权现结未结清出库',
      content: `${subject}存在未结清的现结加工单。继续将实际扣减成品库存并记录放行日志，确认授权出库吗？`,
      okText: '授权实际出库',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
