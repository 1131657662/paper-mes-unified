import { Button, Card, Descriptions, Empty, Skeleton, Space, Tag } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCustomer } from '../../api/customer'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../features/systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../features/systemConfig/hooks/useRuntimeDictOptions'
import { useHasPermission } from '../../stores/authStore'
import type { Customer } from '../../types/customer'
import CustomerServicePriceSummary from './CustomerServicePriceSummary'
import '../documentModule.css'
import './CustomerProfile.css'

const PRICE_TAX_TYPE: Record<number, string> = { 1: '含税价', 2: '未税价' }

export default function CustomerDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [customer, setCustomer] = useState<Customer>()
  const [loading, setLoading] = useState(true)
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const { options: settleOptions } = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)

  useEffect(() => {
    if (!uuid) return
    setLoading(true)
    getCustomer(uuid)
      .then(setCustomer)
      .finally(() => setLoading(false))
  }, [uuid])

  if (loading) {
    return (
      <div className="document-module-page customer-profile-page">
        <Skeleton active paragraph={{ rows: 10 }} />
      </div>
    )
  }

  if (!customer) {
    return (
      <div className="document-module-page customer-profile-page">
        <Empty description="客户不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page customer-profile-page">
      <MesPageHeader
        title={customer.customerName}
        eyebrow="客户档案"
        description={`客户编码：${customer.customerCode ?? '-'} · 联系人：${customer.contact ?? '-'} · 电话：${customer.phone ?? '-'}`}
        onBack={() => navigate('/customers')}
        tags={<Tag color="blue">{settleText(customer, settleOptions)}</Tag>}
        actions={canManageBase ? (
          <Space>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/customers/${customer.uuid}/edit`)}>
              编辑客户
            </Button>
          </Space>
        ) : undefined}
      />

      <section className="customer-detail-overview">
        <MetricCard label="结算方式" value={settleText(customer, settleOptions)} helper="新建加工单默认带出" />
        <MetricCard label="默认开票" value={invoiceText(customer.defaultInvoice, invoiceOptions)} helper={`税率 ${customer.taxRate ?? 0}%`} />
        <MetricCard label="默认锯纸单价" value={moneyText(customer.sawPrice, '元/刀')} helper={priceTaxText(customer.priceIncludeTax)} />
        <MetricCard label="默认复卷单价" value={moneyText(customer.rewindPrice, '元/吨')} helper={priceTaxText(customer.priceIncludeTax)} />
      </section>

      <div className="customer-detail-grid">
        <Card className="document-module-card customer-detail-grid__full" title="附加工艺价格">
          <CustomerServicePriceSummary prices={customer.processPrices} />
        </Card>

        <Card className="document-module-card" title="基础资料">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="客户编码">{text(customer.customerCode)}</Descriptions.Item>
            <Descriptions.Item label="客户名称">{text(customer.customerName)}</Descriptions.Item>
            <Descriptions.Item label="联系人">{text(customer.contact)}</Descriptions.Item>
            <Descriptions.Item label="电话">{text(customer.phone)}</Descriptions.Item>
            <Descriptions.Item label="客户等级">{customerLevelText(customer.customerLevel)}</Descriptions.Item>
            <Descriptions.Item label="模板标识">{text(customer.exportTemplate)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="结算与计费">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="结算方式">{settleText(customer, settleOptions)}</Descriptions.Item>
            <Descriptions.Item label="价格口径">{priceTaxText(customer.priceIncludeTax)}</Descriptions.Item>
            <Descriptions.Item label="锯纸单价">{moneyText(customer.sawPrice, '元/刀')}</Descriptions.Item>
            <Descriptions.Item label="复卷单价">{moneyText(customer.rewindPrice, '元/吨')}</Descriptions.Item>
            <Descriptions.Item label="默认开票">{invoiceText(customer.defaultInvoice, invoiceOptions)}</Descriptions.Item>
            <Descriptions.Item label="税率">{customer.taxRate == null ? '-' : `${customer.taxRate}%`}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="开票资料">
          <Descriptions column={1} size="small">
            <Descriptions.Item label="税号">{text(customer.taxNo)}</Descriptions.Item>
            <Descriptions.Item label="开户行账号">{text(customer.bankAccount)}</Descriptions.Item>
            <Descriptions.Item label="开票地址电话">{text(customer.invoiceAddress)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="物流与备注">
          <Descriptions column={1} size="small">
            <Descriptions.Item label="默认送货地址">{text(customer.deliveryAddress)}</Descriptions.Item>
            <Descriptions.Item label="备注">{text(customer.remark)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({ helper, label, value }: { helper: string; label: string; value: string }) {
  return (
    <div className="customer-detail-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <em>{helper}</em>
    </div>
  )
}

function settleText(customer: Customer, options: Array<{ label: string; value: number | string }>) {
  if (customer.settleType === 1) return optionLabel(options, customer.settleType)
  if (customer.settleType === 2) {
    const text = optionLabel(options, customer.settleType)
    return customer.settleDay ? `${text} ${customer.settleDay}日` : text
  }
  return '-'
}

function invoiceText(value: number | undefined, options: Array<{ label: string; value: number | string }>) {
  return optionLabel(options, value)
}

function optionLabel(options: Array<{ label: string; value: number | string }>, value?: number) {
  if (value == null) return '-'
  return options.find((item) => item.value === value)?.label ?? '-'
}

function priceTaxText(value?: number) {
  return value ? PRICE_TAX_TYPE[value] ?? '-' : '-'
}

function moneyText(value: number | undefined, unit: string) {
  return value == null ? '-' : `¥${value.toFixed(2)} / ${unit}`
}

function customerLevelText(value?: number) {
  if (value === 1) return 'A 重点客户'
  if (value === 2) return 'B 常规客户'
  if (value === 3) return 'C 临时客户'
  return '-'
}

function text(value?: string) {
  return value || '-'
}
