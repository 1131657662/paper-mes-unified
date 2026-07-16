import { DatePicker, Form, Input, Radio } from 'antd'
import type { FormInstance } from 'antd'
import { payMethodError, type ReceiveFormValues } from './receiveFormModel'

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
      <Form.Item name="payMethod" label="现金收款方式" rules={[{
        validator: () => validatePayMethod(form.getFieldsValue()),
      }]}>
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
      <Form.Item name="receiveDate" label="收款时间">
        <DatePicker showTime placeholder="选择收款时间" style={{ width: '100%' }} />
      </Form.Item>
      <Form.Item name="remark" label="备注">
        <Input.TextArea rows={2} placeholder="可填写优惠原因、废纸来源或收款备注" />
      </Form.Item>
    </>
  )
}

function validatePayMethod(values: ReceiveFormValues): Promise<void> {
  const error = payMethodError(values)
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}
