import { Alert, Button, Form, InputNumber, Statistic } from 'antd'
import type { FormInstance } from 'antd'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import { formatNumber, formatTrimmedNumber } from '../../utils/numberFormatters'
import {
  roundPrice,
  remainingAmount,
  scrapWeightError,
  receiveTotalError,
  settledAmount,
  type ReceiveFormValues,
} from './receiveFormModel'

interface Props {
  disabled?: boolean
  form: FormInstance<ReceiveFormValues>
  unreceivedAmount: number
}

export default function ReceiveAmountFields({ disabled = false, form, unreceivedAmount }: Props) {
  const cashAmount = Form.useWatch('cashAmount', form) ?? 0
  const scrapOffsetAmount = Form.useWatch('scrapOffsetAmount', form) ?? 0
  const discountAmount = Form.useWatch('discountAmount', form) ?? 0
  const scrapWeight = Form.useWatch('scrapWeight', form) ?? 0
  const canDiscount = useHasPermission(PERMISSIONS.settleDiscount)
  const totalAmount = settledAmount({ cashAmount, scrapOffsetAmount, discountAmount })
  const cashMax = remainingAmount(unreceivedAmount, { scrapOffsetAmount, discountAmount })
  const scrapMax = remainingAmount(unreceivedAmount, { cashAmount, discountAmount })
  const totalError = receiveTotalError({ cashAmount, scrapOffsetAmount, discountAmount }, unreceivedAmount)
  const scrapUnitPrice = scrapWeight > 0 ? roundPrice(Number(scrapOffsetAmount) / Number(scrapWeight)) : 0

  const fillUnreceivedAmount = () => form.setFieldsValue({
    cashAmount: unreceivedAmount,
    scrapOffsetAmount: 0,
    discountAmount: 0,
    discountReason: undefined,
    discountApprovalUuid: undefined,
  })

  const fillFullWaiver = () => form.setFieldsValue({
    cashAmount: 0,
    scrapOffsetAmount: 0,
    discountAmount: unreceivedAmount,
    discountReason: undefined,
    discountApprovalUuid: undefined,
  })

  return (
    <>
      <div className="mes-modal-tip">
        <span>未收金额</span>
        <strong>{formatNumber(unreceivedAmount, 2)}</strong>
        <Button size="small" type="link" disabled={disabled || unreceivedAmount <= 0} onClick={fillUnreceivedAmount}>
          填入未收
        </Button>
        <Button size="small" type="link" disabled={disabled || !canDiscount || unreceivedAmount <= 0} onClick={fillFullWaiver}>
          全额免收
        </Button>
      </div>
      <Form.Item name="cashAmount" label="实际到账金额">
        <InputNumber disabled={disabled} style={{ width: '100%' }} min={0} max={cashMax} precision={2} />
      </Form.Item>
      <Form.Item name="scrapOffsetAmount" label="废纸抵扣金额">
        <InputNumber disabled={disabled} style={{ width: '100%' }} min={0} max={scrapMax} precision={2} />
      </Form.Item>
      <Form.Item name="scrapWeight" label="废纸重量 kg" rules={[{
        validator: () => validateScrapWeight(form.getFieldsValue()),
      }]}>
        <InputNumber disabled={disabled} style={{ width: '100%' }} min={0} precision={3} />
      </Form.Item>
      <ReceiveAmountSummary totalAmount={totalAmount} scrapUnitPrice={scrapUnitPrice} />
      {totalError && <Alert type="error" showIcon message={totalError}
        description="实际到账、废纸抵扣和优惠核销合计不能超过未收金额。" />}
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
