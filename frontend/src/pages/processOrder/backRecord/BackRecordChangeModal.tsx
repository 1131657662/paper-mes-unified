import { Alert, Button, Modal, Popconfirm, Space, Typography } from 'antd'
import { PlusOutlined, RollbackOutlined } from '@ant-design/icons'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  open: boolean
  detail: ProcessOrderDetailVO | null
  item: BackRecordWorkItem | null
  rollingBack?: boolean
  onCancel: () => void
  onAddExtraStep: () => void
  onRollbackToConfig: () => void
}

export default function BackRecordChangeModal({
  open,
  detail,
  item,
  rollingBack,
  onCancel,
  onAddExtraStep,
  onRollbackToConfig,
}: Props) {
  const canRollback = detail?.order.orderStatus === 3

  return (
    <Modal
      title="现场变更处理"
      open={open}
      onCancel={onCancel}
      footer={(
        <div className="mes-drawer-footer">
          <Button onClick={onCancel}>关闭</Button>
        </div>
      )}
      width={800}
      destroyOnHidden
    >
      <div className="back-record-change">
        <Alert
          showIcon
          type="info"
          message="回录记录实际结果；会改变成品号、来源关系、打印快照的主方案变更，需要先回退待下发再重配。"
        />
        <div className="back-record-change__current">
          <Typography.Text type="secondary">当前定位</Typography.Text>
          <Typography.Text strong>{item?.title || detail?.order.orderNo || '整单'}</Typography.Text>
        </div>
        <div className="back-record-change__grid">
          <ChangeCard
            title="实际值偏差"
            description="克重、门幅、复称重量、成品实重、损耗和异常说明，直接在当前回录页录入。"
          />
          <ChangeCard
            title="现场追加工序"
            description="例如主工艺复卷后，车间又锯掉水湿边；这类不重生成品号的追加工序可在回录时补记并参与计费。"
            action={
              <Button type="primary" icon={<PlusOutlined />} onClick={onAddExtraStep}>
                记录追加工序
              </Button>
            }
          />
          <ChangeCard
            title="主方案改动"
            description="例如复卷改锯纸、成品规格/数量变化、改门幅+改直径变成只改门幅；这些会影响卷号、来源、快照和计费。"
            action={
              <Popconfirm
                title="确认回退到待下发？"
                description="回退后需要重新配置工艺并再次下发，避免回录数据和打印快照不一致。"
                onConfirm={onRollbackToConfig}
                disabled={!canRollback}
              >
                <Button danger icon={<RollbackOutlined />} loading={rollingBack} disabled={!canRollback}>
                  回退待下发重配
                </Button>
              </Popconfirm>
            }
          />
        </div>
      </div>
    </Modal>
  )
}

function ChangeCard({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: React.ReactNode
}) {
  return (
    <section className="back-record-change-card">
      <div>
        <Typography.Text strong>{title}</Typography.Text>
        <Typography.Paragraph type="secondary">{description}</Typography.Paragraph>
      </div>
      {action && <Space>{action}</Space>}
    </section>
  )
}
