import { Modal, Typography } from 'antd'

interface Params {
  completeOrder: boolean
  orderNo?: string
  selectedCount: number
  warehouseName: string
}

export function confirmBackRecordSubmission({ completeOrder, orderNo, selectedCount, warehouseName }: Params): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: completeOrder ? '确认完成整单并入库？' : '确认保存选中批次？',
      content: (
        <div className="back-record-submit-confirmation">
          <Typography.Paragraph>
            加工单 <Typography.Text strong>{orderNo ?? '-'}</Typography.Text> 本次选择的
            <Typography.Text strong> {selectedCount} 个母卷组</Typography.Text>
            {completeOrder ? '将完成整单，相关成品与余料入库至：' : '将保存闭合结果，相关成品与余料立即入库至：'}
          </Typography.Paragraph>
          <Typography.Text strong>{warehouseName}</Typography.Text>
        </div>
      ),
      okText: completeOrder ? '确认完成整单' : '确认保存本批',
      cancelText: '继续检查',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
