import { Typography } from 'antd'

interface Props {
  completionOnly: boolean
  completeOrder: boolean
  orderNo?: string
  selectedCount: number
  warehouseName: string
}

export function BackRecordConfirmationContent({
  completionOnly,
  completeOrder,
  orderNo,
  selectedCount,
  warehouseName,
}: Props) {
  if (completionOnly) {
    return (
      <div className="back-record-submit-confirmation">
        <Typography.Paragraph>
          加工单 <Typography.Text strong>{orderNo ?? '-'}</Typography.Text>{' '}
          当前没有待回录母卷。将仅关闭整单状态，不会重复入库，也不会改写已回录明细。
        </Typography.Paragraph>
      </div>
    )
  }
  return (
    <div className="back-record-submit-confirmation">
      <Typography.Paragraph>
        加工单 <Typography.Text strong>{orderNo ?? '-'}</Typography.Text>{' '}
        本次选择的
        <Typography.Text strong> {selectedCount} 个母卷组</Typography.Text>{' '}
        {completeOrder
          ? '将完成整单，相关成品与余料入库至：'
          : '将保存闭合结果，相关成品与余料立即入库至：'}
      </Typography.Paragraph>
      <Typography.Text strong>{warehouseName}</Typography.Text>
    </div>
  )
}
