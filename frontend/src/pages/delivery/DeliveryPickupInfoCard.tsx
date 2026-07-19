import { Card, DatePicker, Form, Input, Select } from 'antd'
import type { FormInstance } from 'antd'
import dayjs from 'dayjs'
import type { Customer } from '../../types/customer'
import type { Warehouse } from '../../types/warehouse'
import type { DeliveryCreateFormValues } from './deliveryCreateSubmit'

interface Props {
  customers: Customer[]
  form: FormInstance<DeliveryCreateFormValues>
  initialCustomerUuid?: string
  initialWarehouseUuid?: string
  loading: boolean
  warehouses: Warehouse[]
  onCustomerChange: (value?: string) => void
  onWarehouseChange: (value?: string) => void
}

export default function DeliveryPickupInfoCard(props: Props) {
  return (
    <Card className="document-module-card delivery-create-page__info" title="提货信息">
      <Form
        form={props.form}
        layout="vertical"
        initialValues={{ customerUuid: props.initialCustomerUuid, warehouseUuid: props.initialWarehouseUuid, deliveryDate: dayjs() }}
      >
        <div className="document-module-grid delivery-create-page__form">
          <Form.Item
            name="customerUuid"
            label="客户"
            rules={[{ required: true, message: '请选择客户' }]}
          >
            <Select
              allowClear
              showSearch
              loading={props.loading}
              placeholder="选择客户"
              options={props.customers.map((item) => ({
                label: item.customerName,
                value: item.uuid,
              }))}
              optionFilterProp="label"
              onChange={props.onCustomerChange}
            />
          </Form.Item>
          <Form.Item name="warehouseUuid" label="出库仓库" rules={[{ required: true, message: '请选择出库仓库' }]}>
            <Select
              showSearch
              loading={props.loading}
              placeholder="选择出库仓库"
              options={props.warehouses.filter((item) => item.status === 1).map((item) => ({
                label: `${item.isDefault === 1 ? '默认 · ' : ''}${item.location ? `${item.warehouseName} · ${item.location}` : item.warehouseName}`,
                value: item.uuid,
              }))}
              optionFilterProp="label"
              onChange={props.onWarehouseChange}
            />
          </Form.Item>
          <Form.Item
            name="deliveryDate"
            label="出库日期"
            rules={[{ required: true, message: '请选择出库日期' }]}
          >
            <DatePicker />
          </Form.Item>
          <Form.Item name="pickerName" label="提货人">
            <Input placeholder="司机或提货人姓名" />
          </Form.Item>
          <Form.Item name="carNo" label="车牌号">
            <Input placeholder="车牌号" />
          </Form.Item>
          <Form.Item name="containerNo" label="柜号">
            <Input placeholder="柜号" />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input placeholder="本次出库备注" />
          </Form.Item>
        </div>
      </Form>
    </Card>
  )
}
