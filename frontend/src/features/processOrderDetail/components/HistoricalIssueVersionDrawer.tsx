import { Alert, Drawer, Spin } from 'antd'
import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import { printVersionProps } from '../printVersionModel'
import PrintPreviewSheet from './PrintPreviewSheet'

interface Props { issueVersion?: number; orderUuid?: string; onClose: () => void }

export default function HistoricalIssueVersionDrawer({ issueVersion, orderUuid, onClose }: Props) {
  const enabled = Boolean(orderUuid && issueVersion)
  const { data: view, isError, isLoading } = useQuery({
    ...queries.processOrderDetail.historicalIssuePrintView(orderUuid ?? '', issueVersion ?? 0),
    enabled,
  })
  return (
    <Drawer destroyOnHidden open={enabled} onClose={onClose}
      title={`历史下发 V${issueVersion ?? '-'}（只读追溯）`}
      width="min(1180px, calc(100vw - 32px))">
      <Alert showIcon type="warning" message="历史版本仅用于追溯或审计"
        description="此版本不能作为当前生产指令，系统不提供历史版本的生产打印确认。" />
      <Spin spinning={isLoading}>
        {isError ? <Alert showIcon type="error" message="历史版本加载失败" description="请刷新后重试；系统不会使用当前数据替代历史快照。" /> : null}
        {view ? <div className="print-issue__sheet-frame"><PrintPreviewSheet
          detail={view.detail} historical {...printVersionProps('ISSUED', view)} />
        </div> : null}
      </Spin>
    </Drawer>
  )
}
