import { DatePicker, Form, Input, Radio } from 'antd'
import type { FormInstance } from 'antd'
import { payMethodError, payNoError, type ReceiveFormValues } from './receiveFormModel'

interface Props {
  form: FormInstance<ReceiveFormValues>
  operatorName: string
}

export default function ReceivePaymentFields({ form, operatorName }: Props) {
  const cashAmount = Form.useWatch('cashAmount', form) ?? 0

  return (
    <>
      <div className="mes-modal-tip mes-modal-tip--muted">
        <span>经办人</span>
        <strong>{operatorName}</strong>
        <span>将按当前登录账号记录</span>
      </div>
      <Form.Item name="payMethod" label="到账方式" rules={[{
        validator: () => validatePayMethod(form.getFieldsValue()),
      }]}>
        <Radio.Group disabled={Number(cashAmount) <= 0}>
          <Radio value={1}>现金</Radio>
          <Radio value={2}>转账</Radio>
          <Radio value={3}>微信</Radio>
          <Radio value={4}>支付宝</Radio>
        </Radio.Group>
      </Form.Item>
      <Form.Item name="payNo" label="交易流水号" rules={[{
        validator: () => validatePayNo(form.getFieldsValue()),
      }]}>
        <Input placeholder="转账、微信或支付宝到账时必填" />
      </Form.Item>
      <Form.Item name="receiveDate" label="收款时间">
        <DatePicker showTime placeholder="选择收款时间" style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item name="remark" label="备注">
        <Input.TextArea rows={2} placeholder="可填写废纸来源或到账备注" />
      </Form.Item>
    </>
  )
}

function validatePayNo(values: ReceiveFormValues): Promise<void> {
  const error = payNoError(values)
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}

function validatePayMethod(values: ReceiveFormValues): Promise<void> {
  const error = payMethodError(values)
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}
