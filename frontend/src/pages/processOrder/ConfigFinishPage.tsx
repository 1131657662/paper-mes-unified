import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Empty, Space, Spin, message } from 'antd'
import {
  SaveOutlined,
} from '@ant-design/icons'
import { saveFinishConfig, saveFinishConfigBatch } from '../../api/processOrder'
import { notifyErrorOnce } from '../../api/request'
import MesTooltip from '../../components/biz/MesTooltip'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import { useProcessOrderDetail } from '../../features/processOrderDetail/hooks/useProcessOrderDetail'
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
  const {
    data: detail,
    isError: isDetailError,
    isLoading: isLoadingDetail,
    refetch: refetchDetail,
  } = useProcessOrderDetail(uuid)
  const [saving, setSaving] = useState(false)
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard()
  const rolls = detail?.originalRolls ?? []
  const selection = useConfigFinishSelection(rolls)
  const finishCounts = configuredFinishCounts(detail?.rollProductions ?? [])
  const sourceOnlyUuids = mergedSourceRollUuids(detail?.rollProductions ?? [])

  const saveCurrent = async () => {
    if (!uuid || !selection.currentRoll) return
    const savedUuid = selection.currentRoll.uuid
    const savedConfig = configForRoll(selection.currentRoll, selection.configs)
    setSaving(true)
    try {
      const result = await saveFinishConfig(uuid, savedUuid, savedConfig)
      message.success(`保存成功，已生成 ${result.finishRollNos?.length ?? 0} 个正式成品号`)
      selection.clearDirty([savedUuid])
      if (!selection.dirtyUuids.some((item) => item !== savedUuid)) clearDirty()
      await refetchDetail()
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
      const result = await saveFinishConfigBatch(uuid, {
        items: plan.toSave.map((roll) => ({
          rollUuid: roll.uuid,
          config: configForRoll(roll, selection.configs),
        })),
      })
      const finishCount = (result.results ?? []).reduce(
        (count, item) => count + (item.finishRollNos?.length ?? 0),
        0,
      )
      message.success(`保存成功，新生成 ${finishCount} 个正式成品号${skipSuffix(plan)}`)
      selection.clearAllDirty()
      clearDirty()
      navigate('/process-orders')
    } catch (error) {
      await refetchDetail()
      notifyErrorOnce(error, '保存未全部完成，已重新同步最新结果，请核对未配置母卷后重试')
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
    markDirty()
    message.success(`已复制配置到 ${selection.checkedUuids.length} 个母卷`)
  }

  const selectRoll = (index: number) => {
    selection.selectIndex(index)
  }

  return (
    <div className="config-finish-page">
      <MesPageHeader
        actions={(
          <HeaderActions
            disabledReason={saveAllDisabledReason({ detailLoaded: Boolean(detail), isDetailError, isLoadingDetail, rollCount: rolls.length })}
            saving={saving}
            onCancel={() => runIfClean(() => navigate('/process-orders'))}
            onSaveAll={saveAll}
          />
        )}
        description={`加工单：${detail?.order.orderNo ?? '-'} · 客户：${detail?.order.customerName ?? '-'}`}
        title="成品规格配置"
      />
      <Spin spinning={isLoadingDetail}>
        {isDetailError ? (
          <QueryLoadErrorAlert
            description="当前空白不代表加工单没有母卷，加载成功前不会执行成品配置保存。"
            message="加工单与母卷信息加载失败"
            onRetry={() => void refetchDetail()}
          />
        ) : rolls.length === 0 && detail ? (
          <div className="config-finish-empty-state">
            <Empty description="该加工单暂无可配置母卷" />
          </div>
        ) : detail ? <div className="config-finish-workspace">
          <ConfigFinishRollList
            onSelect={selectRoll}
            onToggle={selection.toggleChecked}
            rolls={rolls}
            selection={{ checkedUuids: selection.checkedUuids, selectedIndex: selection.selectedIndex }}
            status={{ finishCounts, sourceOnlyUuids }}
          />
          <ConfigFinishEditor
            actions={{
              onConfigChange: (config) => { markDirty(); selection.updateCurrentConfig(config) },
              onCopy: copyToChecked,
              onNext: () => selectRoll(selection.selectedIndex + 1),
              onPrevious: () => selectRoll(selection.selectedIndex - 1),
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
        </div> : null}
      </Spin>
    </div>
  )
}

interface HeaderActionProps {
  disabledReason?: string
  onCancel: () => void
  onSaveAll: () => void
  saving: boolean
}

export function HeaderActions(props: HeaderActionProps) {
  return (
    <Space>
      <Button onClick={props.onCancel}>取消</Button>
      <MesTooltip title={props.disabledReason}>
        <span className="config-finish-action-slot">
          <Button
            aria-label={props.disabledReason ? `保存全部并完成：${props.disabledReason}` : '保存全部并完成'}
            disabled={Boolean(props.disabledReason)}
            icon={<SaveOutlined />}
            loading={props.saving}
            type="primary"
            onClick={props.onSaveAll}
          >保存全部并完成</Button>
        </span>
      </MesTooltip>
    </Space>
  )
}

function saveAllDisabledReason(options: {
  detailLoaded: boolean
  isDetailError: boolean
  isLoadingDetail: boolean
  rollCount: number
}) {
  if (options.isLoadingDetail) return '加工单与母卷信息加载中'
  if (options.isDetailError) return '加工单信息加载失败，请重新加载'
  if (!options.detailLoaded) return '未取得加工单信息'
  if (options.rollCount === 0) return '该加工单暂无可配置母卷'
  return undefined
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
