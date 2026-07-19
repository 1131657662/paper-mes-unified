import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'

interface Props {
  customersError: boolean
  ordersError: boolean
  summaryError: boolean
  onRetryCustomers: () => void
  onRetryOrders: () => void
  onRetrySummary: () => void
}

export default function SettleListErrors(props: Props) {
  return (
    <>
      {props.ordersError && <QueryLoadErrorAlert message="结算单加载失败"
        description="结算单未成功加载，当前空表不代表没有待收款或已结算记录。" onRetry={props.onRetryOrders} />}
      {props.customersError && <QueryLoadErrorAlert message="客户资料加载失败"
        description="客户筛选项未成功加载，当前客户选项可能不完整。" onRetry={props.onRetryCustomers} />}
      {props.summaryError && <QueryLoadErrorAlert message="结算汇总加载失败"
        description="结算汇总未成功加载，表格数据仍可继续查看。" onRetry={props.onRetrySummary} />}
    </>
  )
}
