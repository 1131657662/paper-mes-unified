import { Alert, Form, Modal, message } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useRef } from 'react'
import { useReceiveSettle } from '../../features/settle/hooks/useReceiveSettle'
import { useAuthUser } from '../../stores/authStore'
import ReceiveAmountFields from './ReceiveAmountFields'
import ReceivePaymentFields from './ReceivePaymentFields'
import ReceiveDiscountFields from './ReceiveDiscountFields'
import {
  buildReceiveDTO,
  receiveTotalError,
  resolveReceiveAmountChange,
  roundMoney,
  type ReceiveAmountBaseline,
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
  const sessionBaselineRef = useRef<ReceiveAmountBaseline | null>(null)
  const submittingRef = useRef(false)
  const { mutateAsync: receiveSettle, isPending: isReceiving } = useReceiveSettle()
  const user = useAuthUser()
  const usableUnreceivedAmount = roundMoney(unreceivedAmount)
  const operatorName = user?.realName ?? user?.username ?? '当前登录账号'
  const amountChange = resolveReceiveAmountChange({
    baseline: sessionBaselineRef.current,
    settleUuid,
    unreceivedAmount: usableUnreceivedAmount,
  })

  useEffect(() => {
    if (!open || !settleUuid) {
      sessionBaselineRef.current = null
      return
    }
    if (sessionBaselineRef.current?.settleUuid === settleUuid) return
    form.setFieldsValue({
      cashAmount: usableUnreceivedAmount > 0 ? usableUnreceivedAmount : undefined,
      scrapOffsetAmount: 0,
      discountAmount: 0,
      scrapWeight: undefined,
      payMethod: 2,
      receiveDate: dayjs(),
    })
    sessionBaselineRef.current = { settleUuid, unreceivedAmount: usableUnreceivedAmount }
  }, [form, open, settleUuid, usableUnreceivedAmount])

  const handleClose = () => {
    form.resetFields()
    sessionBaselineRef.current = null
    requestIdRef.current = crypto.randomUUID()
    onClose()
  }

  const handleSubmit = async () => {
    if (submittingRef.current) return
    submittingRef.current = true
    try {
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
      form.resetFields()
      sessionBaselineRef.current = null
      requestIdRef.current = crypto.randomUUID()
      onSuccess()
    } finally {
      submittingRef.current = false
    }
  }

  return (
    <Modal
      title="登记收款"
      open={open}
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={isReceiving}
      okText="确认登记"
      cancelText="取消"
      destroyOnHidden
    >
      <Form
        key={settleUuid ?? 'no-settle'}
        className="mes-modal-form"
        form={form}
        layout="vertical"
      >
        {amountChange && (
          <Form.Item>
            <Alert
              showIcon
              type="warning"
              message="未收金额已更新"
              description={`当前未收金额已从 ¥${amountChange.previousAmount.toFixed(2)} 变为 ¥${amountChange.currentAmount.toFixed(2)}，已保留本次输入，请核对后再登记。`}
            />
          </Form.Item>
        )}
        <ReceiveAmountFields form={form} unreceivedAmount={usableUnreceivedAmount} />
        <ReceiveDiscountFields form={form} open={open} settleUuid={settleUuid ?? ''}
          unreceivedAmount={usableUnreceivedAmount} />
        <ReceivePaymentFields form={form} operatorName={operatorName} />
      </Form>
    </Modal>
  )
}
