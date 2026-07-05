import { Button, DatePicker, Form, Input, InputNumber, Modal, Radio, Statistic, message } from 'antd'
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
  cashAmount?: number
  scrapOffsetAmount?: number
  scrapWeight?: number
  payMethod?: number
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
  const cashAmount = Form.useWatch('cashAmount', form) ?? 0
  const scrapOffsetAmount = Form.useWatch('scrapOffsetAmount', form) ?? 0
  const scrapWeight = Form.useWatch('scrapWeight', form) ?? 0
  const totalAmount = roundMoney(Number(cashAmount) + Number(scrapOffsetAmount))
  const scrapUnitPrice = scrapWeight > 0 ? roundPrice(Number(scrapOffsetAmount) / Number(scrapWeight)) : 0
  const operatorName = user?.realName ?? user?.username ?? '当前登录账号'

  const handleOpenChange = (visible: boolean) => {
    if (!visible) {
      form.resetFields()
      return
    }
    form.setFieldsValue({
      cashAmount: usableUnreceivedAmount > 0 ? usableUnreceivedAmount : undefined,
      scrapOffsetAmount: 0,
      scrapWeight: undefined,
      payMethod: 2,
      receiveDate: dayjs(),
    })
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (!settleUuid) {
      message.error('结算单 UUID 不能为空')
      return
    }
    if (totalAmount <= 0) {
      message.error('本次结清金额必须大于 0')
      return
    }
    if (totalAmount > usableUnreceivedAmount) {
      message.error('本次结清金额不能超过未收金额')
      return
    }
    const dto = buildReceiveDTO(values, totalAmount)
    await receiveMutation.mutateAsync({ uuid: settleUuid, data: dto })
    message.success('收款登记成功')
    onSuccess()
  }

  const fillUnreceivedAmount = () => {
    if (usableUnreceivedAmount <= 0) return
    form.setFieldsValue({ cashAmount: usableUnreceivedAmount, scrapOffsetAmount: 0 })
  }

  return (
    <Modal
      title="登记收款"
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      afterOpenChange={handleOpenChange}
      confirmLoading={receiveMutation.isPending}
      destroyOnHidden
      forceRender
    >
      <div className="mes-modal-tip">
        <span>未收金额</span>
        <strong>{formatMoneyText(usableUnreceivedAmount)}</strong>
        <Button size="small" type="link" disabled={usableUnreceivedAmount <= 0} onClick={fillUnreceivedAmount}>
          填入未收
        </Button>
      </div>
      <div className="mes-modal-tip mes-modal-tip--muted">
        <span>经办人</span>
        <strong>{operatorName}</strong>
        <span>将按当前登录账号记录</span>
      </div>

      <Form className="mes-modal-form" form={form} layout="vertical">
        <Form.Item name="cashAmount" label="现金实收金额">
          <InputNumber style={{ width: '100%' }} min={0} max={usableUnreceivedAmount} precision={2} />
        </Form.Item>
        <Form.Item name="scrapOffsetAmount" label="废纸抵扣金额">
          <InputNumber style={{ width: '100%' }} min={0} max={usableUnreceivedAmount} precision={2} />
        </Form.Item>
        <Form.Item
          name="scrapWeight"
          label="废纸重量 kg"
          rules={[{ validator: () => validateScrapWeight(scrapOffsetAmount, scrapWeight) }]}
        >
          <InputNumber style={{ width: '100%' }} min={0} precision={3} />
        </Form.Item>
        <div className="mes-modal-tip">
          <Statistic title="本次结清" value={totalAmount} precision={2} prefix="¥" />
          <Statistic title="废纸折算单价" value={scrapUnitPrice} precision={4} suffix="元/kg" />
        </div>
        <Form.Item
          name="payMethod"
          label="现金收款方式"
          rules={[{ validator: (_, value) => validatePayMethod(cashAmount, value) }]}
        >
          <Radio.Group disabled={Number(cashAmount) <= 0}>
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
          <Input.TextArea rows={2} placeholder="可填写废纸来源、抵扣说明或收款备注" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

function buildReceiveDTO(values: ReceiveFormValues, totalAmount: number): ReceiveDTO {
  return {
    receiveAmount: totalAmount,
    cashAmount: roundMoney(values.cashAmount),
    scrapOffsetAmount: roundMoney(values.scrapOffsetAmount),
    scrapWeight: roundWeight(values.scrapWeight),
    payMethod: Number(values.cashAmount ?? 0) > 0 ? values.payMethod : undefined,
    payNo: cleanText(values.payNo),
    receiveDate: values.receiveDate?.format('YYYY-MM-DDTHH:mm:ss'),
    remark: cleanText(values.remark),
  }
}

function validateScrapWeight(scrapOffsetAmount: number, scrapWeight: number) {
  if (Number(scrapOffsetAmount) > 0 && Number(scrapWeight) <= 0) {
    return Promise.reject(new Error('废纸抵扣金额大于 0 时，废纸重量必须大于 0'))
  }
  return Promise.resolve()
}

function validatePayMethod(cashAmount: number, value?: number) {
  if (Number(cashAmount) > 0 && !value) {
    return Promise.reject(new Error('现金实收金额大于 0 时必须选择收款方式'))
  }
  return Promise.resolve()
}

function roundMoney(value?: number) {
  return Math.round(Number(value || 0) * 100) / 100
}

function roundWeight(value?: number) {
  return Math.round(Number(value || 0) * 1000) / 1000
}

function roundPrice(value: number) {
  return Math.round(Number(value || 0) * 10000) / 10000
}

function formatMoneyText(value: number) {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function cleanText(value?: string) {
  const text = value?.trim()
  return text || undefined
}
