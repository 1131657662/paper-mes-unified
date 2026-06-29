import { Button, Card, Col, DatePicker, Form, Input, InputNumber, Row, Select, Space } from 'antd'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import type { DraftOrderBaseDTO } from '../../../types/processOrder'
import { IS_INVOICE, ORDER_SETTLE_TYPE, PRIORITY } from '../../../constants/processOrder'
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
  onNext: (value: DraftOrderBaseDTO) => void
}

const options = (dict: Record<number, string>) =>
  Object.entries(dict).map(([value, label]) => ({ value: Number(value), label }))

function toDto(value: BaseInfoFormValues): DraftOrderBaseDTO {
  return {
    ...value,
    orderDate: value.orderDate ? value.orderDate.format('YYYY-MM-DD') : dayjs().format('YYYY-MM-DD'),
    expectFinishDate: value.expectFinishDate?.format('YYYY-MM-DD'),
    settleDay: value.settleType === 2 ? value.settleDay : undefined,
  }
}

export default function BaseInfoStep({ customers, warehouses, initialValue, loading, onNext }: Props) {
  const [form] = Form.useForm<BaseInfoFormValues>()
  const settleType = Form.useWatch('settleType', form)

  const handleCustomerChange = (customerUuid: string) => {
    const customer = customers.find((item) => item.value === customerUuid)
    if (!customer) return
    const customerSettleType = customer.settleType ?? 2
    form.setFieldsValue({
      isInvoice: customer.defaultInvoice ?? 2,
      settleDay: customerSettleType === 2 ? customer.settleDay : undefined,
      settleType: customerSettleType,
      taxRate: customer.taxRate,
    })
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
        onFinish={(value) => onNext(toDto(value))}
      >
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
              <Select options={options(PRIORITY)} />
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
              <Select options={options(IS_INVOICE)} />
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="settleType" label="结算方式">
              <Select options={options(ORDER_SETTLE_TYPE)} />
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
