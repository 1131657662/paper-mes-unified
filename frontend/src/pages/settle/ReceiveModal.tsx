import { useEffect } from 'react'
import { Button, DatePicker, Form, Input, InputNumber, Modal, Radio, message } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { useReceiveSettle } from '../../features/settle/hooks/useReceiveSettle'
import { useAuthUser } from '../../stores/authStore'
import type { ReceiveDTO } from '../../types/settle'

interface Props {
  settleUuid: string | null
  unreceivedAmount: number
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

interface ReceiveFormValues {
  receiveAmount: number
  payMethod: number
  payNo?: string
  receiveDate?: Dayjs
  remark?: string
}

export default function ReceiveModal({
  settleUuid,
  unreceivedAmount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [form] = Form.useForm<ReceiveFormValues>()
  const receiveMutation = useReceiveSettle()
  const user = useAuthUser()
  const usableUnreceivedAmount = roundMoney(unreceivedAmount)
  const canReuseUnreceived = usableUnreceivedAmount > 0
  const operatorName = user?.realName ?? user?.username ?? '当前登录账号'

  useEffect(() => {
    if (!open) {
      form.resetFields()
      return
    }
    form.setFieldsValue({
      payMethod: 2,
      receiveAmount: canReuseUnreceived ? usableUnreceivedAmount : undefined,
      receiveDate: dayjs(),
    })
  }, [canReuseUnreceived, form, open, usableUnreceivedAmount])

  const handleSubmit = async () => {
    const values = await form.validateFields()

    if (!settleUuid) {
      message.error('结算单 UUID 不能为空')
      return
    }

    const dto: ReceiveDTO = {
      receiveAmount: values.receiveAmount,
      payMethod: values.payMethod,
      payNo: cleanText(values.payNo),
      receiveDate: values.receiveDate?.format('YYYY-MM-DDTHH:mm:ss'),
      remark: cleanText(values.remark),
    }

    await receiveMutation.mutateAsync({ uuid: settleUuid, data: dto })
    message.success('收款登记成功')
    onSuccess()
  }

  const fillUnreceivedAmount = () => {
    if (!canReuseUnreceived) return
    form.setFieldValue('receiveAmount', usableUnreceivedAmount)
  }

  return (
    <Modal
      title="收款登记"
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      confirmLoading={receiveMutation.isPending}
      destroyOnHidden
      forceRender
    >
      <div className="mes-modal-tip">
        <span>未收金额</span>
        <strong>{formatMoneyText(usableUnreceivedAmount)}</strong>
        <Button size="small" type="link" disabled={!canReuseUnreceived} onClick={fillUnreceivedAmount}>
          填入未收
        </Button>
      </div>
      <div className="mes-modal-tip mes-modal-tip--muted">
        <span>经办人</span>
        <strong>{operatorName}</strong>
        <span>将按当前登录账号记录</span>
      </div>
      <Form className="mes-modal-form" form={form} layout="vertical">
        <Form.Item
          name="receiveAmount"
          label="收款金额"
          rules={[
            { required: true, message: '请输入收款金额' },
            { type: 'number', min: 0.01, message: '收款金额必须大于 0' },
          ]}
        >
          <InputNumber
            style={{ width: '100%' }}
            placeholder="输入收款金额"
            max={usableUnreceivedAmount}
            precision={2}
          />
        </Form.Item>

        <Form.Item
          name="payMethod"
          label="收款方式"
          rules={[{ required: true, message: '请选择收款方式' }]}
        >
          <Radio.Group>
            <Radio value={1}>现金</Radio>
            <Radio value={2}>转账</Radio>
            <Radio value={3}>微信</Radio>
            <Radio value={4}>支付宝</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item name="payNo" label="流水号">
          <Input placeholder="银行流水号或交易号" />
        </Form.Item>

        <Form.Item name="receiveDate" label="收款时间" initialValue={dayjs()}>
          <DatePicker showTime style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item name="remark" label="备注">
          <Input.TextArea rows={2} placeholder="备注信息" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function roundMoney(value: number) {
  return Math.round(Number(value || 0) * 100) / 100
}

function formatMoneyText(value: number) {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function cleanText(value?: string) {
  const text = value?.trim()
  return text || undefined
}
