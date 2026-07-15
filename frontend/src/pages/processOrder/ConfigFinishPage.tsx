import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Space, Spin, message } from 'antd'
import {
  SaveOutlined,
} from '@ant-design/icons'
import { saveFinishConfig } from '../../api/processOrder'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useProcessOrderDetail } from '../../features/processOrderDetail/hooks/useProcessOrderDetail'
import type { FinishConfigSaveDTO, OriginalRoll } from '../../types/processOrder'
import ConfigFinishEditor from './ConfigFinishEditor'
import ConfigFinishRollList from './ConfigFinishRollList'
import {
  buildDefaultConfig,
  buildSavePlan,
  configForRoll,
  configuredFinishCounts,
  mergedSourceRollUuids,
  type ConfigFinishSavePlan,
} from './configFinishModel'
import { useConfigFinishSelection } from './useConfigFinishSelection'
import './ConfigFinishPage.css'

export default function ConfigFinishPage() {
  const { uuid } = useParams<{ uuid: string }>()
  const navigate = useNavigate()
  const { data: detail, isLoading, refetch } = useProcessOrderDetail(uuid)
  const [saving, setSaving] = useState(false)
  const rolls = detail?.originalRolls ?? []
  const selection = useConfigFinishSelection(rolls)
  const finishCounts = configuredFinishCounts(detail?.rollProductions ?? [])
  const sourceOnlyUuids = mergedSourceRollUuids(detail?.rollProductions ?? [])

  const saveCurrent = async () => {
    if (!uuid || !selection.currentRoll) return
    setSaving(true)
    try {
      const result = await saveFinishConfig(
        uuid,
        selection.currentRoll.uuid,
        configForRoll(selection.currentRoll, selection.configs),
      )
      message.success(`保存成功，已生成 ${result.finishRollNos?.length ?? 0} 个正式成品号`)
      await refetch()
    } finally {
      setSaving(false)
    }
  }

  const saveAll = async () => {
    if (!uuid) return
    const plan = buildSavePlan(rolls, finishCounts, sourceOnlyUuids)
    if (plan.toSave.length === 0) return void message.info(emptyPlanMessage(plan))
    setSaving(true)
    try {
      const finishCount = await saveRolls(uuid, plan.toSave, selection.configs)
      message.success(`保存成功，新生成 ${finishCount} 个正式成品号${skipSuffix(plan)}`)
      navigate('/process-orders')
    } finally {
      setSaving(false)
    }
  }

  const copyToChecked = () => {
    if (!selection.currentRoll || selection.checkedUuids.length === 0) {
      message.warning('请先勾选要复制到的母卷')
      return
    }
    const config = configForRoll(selection.currentRoll, selection.configs)
    selection.copyToChecked(config)
    message.success(`已复制配置到 ${selection.checkedUuids.length} 个母卷`)
  }

  return (
    <div className="config-finish-page">
      <MesPageHeader
        actions={<HeaderActions saving={saving} onCancel={() => navigate('/process-orders')} onSaveAll={saveAll} />}
        description={`加工单：${detail?.order.orderNo ?? '-'} · 客户：${detail?.order.customerName ?? '-'}`}
        title="成品规格配置"
      />
      <Spin spinning={isLoading}>
        <div className="config-finish-workspace">
          <ConfigFinishRollList
            onSelect={selection.selectIndex}
            onToggle={selection.toggleChecked}
            rolls={rolls}
            selection={{ checkedUuids: selection.checkedUuids, selectedIndex: selection.selectedIndex }}
            status={{ finishCounts, sourceOnlyUuids }}
          />
          <ConfigFinishEditor
            actions={{
              onConfigChange: selection.updateCurrentConfig,
              onCopy: copyToChecked,
              onNext: () => selection.selectIndex(selection.selectedIndex + 1),
              onPrevious: () => selection.selectIndex(selection.selectedIndex - 1),
              onSave: saveCurrent,
            }}
            detail={detail}
            state={{
              checkedCount: selection.checkedUuids.length,
              config: selection.currentRoll ? selection.configs[selection.currentRoll.uuid] ?? buildDefaultConfig(selection.currentRoll) : undefined,
              currentRoll: selection.currentRoll,
              saving,
              selectedIndex: selection.selectedIndex,
            }}
          />
        </div>
      </Spin>
    </div>
  )
}

function HeaderActions(props: { saving: boolean; onCancel: () => void; onSaveAll: () => void }) {
  return (
    <Space>
      <Button onClick={props.onCancel}>取消</Button>
      <Button icon={<SaveOutlined />} loading={props.saving} type="primary" onClick={props.onSaveAll}>保存全部并完成</Button>
    </Space>
  )
}

async function saveRolls(uuid: string, rolls: OriginalRoll[], configs: Record<string, FinishConfigSaveDTO>) {
  let count = 0
  for (const roll of rolls) {
    const result = await saveFinishConfig(uuid, roll.uuid, configForRoll(roll, configs))
    count += result.finishRollNos?.length ?? 0
  }
  return count
}

function emptyPlanMessage(plan: ConfigFinishSavePlan) {
  const parts = skipParts(plan)
  return parts.length ? `无需保存：${parts.join('；')}` : '没有需要保存的母卷'
}

function skipSuffix(plan: ConfigFinishSavePlan) {
  const parts = skipParts(plan)
  return parts.length ? `；${parts.join('；')}` : ''
}

function skipParts(plan: ConfigFinishSavePlan) {
  const parts: string[] = []
  if (plan.skippedConfigured.length) parts.push(`${plan.skippedConfigured.join('、')} 已配置（跳过）`)
  if (plan.skippedSources.length) parts.push(`${plan.skippedSources.join('、')} 为合并来源（跳过）`)
  return parts
}
