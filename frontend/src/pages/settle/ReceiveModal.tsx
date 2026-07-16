import { Form, Modal, message } from 'antd'
import dayjs from 'dayjs'
import { useRef } from 'react'
import { useReceiveSettle } from '../../features/settle/hooks/useReceiveSettle'
import { useAuthUser } from '../../stores/authStore'
import ReceiveAmountFields from './ReceiveAmountFields'
import ReceivePaymentFields from './ReceivePaymentFields'
import ReceiveDiscountFields from './ReceiveDiscountFields'
import {
  buildReceiveDTO,
  receiveTotalError,
  roundMoney,
  type ReceiveFormValues,
} from './receiveFormModel'

interface Props {
  settleUuid: string | null
  unreceivedAmount: number
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function ReceiveModal({
  settleUuid,
  unreceivedAmount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [form] = Form.useForm<ReceiveFormValues>()
  const requestIdRef = useRef(crypto.randomUUID())
  const { mutateAsync: receiveSettle, isPending: isReceiving } = useReceiveSettle()
  const user = useAuthUser()
  const usableUnreceivedAmount = roundMoney(unreceivedAmount)
  const operatorName = user?.realName ?? user?.username ?? '当前登录账号'

  const handleOpenChange = (visible: boolean) => {
    if (!visible) {
      form.resetFields()
      return
    }
    form.setFieldsValue({
      cashAmount: usableUnreceivedAmount > 0 ? usableUnreceivedAmount : undefined,
      scrapOffsetAmount: 0,
      discountAmount: 0,
      scrapWeight: undefined,
      payMethod: 2,
      receiveDate: dayjs(),
    })
    requestIdRef.current = crypto.randomUUID()
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (!settleUuid) {
      message.error('结算单 UUID 不能为空')
      return
    }
    const totalError = receiveTotalError(values, usableUnreceivedAmount)
    if (totalError) {
      message.error(totalError)
      return
    }
    await receiveSettle({ uuid: settleUuid, data: buildReceiveDTO(values, requestIdRef.current) })
    message.success('收款登记成功')
    onSuccess()
  }

  return (
    <Modal
      title="登记收款"
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      afterOpenChange={handleOpenChange}
      confirmLoading={isReceiving}
      okText="确认登记"
      cancelText="取消"
      destroyOnHidden
    >
      <Form className="mes-modal-form" form={form} layout="vertical">
        <ReceiveAmountFields form={form} unreceivedAmount={usableUnreceivedAmount} />
        <ReceiveDiscountFields form={form} open={open} settleUuid={settleUuid ?? ''}
          unreceivedAmount={usableUnreceivedAmount} />
        <ReceivePaymentFields form={form} operatorName={operatorName} />
      </Form>
    </Modal>
  )
}
