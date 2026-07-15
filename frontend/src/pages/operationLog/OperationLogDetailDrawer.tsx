import { LinkOutlined } from '@ant-design/icons'
import { Button, Descriptions, Drawer, Typography } from 'antd'
import type { OperationLog } from '../../types/operationLog'
import { actionTag, dateText, logText } from './operationLogDisplay'

interface Props {
  log?: OperationLog
  businessPath?: string
  onClose: () => void
  onOpenBusiness: (path: string) => void
}

export default function OperationLogDetailDrawer({ businessPath, log, onClose, onOpenBusiness }: Props) {
  return (
    <Drawer
      className="mes-detail-drawer operation-log-drawer"
      title="日志详情"
      open={!!log}
      width={760}
      onClose={onClose}
      destroyOnHidden
      extra={businessPath && (
        <Button icon={<LinkOutlined />} onClick={() => onOpenBusiness(businessPath)}>
          打开关联业务
        </Button>
      )}
    >
      {log && (
        <div className="operation-log-drawer__body">
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="操作时间">{dateText(log.operateTime)}</Descriptions.Item>
            <Descriptions.Item label="操作人">{logText(log.operator)}</Descriptions.Item>
            <Descriptions.Item label="业务类型">{logText(log.bizType)}</Descriptions.Item>
            <Descriptions.Item label="业务单号">{logText(log.bizNo)}</Descriptions.Item>
            <Descriptions.Item label="动作类型">{actionTag(log.actionType)}</Descriptions.Item>
            <Descriptions.Item label="字段名">{logText(log.fieldName)}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{logText(log.remark)}</Descriptions.Item>
          </Descriptions>

          <section className="operation-log-drawer__diff">
            <h3>变更内容</h3>
            <div>
              <ValueBlock title="修改前" value={log.oldValue} />
              <ValueBlock title="修改后" value={log.newValue} strong />
            </div>
          </section>
        </div>
      )}
    </Drawer>
  )
}

function ValueBlock({ strong, title, value }: { strong?: boolean; title: string; value?: string }) {
  return (
    <div className={strong ? 'operation-log-value operation-log-value--strong' : 'operation-log-value'}>
      <span>{title}</span>
      <Typography.Paragraph copyable={!!value}>{logText(value)}</Typography.Paragraph>
    </div>
  )
}
