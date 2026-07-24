import { Alert, Descriptions, Empty } from 'antd'
import type { FinishConfigSaveDTO, OriginalRoll, ProcessOrder } from '../../types/processOrder'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../constants/processOrder'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'
import DirectShipInfo from './DirectShipInfo'
import RewindingConfigForm from './RewindingConfigForm'
import SawingConfigForm from './SawingConfigForm'

interface Props {
  config?: FinishConfigSaveDTO
  onConfigChange: (config: FinishConfigSaveDTO) => void
  order: ProcessOrder
  originalRolls: OriginalRoll[]
  roll: OriginalRoll
}

export default function FinishConfigPanel(props: Props) {
  return (
    <div className="finish-config-panel">
      <section className="finish-config-panel__source">
        <h2>当前母卷</h2>
        <RollDescriptions roll={props.roll} />
      </section>
      <section>
        <h2 className="finish-config-panel__section-title">成品配置</h2>
        <ConfigEditor {...props} />
      </section>
    </div>
  )
}

function RollDescriptions({ roll }: { roll: OriginalRoll }) {
  const processMode = roll.processMode ?? 1
  const stepType = roll.mainStepType
  return (
    <Descriptions size="small" column={4}>
      <Descriptions.Item label="品名">{roll.paperName || '-'}</Descriptions.Item>
      <Descriptions.Item label="克重">{formatGram(roll.gramWeight)}</Descriptions.Item>
      <Descriptions.Item label="门幅">{formatMm(roll.originalWidth)}</Descriptions.Item>
      <Descriptions.Item label="单重">{formatKg(roll.rollWeight)} × {roll.pieceNum || 1} 件</Descriptions.Item>
      <Descriptions.Item label="加工模式">{PROCESS_MODE[processMode] ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="主工艺">{processModeRequiresMain(processMode) ? STEP_TYPE[stepType ?? 0] ?? '-' : '-'}</Descriptions.Item>
      <Descriptions.Item label="母卷号">{roll.rollNo || '-'}</Descriptions.Item>
    </Descriptions>
  )
}

function ConfigEditor({ config, onConfigChange, order, originalRolls, roll }: Props) {
  const processMode = roll.processMode ?? 1
  const stepType = roll.mainStepType
  if (processMode === 3) return <DirectShipInfo />
  if (processMode === 4) {
    return (
      <Alert
        showIcon
        type="info"
        message="仅附加工艺"
        description="无需配置锯纸或复卷。系统按母卷件数生成整理成品，品名、克重和门幅保持不变；完工时逐件回录实重，并记录剥损或包装损耗。"
      />
    )
  }
  if (stepType === 1) {
    return <SawingConfigForm roll={roll} processMode={processMode} config={config} onChange={onConfigChange} />
  }
  if (stepType === 2) {
    return (
      <RewindingConfigForm
        config={config}
        onChange={onConfigChange}
        orderUuid={order.uuid}
        originalRolls={originalRolls}
        processMode={processMode}
        roll={roll}
      />
    )
  }
  return <Empty description="未知的加工模式或工艺类型" />
}
