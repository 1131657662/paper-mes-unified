import { Modal, Typography } from 'antd'

interface Params {
  orderNo?: string
  warehouseName: string
}

export function confirmBackRecordSubmission({ orderNo, warehouseName }: Params): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: '确认完成回录并入库？',
      content: (
        <div className="back-record-submit-confirmation">
          <Typography.Paragraph>
            加工单 <Typography.Text strong>{orderNo ?? '-'}</Typography.Text> 的全部有效成品与余料将入库至：
          </Typography.Paragraph>
          <Typography.Text strong>{warehouseName}</Typography.Text>
        </div>
      ),
      okText: '确认回录并入库',
      cancelText: '继续检查',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
