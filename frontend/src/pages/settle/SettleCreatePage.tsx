import { Card, DatePicker, Form, Input, Radio, Select, Space, message } from 'antd'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleCandidateTable from '../../features/settle/components/SettleCandidateTable'
import SettleSelectedSummary from '../../features/settle/components/SettleSelectedSummary'
import { useCreateSettleByMonth } from '../../features/settle/hooks/useCreateSettleByMonth'
import { useCreateSettleByOrders } from '../../features/settle/hooks/useCreateSettleByOrders'
import { useSettleQuoteByMonth } from '../../features/settle/hooks/useSettleQuoteByMonth'
import { useSettleQuoteByOrders } from '../../features/settle/hooks/useSettleQuoteByOrders'
import { selectedTotals } from '../../features/settle/utils/settleFormatters'
import { DICT_TYPES, invoiceFallbackOptions } from '../../features/systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../features/systemConfig/hooks/useRuntimeDictOptions'
import SettleCreateFooter from './SettleCreateFooter'
import SettleQuoteSummary from './SettleQuoteSummary'
import { useSettleCandidateSelection } from './useSettleCandidateSelection'
import '../documentModule.css'
import './SettleCreatePage.css'

interface SettleCreateForm {
  customerUuid?: string
  createMode: 'selected' | 'month'
  isInvoice: number
  period?: [Dayjs, Dayjs] | null
  remark?: string
  settleDate: Dayjs
}

export default function SettleCreatePage() {
  const [form] = Form.useForm<SettleCreateForm>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const createByOrdersMutation = useCreateSettleByOrders()
  const createByMonthMutation = useCreateSettleByMonth()
  const createMode = Form.useWatch('createMode', form) ?? 'selected'
  const customerUuid = Form.useWatch('customerUuid', form)
  const period = Form.useWatch('period', form)
  const invoiceChoice = Form.useWatch('isInvoice', form) ?? 0
  const isMonthMode = createMode === 'month'
  const canLoadCandidates = !isMonthMode || Boolean(customerUuid && period?.[0] && period?.[1])
  const selection = useSettleCandidateSelection(canLoadCandidates)
  const selectedOrderUuids = selection.selectedCandidates.map((item) => item.orderUuid)
  const invoice = invoiceChoice || undefined
  const ordersQuote = useSettleQuoteByOrders({ orderUuids: selectedOrderUuids, isInvoice: invoice },
    !isMonthMode && selectedOrderUuids.length > 0)
  const monthQuote = useSettleQuoteByMonth({
    customerUuid: customerUuid ?? '',
    periodStart: period?.[0]?.format('YYYY-MM-DD') ?? '',
    periodEnd: period?.[1]?.format('YYYY-MM-DD') ?? '',
    isInvoice: invoice,
  }, isMonthMode && canLoadCandidates)
  const quoteQuery = isMonthMode ? monthQuote : ordersQuote
  const fallbackTotals = selectedTotals(selection.selectedCandidates)
  const quote = quoteQuery.data
  const quoteRequired = isMonthMode ? canLoadCandidates : selectedOrderUuids.length > 0
  const quoteUnavailable = quoteRequired && (!quote || quoteQuery.isError || quoteQuery.isFetching)

  const handleValuesChange = (changed: Partial<SettleCreateForm>, values: SettleCreateForm) => {
    if (!candidateScopeChanged(changed)) return
    selection.setScope({
      customerUuid: values.customerUuid,
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
      periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
    })
  }

  const submitSelected = async (values: SettleCreateForm) => {
    if (selectedOrderUuids.length === 0) {
      message.warning('请先勾选需要结算的加工单')
      return
    }
    const uuid = await createByOrdersMutation.mutateAsync({
      isInvoice: invoiceValue(values.isInvoice),
      orderUuids: selectedOrderUuids,
      periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
      remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    })
    message.success('结算单已生成')
    navigate(`/settle-orders/${uuid}`)
  }

  const submitMonth = async (values: SettleCreateForm) => {
    if (!values.customerUuid || !values.period) {
      message.warning('按月结算需要选择客户和账期')
      return
    }
    const uuid = await createByMonthMutation.mutateAsync({
      customerUuid: values.customerUuid,
      isInvoice: invoiceValue(values.isInvoice),
      periodEnd: values.period[1].format('YYYY-MM-DD'),
      periodStart: values.period[0].format('YYYY-MM-DD'),
      remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    })
    message.success('月结结算单已生成')
    navigate(`/settle-orders/${uuid}`)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    await (values.createMode === 'month' ? submitMonth(values) : submitSelected(values))
  }

  return (
    <div className="document-module-page settle-create-page">
      <MesPageHeader title="新建结算单" eyebrow="结算管理" onBack={() => navigate('/settle-orders')} />
      <Card className="document-module-card settle-create-page__info" title="结算条件">
        <Form form={form} layout="vertical" initialValues={{ createMode: 'selected', isInvoice: 0, settleDate: dayjs() }}
          onValuesChange={handleValuesChange}>
          <SettleConditionFields customers={customersQuery.data?.records ?? []} loading={customersQuery.isLoading}
            invoiceOptions={invoiceOptions} />
        </Form>
      </Card>
      <Card className="document-module-card settle-create-page__selection"
        title={isMonthMode ? '账期候选预览' : '选择加工单'}
        extra={<Space wrap><Input.Search allowClear placeholder="搜索加工单或客户" onSearch={selection.setKeyword} />
          <SettleSelectedSummary label={isMonthMode ? '候选' : '已选'}
            count={quote?.orderCount ?? fallbackTotals.orderCount} amount={quote?.totalAmount ?? fallbackTotals.total} /></Space>}>
        <SettleQuoteSummary error={quoteQuery.isError} quote={quote} loading={quoteQuery.isFetching} />
        <SettleCandidateTable data={selection.candidates} loading={selection.candidatesQuery.isFetching}
          lockedCustomerUuid={isMonthMode ? undefined : selection.lockedCustomerUuid} selectable={!isMonthMode}
          scrollY="calc(100vh - 610px)" selectedRowKeys={selection.selectedRowKeys}
          onSelectionChange={selection.updateSelection} />
        <DocumentPaginationBar current={selection.query.current ?? 1} pageSize={selection.query.size ?? 20}
          total={selection.candidatesQuery.data?.total ?? 0} onChange={selection.setPage} />
      </Card>
      <SettleCreateFooter amount={quote?.totalAmount ?? fallbackTotals.total}
        count={quote?.orderCount ?? fallbackTotals.orderCount} disabled={quoteUnavailable}
        loading={createByOrdersMutation.isPending || createByMonthMutation.isPending}
        pendingPriceCount={quote?.pendingPriceCount ?? 0} onCancel={() => navigate('/settle-orders')} onSubmit={handleSubmit} />
    </div>
  )
}

function SettleConditionFields({ customers, invoiceOptions, loading }: {
  customers: { customerName: string; uuid: string }[]
  invoiceOptions: { label: string; value: number }[]
  loading: boolean
}) {
  return <div className="document-module-grid settle-create-page__form">
    <Form.Item name="createMode" label="创建方式"><Radio.Group>
      <Radio.Button value="selected">勾选加工单</Radio.Button><Radio.Button value="month">按月自动圈单</Radio.Button>
    </Radio.Group></Form.Item>
    <Form.Item name="customerUuid" label="客户"><Select allowClear showSearch loading={loading} placeholder="全部客户"
      options={customers.map((item) => ({ label: item.customerName, value: item.uuid }))} optionFilterProp="label" /></Form.Item>
    <Form.Item name="period" label="加工日期范围"><DatePicker.RangePicker /></Form.Item>
    <Form.Item name="settleDate" label="结算日期" rules={[{ required: true, message: '请选择结算日期' }]}><DatePicker /></Form.Item>
    <Form.Item name="isInvoice" label="是否开票"><Radio.Group><Radio.Button value={0}>沿用原单/客户</Radio.Button>
      {invoiceOptions.map((item) => <Radio.Button key={item.value} value={item.value}>{item.label}</Radio.Button>)}</Radio.Group></Form.Item>
    <Form.Item name="remark" label="备注"><Input maxLength={255} placeholder="结算备注" /></Form.Item>
  </div>
}

function candidateScopeChanged(changed: Partial<SettleCreateForm>) {
  return 'createMode' in changed || 'customerUuid' in changed || 'period' in changed
}

function invoiceValue(value: number) {
  return value === 1 || value === 2 ? value : undefined
}
