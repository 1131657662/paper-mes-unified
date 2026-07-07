import { Alert, Button, Card, Col, DatePicker, Form, Input, InputNumber, Row, Select, Space } from 'antd'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'
import {
  DICT_TYPES,
  invoiceFallbackOptions,
  priorityFallbackOptions,
  settleFallbackOptions,
} from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'
import type { ReferenceOption } from '../types'

interface BaseInfoFormValues extends Omit<DraftOrderBaseDTO, 'orderDate' | 'expectFinishDate'> {
  orderDate?: Dayjs
  expectFinishDate?: Dayjs
}

interface Props {
  customers: ReferenceOption[]
  warehouses: ReferenceOption[]
  initialValue?: DraftOrderBaseDTO
  loading: boolean
  onChange?: (value: DraftOrderBaseDTO) => void
  onNext: (value: DraftOrderBaseDTO) => void
}

function toDto(value: BaseInfoFormValues): DraftOrderBaseDTO {
  return {
    ...value,
    orderDate: value.orderDate ? value.orderDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
    expectFinishDate: value.expectFinishDate?.format('YYYY-MM-DD'),
    settleDay: value.settleType === 2 ? value.settleDay : undefined,
  }
}

export default function BaseInfoStep({ customers, warehouses, initialValue, loading, onChange, onNext }: Props) {
  const [form] = Form.useForm<BaseInfoFormValues>()
  const { options: priorityOptions } = useNumberDictOptions(DICT_TYPES.priority, priorityFallbackOptions)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)
  const customerUuid = Form.useWatch('customerUuid', form)
  const settleType = Form.useWatch('settleType', form)
  const selectedCustomer = customers.find((item) => item.value === customerUuid)

  const handleCustomerChange = (customerUuid: string) => {
    const customer = customers.find((item) => item.value === customerUuid)
    if (!customer) return
    const customerSettleType = customer.settleType ?? 2
    const nextValues: Partial<BaseInfoFormValues> = {
      isInvoice: customer.defaultInvoice ?? 2,
      settleDay: customerSettleType === 2 ? customer.settleDay : undefined,
      settleType: customerSettleType,
      taxRate: customer.taxRate,
    }
    form.setFieldsValue(nextValues)
    onChange?.(toDto({ ...form.getFieldsValue(), ...nextValues, customerUuid }))
  }

  const handleValuesChange = (_: Partial<BaseInfoFormValues>, values: BaseInfoFormValues) => {
    onChange?.(toDto(values))
  }

  return (
    <Card title="基础信息">
      <Form<BaseInfoFormValues>
        form={form}
        layout="vertical"
        initialValues={{
          ...initialValue,
          orderDate: initialValue?.orderDate ? dayjs(initialValue.orderDate) : dayjs(),
          expectFinishDate: initialValue?.expectFinishDate ? dayjs(initialValue.expectFinishDate) : undefined,
          priority: initialValue?.priority ?? 1,
          isInvoice: initialValue?.isInvoice ?? 2,
          settleType: initialValue?.settleType ?? 2,
        }}
        onValuesChange={handleValuesChange}
        onFinish={(value) => onNext(toDto(value))}
      >
        {selectedCustomer && (
          <Alert
            className="process-order-base-defaults"
            type="info"
            showIcon
            message="已按客户档案带出默认结算与开票设置"
            description={`默认${settleSummary(selectedCustomer)}，${invoiceSummary(selectedCustomer)}，税率 ${selectedCustomer.taxRate ?? 0}%。本单仍可在下方手动修改。`}
          />
        )}
        <Row gutter={16}>
          <Col span={6}>
            <Form.Item name="customerUuid" label="客户" rules={[{ required: true, message: '请选择客户' }]}>
              <Select
                showSearch
                optionFilterProp="label"
                options={customers}
                placeholder="选择客户"
                onChange={handleCustomerChange}
              />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="orderDate" label="制单日期" rules={[{ required: true, message: '请选择日期' }]}>
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="expectFinishDate" label="期望完成">
              <DatePicker style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="priority" label="优先级">
              <Select options={priorityOptions} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="warehouseUuid" label="仓库">
              <Select allowClear options={warehouses} placeholder="选择仓库" />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="teamGroup" label="班组">
              <Input placeholder="班组" />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="isInvoice" label="是否开票">
              <Select options={invoiceOptions} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="settleType" label="结算方式">
              <Select options={settleOptions} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="settleDay" label="月结对账日">
              <InputNumber
                min={1}
                max={31}
                disabled={settleType !== 2}
                style={{ width: '100%' }}
                placeholder={settleType === 2 ? '如 25' : '次结无需填写'}
              />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="taxRate" label="税率(%)">
              <InputNumber min={0} max={100} style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={24}>
            <Form.Item name="remark" label="备注">
              <Input.TextArea rows={2} />
            </Form.Item>
          </Col>
        </Row>
        <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
          <Button type="primary" htmlType="submit" loading={loading}>
            下一步：原纸录入
          </Button>
        </Space>
      </Form>
    </Card>
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
