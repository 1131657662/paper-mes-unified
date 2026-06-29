import { Button, DatePicker, Form, Input, Select, Space } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import type { Customer } from '../../../types/customer'

interface Props {
  customers: Customer[]
  loadingCustomers: boolean
  selectedCustomerUuid?: string
  onCustomerChange: (uuid?: string) => void
  onReload: () => void
  onSubmit: () => void
  submitting: boolean
}

export default function DeliveryWorkbenchHeader({
  customers,
  loadingCustomers,
  onCustomerChange,
  onReload,
  onSubmit,
  selectedCustomerUuid,
  submitting,
}: Props) {
  return (
    <div className="delivery-header">
      <div className="delivery-header__title">
        <h2>出库管理</h2>
        <p>按客户筛选可出库成品，跨加工单合并提货，确认后扣减库存。</p>
      </div>
      <Space className="delivery-header__actions">
        <Button icon={<ReloadOutlined />} onClick={onReload}>
          刷新
        </Button>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          loading={submitting}
          disabled={!selectedCustomerUuid}
          onClick={onSubmit}
        >
          生成待出库单
        </Button>
      </Space>
      <div className="delivery-form-grid">
        <Form.Item label="客户" name="customerUuid" rules={[{ required: true, message: '请选择客户' }]}>
          <Select
            allowClear
            showSearch
            loading={loadingCustomers}
            optionFilterProp="label"
            placeholder="选择客户后显示可出库库存"
            options={customers.map((item) => ({ value: item.uuid, label: item.customerName }))}
            onChange={onCustomerChange}
          />
        </Form.Item>
        <Form.Item<DeliveryHeaderForm> label="出库日期" name="deliveryDate" rules={[{ required: true, message: '请选择日期' }]}>
          <DatePicker />
        </Form.Item>
        <Form.Item<DeliveryHeaderForm> label="提货人" name="pickerName">
          <Input placeholder="提货人" />
        </Form.Item>
        <Form.Item<DeliveryHeaderForm> label="车牌号" name="carNo">
          <Input placeholder="车牌号" />
        </Form.Item>
        <Form.Item<DeliveryHeaderForm> label="柜号" name="containerNo">
          <Input placeholder="柜号" />
        </Form.Item>
        <Form.Item<DeliveryHeaderForm> label="备注" name="remark">
          <Input placeholder="本次出库备注" />
        </Form.Item>
      </div>
    </div>
  )
}

export interface DeliveryHeaderForm {
  carNo?: string
  containerNo?: string
  customerUuid?: string
  deliveryDate?: Dayjs
  pickerName?: string
  remark?: string
}
