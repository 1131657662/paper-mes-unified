import { Modal } from 'antd'
import { BackRecordConfirmationContent } from './BackRecordConfirmationContent'

interface Params {
  completeOrder: boolean
  orderNo?: string
  selectedCount: number
  warehouseName: string
}

interface ConditionalParams extends Params {
  skipConfirmation: boolean
}

export function confirmBackRecordIfRequired(
  params: ConditionalParams,
): Promise<boolean> {
  if (params.skipConfirmation) return Promise.resolve(true)
  return confirmBackRecordSubmission(params)
}

export function confirmBackRecordSubmission({
  completeOrder,
  orderNo,
  selectedCount,
  warehouseName,
}: Params): Promise<boolean> {
  const completionOnly = completeOrder && selectedCount === 0
  return new Promise((resolve) => {
    Modal.confirm({
      title: completionOnly
        ? '确认关闭整单？'
        : completeOrder
          ? '确认完成整单并入库？'
          : '确认保存选中批次？',
      content: (
        <BackRecordConfirmationContent
          completionOnly={completionOnly}
          completeOrder={completeOrder}
          orderNo={orderNo}
          selectedCount={selectedCount}
          warehouseName={warehouseName}
        />
      ),
      okText: completeOrder ? '确认完成整单' : '确认保存本批',
      cancelText: '继续检查',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
