import { Button, Form, InputNumber, Statistic } from 'antd'
import type { FormInstance } from 'antd'
import { formatNumber, formatTrimmedNumber } from '../../utils/numberFormatters'
import {
  roundPrice,
  scrapWeightError,
  settledAmount,
  type ReceiveFormValues,
} from './receiveFormModel'

interface Props {
  form: FormInstance<ReceiveFormValues>
  unreceivedAmount: number
}

export default function ReceiveAmountFields({ form, unreceivedAmount }: Props) {
  const cashAmount = Form.useWatch('cashAmount', form) ?? 0
  const scrapOffsetAmount = Form.useWatch('scrapOffsetAmount', form) ?? 0
  const discountAmount = Form.useWatch('discountAmount', form) ?? 0
  const scrapWeight = Form.useWatch('scrapWeight', form) ?? 0
  const totalAmount = settledAmount({ cashAmount, scrapOffsetAmount, discountAmount })
  const scrapUnitPrice = scrapWeight > 0 ? roundPrice(Number(scrapOffsetAmount) / Number(scrapWeight)) : 0

  const fillUnreceivedAmount = () => form.setFieldsValue({
    cashAmount: unreceivedAmount,
    scrapOffsetAmount: 0,
    discountAmount: 0,
  })

  return (
    <>
      <div className="mes-modal-tip">
        <span>未收金额</span>
        <strong>{formatNumber(unreceivedAmount, 2)}</strong>
        <Button size="small" type="link" disabled={unreceivedAmount <= 0} onClick={fillUnreceivedAmount}>
          填入未收
        </Button>
      </div>
      <Form.Item name="cashAmount" label="现金实收金额">
        <InputNumber style={{ width: '100%' }} min={0} max={unreceivedAmount} precision={2} />
      </Form.Item>
      <Form.Item name="scrapOffsetAmount" label="废纸抵扣金额">
        <InputNumber style={{ width: '100%' }} min={0} max={unreceivedAmount} precision={2} />
      </Form.Item>
      <Form.Item
        name="discountAmount"
        label="优惠/尾差核销"
        tooltip="不计入客户实际支付，仅用于核销双方确认的优惠或尾差"
      >
        <InputNumber style={{ width: '100%' }} min={0} max={unreceivedAmount} precision={2} />
      </Form.Item>
      <Form.Item name="scrapWeight" label="废纸重量 kg" rules={[{
        validator: () => validateScrapWeight(form.getFieldsValue()),
      }]}>
        <InputNumber style={{ width: '100%' }} min={0} precision={3} />
      </Form.Item>
      <ReceiveAmountSummary totalAmount={totalAmount} scrapUnitPrice={scrapUnitPrice} />
    </>
  )
}

function ReceiveAmountSummary({ totalAmount, scrapUnitPrice }: {
  totalAmount: number
  scrapUnitPrice: number
}) {
  return (
    <div className="mes-modal-tip">
      <Statistic title="本次结清" value={totalAmount} precision={2} prefix="¥" formatter={formatMoneyValue} />
      <Statistic title="废纸折算单价" value={scrapUnitPrice} precision={4} suffix="元/kg" formatter={formatPriceValue} />
    </div>
  )
}

function validateScrapWeight(values: ReceiveFormValues): Promise<void> {
  const error = scrapWeightError(values)
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}

function formatMoneyValue(value?: string | number): string {
  return formatNumber(Number(value ?? 0), 2)
}

function formatPriceValue(value?: string | number): string {
  return formatTrimmedNumber(Number(value ?? 0), 4)
}
