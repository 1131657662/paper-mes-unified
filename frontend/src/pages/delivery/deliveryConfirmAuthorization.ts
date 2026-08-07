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
    message.warning('当前账号没有“现结出库放行”权限，请由财务或管理员账号处理')
    return false
  }
  if (!await confirmCashRelease(count)) return false
  await submit(true)
  return true
}

function confirmCashRelease(count: number): Promise<boolean> {
  const subject = count > 1 ? `所选 ${count} 张出库单中` : '本次操作涉及的出库明细中'
  return new Promise((resolve) => {
    Modal.confirm({
      title: '现结未结清放行授权',
      content: `${subject}存在未结清的现结加工单。继续操作需要“现结出库放行”权限，系统将记录放行日志；创建待出库单时只锁定库存，签收时才实际扣减。`,
      okText: '授权并继续',
      okButtonProps: { danger: true },
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
