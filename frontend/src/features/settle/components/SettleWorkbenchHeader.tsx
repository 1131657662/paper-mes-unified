import { Button, DatePicker, Form, Input, Radio, Select, Space } from 'antd'
import { FileAddOutlined, ReloadOutlined } from '@ant-design/icons'
import type { Dayjs } from 'dayjs'
import { DICT_TYPES, invoiceFallbackOptions } from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'
import type { Customer } from '../../../types/customer'

const { RangePicker } = DatePicker

interface Props {
  customers: Customer[]
  loadingCustomers: boolean
  onCustomerChange: (uuid?: string) => void
  onCreate: () => void
  onReload: () => void
  selectedCount: number
  submitting: boolean
}

export default function SettleWorkbenchHeader({
  customers,
  loadingCustomers,
  onCreate,
  onCustomerChange,
  onReload,
  selectedCount,
  submitting,
}: Props) {
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)

  return (
    <div className="settle-header">
      <div className="settle-header__title">
        <h2>应收结算工作台</h2>
        <p>选择已完成加工单生成结算单，按应收、已收、未收跟踪收款进度。</p>
      </div>
      <Space className="settle-header__actions">
        <Button icon={<ReloadOutlined />} onClick={onReload}>
          刷新
        </Button>
        <Button
          type="primary"
          icon={<FileAddOutlined />}
          loading={submitting}
          disabled={selectedCount === 0}
          onClick={onCreate}
        >
          生成结算单
        </Button>
      </Space>
      <div className="settle-form-grid">
        <Form.Item<SettleWorkbenchForm> label="客户" name="customerUuid">
          <Select
            allowClear
            showSearch
            loading={loadingCustomers}
            optionFilterProp="label"
            placeholder="选择客户后查看可结算加工单"
            options={customers.map((item) => ({ value: item.uuid, label: item.customerName }))}
            onChange={onCustomerChange}
          />
        </Form.Item>
        <Form.Item<SettleWorkbenchForm> label="结算范围" name="period">
          <RangePicker />
        </Form.Item>
        <Form.Item<SettleWorkbenchForm> label="结算日期" name="settleDate">
          <DatePicker />
        </Form.Item>
        <Form.Item<SettleWorkbenchForm> label="是否开票" name="isInvoice">
          <Radio.Group optionType="button">
            {invoiceOptions.map((item) => (
              <Radio.Button key={item.value} value={item.value}>
                {item.label}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>
        <Form.Item<SettleWorkbenchForm> label="备注" name="remark">
          <Input placeholder="本次结算备注" />
        </Form.Item>
      </div>
    </div>
  )
}

export interface SettleWorkbenchForm {
  customerUuid?: string
  isInvoice?: number
  period?: [Dayjs, Dayjs]
  remark?: string
  settleDate?: Dayjs
}
