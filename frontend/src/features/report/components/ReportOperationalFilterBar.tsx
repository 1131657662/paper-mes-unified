import { ExportOutlined, ReloadOutlined, SearchOutlined } from '@ant-design/icons'
import { Button, DatePicker, Form, Select } from 'antd'
import type { ReactNode } from 'react'
import type { Customer } from '../../../types/customer'
import type { Paper } from '../../../types/paper'
import type { ReportOperationalTopicCode } from '../../../types/reportOperational'
import type { ReportFilterValues } from './ReportFilterBar'
import ReportCustomerFilter from './ReportCustomerFilter'
import ReportPaperFilter from './ReportPaperFilter'
import ReportPeriodPreset from './ReportPeriodPreset'
import { DICT_TYPES, invoiceFallbackOptions, settleFallbackOptions } from '../../systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../systemConfig/hooks/useRuntimeDictOptions'

interface Props {
  actions: {
    export: () => void
    refresh: () => void
    submit: (values: ReportFilterValues) => void
  }
  data: { customers: Customer[]; papers: Paper[] }
  headerActions?: ReactNode
  initialValues: ReportFilterValues
  status: { exporting: boolean; loading: boolean }
  topic: ReportOperationalTopicCode
}

export default function ReportOperationalFilterBar(props: Props) {
  const [form] = Form.useForm<ReportFilterValues>()
  const invoice = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const settle = useNumberDictOptions(DICT_TYPES.settleType, settleFallbackOptions)
  const financial = props.topic === 'settlement' || props.topic === 'collection'
  const applyPeriod = (period: ReportFilterValues['period']) => {
    form.setFieldValue('period', period)
    form.submit()
  }
  return <div className="report-header report-operational-filter">
    <p className="report-header__description">{descriptions[props.topic]}</p>
    <div className="report-header__actions">
      {props.headerActions}
      <Button icon={<ExportOutlined />} loading={props.status.exporting}
        onClick={props.actions.export}>导出</Button>
      <Button icon={<ReloadOutlined />} onClick={props.actions.refresh}>刷新</Button>
    </div>
    <Form form={form} initialValues={props.initialValues} layout="vertical" onFinish={props.actions.submit}>
      <div className="report-operational-filter__fields">
        <Form.Item className="report-filter__period" label={periodLabels[props.topic]}>
          <div className="report-filter__period-control">
            <ReportPeriodPreset onApply={applyPeriod} />
            <Form.Item name="period" noStyle><DatePicker.RangePicker allowClear={false} /></Form.Item>
          </div>
        </Form.Item>
        <Form.Item label="客户" name="customerUuid">
          <ReportCustomerFilter initial={props.data.customers} loading={props.status.loading} />
        </Form.Item>
        {props.topic === 'inventory' && <Form.Item label="产品 / 品名" name="paperName">
          <ReportPaperFilter initial={props.data.papers} />
        </Form.Item>}
        {financial && <Form.Item label="结算方式" name="settleType">
          <Select allowClear options={settle.options} placeholder="全部方式" />
        </Form.Item>}
        {financial && <Form.Item label="开票" name="isInvoice">
          <Select allowClear options={invoice.options} placeholder="全部" />
        </Form.Item>}
        <Form.Item className="report-filter__actions" label=" ">
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>查询</Button>
        </Form.Item>
      </div>
    </Form>
  </div>
}

const descriptions: Record<ReportOperationalTopicCode, string> = {
  settlement: '按结算日期分析有效结算单、应收覆盖、未收余额和逾期风险。',
  collection: '按实际到账时间拆分现金、废纸抵扣与优惠核销，避免混淆回款口径。',
  inventory: '查看当前在库快照，并按入库月份和仓库定位库存结构与锁定占用。',
  delivery: '按出库单日期分析待出库、已签收和仓库履约结构。',
}

const periodLabels: Record<ReportOperationalTopicCode, string> = {
  settlement: '结算日期', collection: '到账日期', inventory: '入库日期', delivery: '出库日期',
}
