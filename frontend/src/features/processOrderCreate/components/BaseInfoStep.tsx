import { useEffect } from 'react'
import { Button, Card, Form, Tag } from 'antd'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'
import type { ReferenceOption } from '../types'
import BaseInfoFormSections from './BaseInfoFormSections'
import { baseInfoInitialValues, toBaseInfoDto, type BaseInfoFormValues } from './baseInfoModel'
import { useBaseInfoStepForm } from './useBaseInfoStepForm'
import './BaseInfoStep.css'

interface Props {
  customers: ReferenceOption[]
  warehouses: ReferenceOption[]
  initialValue?: DraftOrderBaseDTO
  loading: boolean
  onChange?: (value: DraftOrderBaseDTO) => void
  onNext: (value: DraftOrderBaseDTO) => void
}

export default function BaseInfoStep({ customers, warehouses, initialValue, loading, onChange, onNext }: Props) {
  const session = useBaseInfoStepForm({ customers, onChange })

  useEffect(() => {
    if (!initialValue || session.form.isFieldsTouched()) return
    session.form.setFieldsValue(baseInfoInitialValues(initialValue))
  }, [initialValue, session.form])

  return (
    <Card title="基础信息" className="process-order-base">
      <Form<BaseInfoFormValues>
        form={session.form}
        layout="vertical"
        initialValues={baseInfoInitialValues(initialValue)}
        onValuesChange={session.onValuesChange}
        onFinish={(value) => onNext(toBaseInfoDto(value))}
      >
        <CustomerDefaults customer={session.selectedCustomer} />
        <BaseInfoFormSections
          onCustomerChange={session.onCustomerChange}
          settleType={session.settleType}
          options={{ customers, warehouses, priorities: session.priorityOptions, invoices: session.invoiceOptions, settlements: session.settleOptions }}
        />
        <div className="process-order-base__footer">
          <Button type="primary" htmlType="submit" loading={loading}>
            下一步：原纸录入
          </Button>
        </div>
      </Form>
    </Card>
  )
}

function CustomerDefaults({ customer }: { customer?: ReferenceOption }) {
  if (!customer) return null
  return (
    <div className="process-order-base__defaults">
      <Tag color="blue">客户默认</Tag>
      <span>{settleSummary(customer)} / {invoiceSummary(customer)} / 税率 {customer.taxRate ?? 0}%</span>
    </div>
  )
}

function settleSummary(customer: ReferenceOption) {
  if (customer.settleType === 1) return '次结'
  if (customer.settleType === 2) {
    return customer.settleDay ? `月结 ${customer.settleDay}日` : '月结'
  }
  return '月结'
}

function invoiceSummary(customer: ReferenceOption) {
  if (customer.defaultInvoice === 1) return '默认开票'
  return '默认不开票'
}
