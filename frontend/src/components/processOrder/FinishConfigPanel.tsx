import { Button, Card, Descriptions, Divider, Space, Typography } from 'antd'
import type { FinishConfigSaveDTO, OriginalRoll, ProcessOrder } from '../../types/processOrder'
import { PROCESS_MODE, STEP_TYPE } from '../../constants/processOrder'
import SawingConfigForm from './SawingConfigForm'
import RewindingConfigForm from './RewindingConfigForm'
import DirectShipInfo from './DirectShipInfo'

interface Props {
  roll: OriginalRoll
  originalRolls: OriginalRoll[]
  order: ProcessOrder
  config?: FinishConfigSaveDTO
  onCopyToSelected: () => void
  selectedCount: number
  onConfigChange: (config: FinishConfigSaveDTO) => void
  onSaveCurrent: () => void
}

export default function FinishConfigPanel({
  roll,
  originalRolls,
  order,
  config,
  onCopyToSelected,
  selectedCount,
  onConfigChange,
  onSaveCurrent,
}: Props) {
  const processMode = roll.processMode ?? 1
  const mainStepType = roll.mainStepType ?? 1

  const renderConfigForm = () => {
    if (processMode === 3) {
      return <DirectShipInfo />
    }

    if (processMode === 1 || processMode === 2) {
      if (mainStepType === 1) {
        return <SawingConfigForm roll={roll} processMode={processMode} config={config} onChange={onConfigChange} />
      }
      if (mainStepType === 2) {
        return (
          <RewindingConfigForm
            orderUuid={order.uuid}
            roll={roll}
            originalRolls={originalRolls}
            processMode={processMode}
            config={config}
            onChange={onConfigChange}
          />
        )
      }
    }

    return (
      <div style={{ textAlign: 'center', padding: 48, color: '#999' }}>
        未知的加工模式或工艺类型
      </div>
    )
  }

  return (
    <div>
      <Card size="small" title="原纸信息" style={{ marginBottom: 16 }}>
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="品名">{roll.paperName}</Descriptions.Item>
          <Descriptions.Item label="克重">{roll.gramWeight}g</Descriptions.Item>
          <Descriptions.Item label="门幅">{roll.originalWidth}mm</Descriptions.Item>
          <Descriptions.Item label="单重">
            {roll.rollWeight}kg × {roll.pieceNum || 1}件
          </Descriptions.Item>
          <Descriptions.Item label="加工模式">{PROCESS_MODE[processMode] ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="主工艺">
            {mainStepType ? STEP_TYPE[mainStepType] ?? '-' : '-'}
          </Descriptions.Item>
          {roll.rollNo && <Descriptions.Item label="母卷号">{roll.rollNo}</Descriptions.Item>}
        </Descriptions>
      </Card>

      <Divider orientation="left">
        <Typography.Text strong>成品配置</Typography.Text>
      </Divider>

      {renderConfigForm()}

      {processMode !== 3 && (
        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
          <Space>
            <Button type="primary" onClick={onSaveCurrent}>保存此原纸配置</Button>
            {selectedCount > 0 && (
              <Button onClick={onCopyToSelected}>复制到选中原纸 ({selectedCount})</Button>
            )}
          </Space>
        </div>
      )}
    </div>
  )
}
