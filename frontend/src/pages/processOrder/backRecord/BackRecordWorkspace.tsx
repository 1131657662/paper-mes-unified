import { Button, Empty, Form, Space, Spin, Typography } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import BackRecordQuickActions from './BackRecordQuickActions'
import BackRecordSummaryPanel from './BackRecordSummaryPanel'
import BackRecordWorkbench from './BackRecordWorkbench'
import { useBackRecordWorkspace } from './useBackRecordWorkspace'
import type { BackRecordFormValues } from './backRecordUtils'
import './BackRecordDrawer.css'

interface Props {
  uuid?: string | null
  enabled?: boolean
  cancelText?: string
  mode?: 'drawer' | 'page'
  onClose: () => void
  onSuccess: () => void
}

export default function BackRecordWorkspace({
  uuid,
  enabled = true,
  cancelText = '取消',
  mode = 'page',
  onClose,
  onSuccess,
}: Props) {
  const workspace = useBackRecordWorkspace({ uuid, enabled, onClose, onSuccess })

  return (
    <>
      <Spin spinning={workspace.isLoadingDetail}>
        <Form
          form={workspace.form}
          layout="vertical"
          className={`back-record-drawer back-record-workspace--${mode}`}
        >
          <WorkspaceTopbar
            cancelText={cancelText}
            onCancel={onClose}
            onSubmit={() => workspace.submit()}
            showCancel={mode === 'drawer'}
            submitting={workspace.isSubmitting}
            workspace={workspace}
          />
          {workspace.detail ? (
            <BackRecordWorkbench
              key={workspace.detail.order.uuid}
              detail={workspace.detail}
              values={workspace.values}
              onProcessChange={workspace.openChangeGuide}
            />
          ) : (
            !workspace.isLoadingDetail && <Empty description="加工单不存在或不可回录" />
          )}
        </Form>
      </Spin>
      {workspace.modals}
    </>
  )
}

function WorkspaceTopbar({
  cancelText,
  onCancel,
  onSubmit,
  showCancel,
  submitting,
  workspace,
}: {
  cancelText: string
  onCancel: () => void
  onSubmit: () => void
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
          <Typography.Text type="secondary">批量填入、现场变更和整单提交集中处理</Typography.Text>
        </div>
        <div className="back-record-commandbar__actions">
          <BackRecordQuickActions
            detail={workspace.detail ?? null}
            form={workspace.form}
            onValuesFilled={workspace.syncFilledValues}
            onOpenChange={() => workspace.openChangeGuide(null)}
          />
          <Space wrap size={[8, 8]}>
            {showCancel && <Button onClick={onCancel}>{cancelText}</Button>}
            <Button type="primary" icon={<SaveOutlined />} loading={submitting} onClick={onSubmit}>
              提交回录
            </Button>
          </Space>
        </div>
      </div>
    </div>
  )
}
