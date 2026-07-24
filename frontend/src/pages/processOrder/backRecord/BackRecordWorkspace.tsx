import { Button, Empty, Form, Space, Spin, Typography } from 'antd'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'
import BackRecordQuickActions from './BackRecordQuickActions'
import BackRecordSubmissionActions from './BackRecordSubmissionActions'
import BackRecordSummaryPanel from './BackRecordSummaryPanel'
import BackRecordWorkbench from './BackRecordWorkbench'
import BackRecordWarehouseField from './BackRecordWarehouseField'
import { useBackRecordWorkspace } from './useBackRecordWorkspace'
import type { BackRecordFormValues } from './backRecordUtils'
import { getBackRecordWorkspaceViewState } from './backRecordWorkspaceViewModel'
import './BackRecordWorkspace.css'
import './BackRecordDesktopLayout.css'

interface Props {
  uuid?: string | null
  enabled?: boolean
  cancelText?: string
  mode?: 'drawer' | 'page'
  onClose: () => void
  onDirty?: () => void
  onPersisted?: () => void
  onSuccess: () => void
}

export default function BackRecordWorkspace({
  uuid,
  enabled = true,
  cancelText = '取消',
  mode = 'page',
  onClose,
  onDirty,
  onPersisted,
  onSuccess,
}: Props) {
  const workspace = useBackRecordWorkspace({ uuid, enabled, onClose, onPersisted, onSuccess })
  const viewState = getBackRecordWorkspaceViewState({
    hasDetail: Boolean(workspace.detail),
    isDetailError: workspace.isDetailError,
    isLoadingDetail: workspace.isLoadingDetail,
  })

  return (
    <>
      <Spin spinning={workspace.isLoadingDetail}>
        {viewState === 'error' ? (
          <QueryLoadErrorAlert
            message="加工单详情加载失败"
            description="详情未成功加载，当前空白不代表加工单不存在。请重新加载后再开始回录。"
            onRetry={() => void workspace.refetchDetail()}
          />
        ) : viewState === 'ready' && workspace.detail ? (
          <Form
            form={workspace.form}
            layout="vertical"
            className={`back-record-drawer back-record-workspace--${mode}`}
            onValuesChange={onDirty}
          >
            <WorkspaceTopbar
              cancelText={cancelText}
              onCancel={onClose}
              onComplete={() => workspace.submit(true)}
              onDirty={onDirty}
              onSaveBatch={() => workspace.submit(false)}
              showCancel={mode === 'drawer'}
              submitting={workspace.isSubmitting}
              workspace={workspace}
            />
            <BackRecordWorkbench
              key={workspace.detail.order.uuid}
              detail={workspace.detail}
              values={workspace.values}
              onProcessChange={workspace.openChangeGuide}
              onToggleSelection={workspace.selection.toggle}
              selectedKeys={workspace.selection.selectedItemKeys}
            />
          </Form>
        ) : viewState === 'empty' ? <Empty description="加工单不存在或不可回录" /> : null}
      </Spin>
      {workspace.modals}
    </>
  )
}

function WorkspaceTopbar({
  cancelText,
  onCancel,
  onComplete,
  onDirty,
  onSaveBatch,
  showCancel,
  submitting,
  workspace,
}: {
  cancelText: string
  onCancel: () => void
  onComplete: () => void
  onDirty?: () => void
  onSaveBatch: () => void
  showCancel: boolean
  submitting: boolean
  workspace: ReturnType<typeof useBackRecordWorkspace>
}) {
  return (
    <div className="back-record-drawer__topbar">
      <BackRecordSummaryPanel
        detail={workspace.detail ?? null}
        values={workspace.values as BackRecordFormValues}
      />
      <div className="back-record-commandbar">
        <div className="back-record-commandbar__caption">
          <Typography.Text strong>录入操作</Typography.Text>
          <Typography.Text type="secondary">
            已选 {workspace.selection.selectedCount} / {workspace.selection.remainingCount} 个未回录母卷组
          </Typography.Text>
        </div>
        <BackRecordWarehouseField {...workspace.warehouse} />
        <div className="back-record-commandbar__actions">
          <BackRecordQuickActions
            detail={workspace.detail ?? null}
            form={workspace.form}
            onDirty={onDirty}
            onValuesFilled={workspace.syncFilledValues}
            onOpenChange={() => workspace.openChangeGuide(null)}
          />
          <Space wrap size={[8, 8]}>
            {showCancel && <Button onClick={onCancel}>{cancelText}</Button>}
            <BackRecordSubmissionActions
              allRemainingSelected={workspace.selection.allRemainingSelected}
              selectedCount={workspace.selection.selectedCount}
              submitting={submitting}
              onComplete={onComplete}
              onSaveBatch={onSaveBatch}
            />
          </Space>
        </div>
      </div>
    </div>
  )
}
