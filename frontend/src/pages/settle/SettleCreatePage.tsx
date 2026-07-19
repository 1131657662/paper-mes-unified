import { Card, Form, Input, Space, message } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
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
import SettleConditionFields from './SettleConditionFields'
import type { SettleCreateForm } from './SettleConditionFields'
import SettleCreateFooter from './SettleCreateFooter'
import SettleQuoteSummary from './SettleQuoteSummary'
import SettleSelectionNotice from './SettleSelectionNotice'
import { applyQuoteLines } from './settleQuoteModel'
import { isCandidateEligibilityError } from './settleQuoteErrorModel'
import { settleListReturnTarget } from './settleListNavigation'
import { useSettleCandidateSelection } from './useSettleCandidateSelection'
import '../documentModule.css'
import './SettleCreatePage.css'

export default function SettleCreatePage() {
  const [form] = Form.useForm<SettleCreateForm>()
  const location = useLocation()
  const navigate = useNavigate()
  const returnTo = settleListReturnTarget(location.state)
  const requestIdRef = useRef(crypto.randomUUID())
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
  const { clearSelection, refreshCandidates } = selection
  const handledQuoteErrorAtRef = useRef(0)
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
  const quotedCandidates = applyQuoteLines(selection.candidates, quote?.lines)
  const quoteRequired = isMonthMode ? canLoadCandidates : selectedOrderUuids.length > 0
  const quoteUnavailable = quoteRequired && (!quote || quoteQuery.isError || quoteQuery.isFetching)
  const candidateTotal = canLoadCandidates ? selection.candidatesQuery.data?.total ?? 0 : 0
  const awaitingMonthScope = isMonthMode && !canLoadCandidates
  const emptyText = awaitingMonthScope ? '请选择客户和归属日期范围后查看候选' : undefined
  const quoteEmptyText = awaitingMonthScope ? '选择客户和归属日期范围后显示准确试算' : '选择加工单后显示准确试算'

  useEffect(() => {
    if (!quoteQuery.isError || quoteQuery.errorUpdatedAt <= handledQuoteErrorAtRef.current) return
    if (isMonthMode || !isCandidateEligibilityError(quoteQuery.error)) return
    handledQuoteErrorAtRef.current = quoteQuery.errorUpdatedAt
    clearSelection()
    void refreshCandidates()
  }, [isMonthMode, quoteQuery.error, quoteQuery.errorUpdatedAt, quoteQuery.isError,
    clearSelection, refreshCandidates])

  const handleValuesChange = (changed: Partial<SettleCreateForm>, values: SettleCreateForm) => {
    if (!candidateScopeChanged(changed)) return
    selection.setScope({
      customerUuid: values.customerUuid,
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
      periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
    })
  }

  const recoverFromEligibilityError = async (error: unknown) => {
    if (!isCandidateEligibilityError(error)) return
    clearSelection()
    await refreshCandidates()
    if (isMonthMode) await quoteQuery.refetch()
    message.info('候选加工单状态已变化，页面已刷新，请重新确认')
  }

  const submitSelected = async (values: SettleCreateForm) => {
    if (selectedOrderUuids.length === 0) {
      message.warning('请先勾选需要结算的加工单')
      return
    }
    const uuid = await createByOrdersMutation.mutateAsync({
      requestId: requestIdRef.current, quoteVersion: requireQuote().quoteVersion,
      quoteHash: requireQuote().quoteHash, isInvoice: invoiceValue(values.isInvoice),
      orderUuids: selectedOrderUuids, periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'), remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    }).catch(async (error: unknown) => {
      await recoverFromEligibilityError(error)
      throw error
    })
    message.success('结算单已生成')
    navigate(`/settle-orders/${uuid}`, { state: { from: returnTo } })
  }

  const submitMonth = async (values: SettleCreateForm) => {
    if (!values.customerUuid || !values.period) {
      message.warning('按账期结算需要选择客户和归属日期范围')
      return
    }
    const uuid = await createByMonthMutation.mutateAsync({
      requestId: requestIdRef.current, quoteVersion: requireQuote().quoteVersion,
      quoteHash: requireQuote().quoteHash, customerUuid: values.customerUuid,
      isInvoice: invoiceValue(values.isInvoice), periodEnd: values.period[1].format('YYYY-MM-DD'),
      periodStart: values.period[0].format('YYYY-MM-DD'), remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    }).catch(async (error: unknown) => {
      await recoverFromEligibilityError(error)
      throw error
    })
    message.success('账期结算单已生成')
    navigate(`/settle-orders/${uuid}`, { state: { from: returnTo } })
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    await (values.createMode === 'month' ? submitMonth(values) : submitSelected(values))
  }

  const requireQuote = () => {
    if (!quote) throw new Error('结算报价尚未就绪')
    return quote
  }

  return (
    <div className="document-module-page settle-create-page">
      <MesPageHeader title="新建结算单" eyebrow="结算管理" onBack={() => navigate(returnTo)} />
      <Card className="document-module-card settle-create-page__info" title="结算条件">
        <Form form={form} layout="vertical" initialValues={{ createMode: 'selected', isInvoice: 0, settleDate: dayjs() }}
          onValuesChange={handleValuesChange}>
          <SettleConditionFields customers={customersQuery.data?.records ?? []} loading={customersQuery.isLoading}
            invoiceOptions={invoiceOptions} isMonthMode={isMonthMode} />
        </Form>
      </Card>
      <Card className={
        `document-module-card settle-create-page__selection${isMonthMode ? ' settle-create-page__selection--month' : ''}`
      }
        title={isMonthMode ? '账期候选预览' : '选择加工单'}
        extra={<Space wrap><Input.Search allowClear disabled={awaitingMonthScope}
          placeholder={awaitingMonthScope ? '请先选择客户和日期范围' : '搜索加工单或客户'} onSearch={selection.setKeyword} />
          <SettleSelectedSummary label={isMonthMode ? '候选' : '已选'}
            count={quote?.orderCount ?? fallbackTotals.orderCount} amount={quote?.totalAmount ?? fallbackTotals.total} /></Space>}>
        <SettleQuoteSummary error={quoteQuery.isError} quote={quote} loading={quoteQuery.isFetching}
          emptyText={quoteEmptyText} onRetry={() => void quoteQuery.refetch()} />
        {!isMonthMode && <SettleSelectionNotice selectedCount={selection.selectedCandidates.length}
          customerName={selection.selectedCandidates[0]?.customerName} onClear={selection.clearSelection} />}
        {canLoadCandidates && selection.candidatesQuery.isError && (
          <QueryLoadErrorAlert message="结算候选加载失败"
            description="本次未取得候选加工单，当前空表不代表没有可结算数据。"
            onRetry={() => void selection.candidatesQuery.refetch()} />
        )}
        <SettleCandidateTable data={quotedCandidates} loading={selection.candidatesQuery.isFetching}
          lockedCustomerUuid={isMonthMode ? undefined : selection.lockedCustomerUuid} selectable={!isMonthMode}
          emptyText={emptyText} scrollY="100%" selectedRowKeys={selection.selectedRowKeys}
          onSelectionChange={selection.updateSelection} />
        {!awaitingMonthScope && (
          <DocumentPaginationBar current={selection.query.current ?? 1} pageSize={selection.query.size ?? 20}
            total={candidateTotal} onChange={selection.setPage} />
        )}
      </Card>
      <SettleCreateFooter amount={quote?.totalAmount ?? fallbackTotals.total}
        count={quote?.orderCount ?? fallbackTotals.orderCount} disabled={quoteUnavailable}
        loading={createByOrdersMutation.isPending || createByMonthMutation.isPending}
        pendingPriceCount={quote?.pendingPriceCount ?? 0} onCancel={() => navigate(returnTo)} onSubmit={handleSubmit} />
    </div>
  )
}

function candidateScopeChanged(changed: Partial<SettleCreateForm>) {
  return 'createMode' in changed || 'customerUuid' in changed || 'period' in changed
}

function invoiceValue(value: number) {
  return value === 1 || value === 2 ? value : undefined
}
