import { Alert, Button, Descriptions, Modal, Spin, Tag } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useRuntimeVersion } from '../hooks/useRuntimeVersion'

interface Props {
  open: boolean
  onClose: () => void
}

export default function RuntimeVersionModal({ open, onClose }: Props) {
  const {
    data: runtimeVersion,
    isError: isRuntimeVersionError,
    isFetching: isFetchingRuntimeVersion,
    refetch: refetchRuntimeVersion,
  } = useRuntimeVersion(open)

  return (
    <Modal title="运行版本" open={open} footer={null} width={560} onCancel={onClose}>
      <Spin spinning={isFetchingRuntimeVersion}>
        {isRuntimeVersionError && (
          <Alert
            type="error"
            showIcon
            message="运行版本加载失败"
            description="无法确认当前前后端与数据库版本是否一致。"
            action={(
              <Button size="small" icon={<ReloadOutlined />} onClick={() => void refetchRuntimeVersion()}>
                重试
              </Button>
            )}
          />
        )}
        {runtimeVersion && (
          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="运行状态">
              <Tag color={runtimeVersion.databaseReady ? 'success' : 'error'}>
                {runtimeVersion.databaseReady ? '数据库结构就绪' : '数据库结构未同步'}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="后端版本">{runtimeVersion.backendVersion}</Descriptions.Item>
            <Descriptions.Item label="前端版本">{runtimeVersion.frontendVersion}</Descriptions.Item>
            <Descriptions.Item label="Git SHA">{runtimeVersion.gitSha}</Descriptions.Item>
            <Descriptions.Item label="构建时间">{runtimeVersion.buildTime}</Descriptions.Item>
            <Descriptions.Item label="数据库版本">
              {runtimeVersion.databaseVersion} / 预期 {runtimeVersion.expectedDatabaseVersion}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Spin>
    </Modal>
  )
}
