import { useEffect, useRef, useState } from 'react'
import { Card, Modal, Result, Spin, Steps, message } from 'antd'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import MesPageHeader from '../../components/layout/MesPageHeader'
import RollInputStep from '../../features/processOrderCreate/components/RollInputStep'
import ProcessModeStep from '../../features/processOrderCreate/components/ProcessModeStep'
import ConfigStep from '../../features/processOrderCreate/components/ConfigStep'
import { defaultPlanForRoll, rollDraftFromOriginal, toRollDto } from '../../features/processOrderCreate/draftMappers'
import { appendPreview, fromAppendConfig, toAppendConfig } from '../../features/processOrderCreate/appendPlanAdapter'
import { useMachines } from '../../features/processOrderCreate/hooks/useReferenceData'
import { queries } from '../../queries'
import type {
  PlanPreviewVO,
  ProcessOrderAppendRollVO,
  ProcessOrderAppendSessionVO,
  ProcessPlanDTO,
} from '../../types/processOrder'
import type { Machine } from '../../types/machine'
import type { RollDraft } from '../../features/processOrderCreate/types'
import {
  commitProcessOrderAppend,
  previewOriginalRollImport,
  previewProcessOrderAppend,
  previewProcessOrderAppendPlan,
  saveProcessOrderAppendPlan,
  saveProcessOrderAppendProcessSettings,
  saveProcessOrderAppendRolls,
} from '../../api/processOrder'
import AppendOrderPreviewStep from './AppendOrderPreviewStep'
import { BizError, notifyErrorOnce } from '../../api/request'
import {
  clearAppendOrderDraft,
  readAppendOrderDraft,
  writeAppendOrderDraft,
} from './appendOrderDraft'
import { mergeAppendConflictRolls, summarizeAppendConflict } from './appendOrderConflict'

const stepItems = ['原纸录入', '加工方式', '工艺配置', '预览确认'].map((title) => ({ title }))

export default function AppendOrderPage() {
  const { uuid = '' } = useParams<{ uuid: string }>()
  const [searchParams] = useSearchParams()
  const sessionUuid = searchParams.get('session') ?? ''
  const sessionQuery = useQuery({
    ...queries.processOrderAppend.session(uuid, sessionUuid),
    enabled: Boolean(uuid && sessionUuid),
  })
  const machineQuery = useMachines()
  if (!sessionUuid) return <Result status="error" title="缺少追加会话" />
  if (sessionQuery.isLoading || machineQuery.isLoading) return <Spin fullscreen />
  if (!sessionQuery.data || sessionQuery.isError || machineQuery.isError) {
    return <Result status="error" title="追加会话加载失败" subTitle="请返回加工单详情后重新发起追加。" />
  }
  return <AppendOrderContent session={sessionQuery.data} machines={machineQuery.data?.records ?? []}
    onReloadSession={async () => (await sessionQuery.refetch()).data} />
}

function AppendOrderContent({ session, machines, onReloadSession }: ContentProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const draftRef = useRef(readAppendOrderDraft(session.sessionUuid))
  const serverSessionRef = useRef(session)
  const draft = draftRef.current
  const [current, setCurrent] = useState(draft?.current ?? 0)
  const initialRolls = draft?.rolls ?? (session.rolls ?? []).map(rollDraftFromOriginal)
  const [rolls, setRolls] = useState<RollDraft[]>(initialRolls)
  const [selectedId, setSelectedId] = useState(draft?.selectedId ?? initialRolls[0]?.localId)
  const [version, setVersion] = useState(session.sessionVersion ?? 1)
  const [plans, setPlans] = useState<Record<string, ProcessPlanDTO>>(() => ({
    ...restorePlans(session.rolls), ...(draft?.plans ?? {}),
  }))
  const [previews, setPreviews] = useState<Record<string, PlanPreviewVO>>(() => restorePreviews(session.rolls))
  const [configuredIds, setConfiguredIds] = useState<string[]>(() =>
    (session.rolls ?? []).filter((roll) => roll.configStatus === 1).map((roll) => roll.uuid))
  const [pending, setPending] = useState(false)

  useEffect(() => {
    writeAppendOrderDraft({
      current, plans, rolls, savedAt: new Date().toISOString(), selectedId,
      sessionUuid: session.sessionUuid, sessionVersion: version,
    })
  }, [current, plans, rolls, selectedId, session.sessionUuid, version])

  const applyRollResponse = (response: ProcessOrderAppendSessionVO) => {
    serverSessionRef.current = response
    const saved = (response.rolls ?? []).map(rollDraftFromOriginal)
    setRolls(saved)
    setSelectedId((selected) => saved.some((roll) => roll.localId === selected) ? selected : saved[0]?.localId)
    setVersion(response.sessionVersion ?? version + 1)
    setPlans(restorePlans(response.rolls))
    setPreviews(restorePreviews(response.rolls))
    setConfiguredIds(configuredRollIds(response.rolls))
  }

  const recoverConflict = async (error: unknown) => {
    if (!(error instanceof BizError) || error.errorCode !== 'E006') return false
    const latest = await onReloadSession()
    if (!latest) {
      notifyErrorOnce(error, '追加会话已变化，最新数据加载失败，请保留本地草稿后重试')
      return true
    }
    const previous = serverSessionRef.current
    const summary = summarizeAppendConflict(previous, latest)
    Modal.confirm({
      title: '追加会话已被其他页面修改',
      content: `服务端版本已更新：新增 ${summary.added} 条、变更 ${summary.changed} 条、删除 ${summary.removed} 条。当前填写已保留为本地草稿，确认后合并并继续编辑。`,
      okText: '合并并继续编辑',
      cancelText: '稍后处理',
      onOk: () => {
        serverSessionRef.current = latest
        setRolls((local) => mergeAppendConflictRolls(previous, local, latest))
        setVersion(latest.sessionVersion ?? version)
        setPlans((local) => ({ ...restorePlans(latest.rolls), ...local }))
        setPreviews(restorePreviews(latest.rolls))
        setConfiguredIds(configuredRollIds(latest.rolls))
        message.success('已合并最新追加会话，本地草稿仍保留')
      },
    })
    return true
  }

  const run = async (work: () => Promise<void>) => {
    setPending(true)
    try {
      await work()
    } catch (error) {
      if (!(await recoverConflict(error))) throw error
    } finally {
      setPending(false)
    }
  }
  const saveRolls = () => run(async () => {
    const response = await saveProcessOrderAppendRolls(session.orderUuid, session.sessionUuid, {
      expectedSessionVersion: version, rolls: rolls.map(toRollDto),
    })
    applyRollResponse(response)
    setCurrent(1)
  })
  const saveSettings = () => run(async () => {
    const response = await saveProcessOrderAppendProcessSettings(session.orderUuid, session.sessionUuid, {
      expectedSessionVersion: version,
      rolls: rolls.filter((roll) => roll.uuid).map((roll) => ({
        rollUuid: roll.uuid!, processMode: roll.processMode ?? 1,
        mainStepType: roll.mainStepType, machineUuid: roll.machineUuid,
      })),
    })
    applyRollResponse(response)
    setCurrent(2)
  })
  const previewPlan = async (roll: RollDraft, plan: ProcessPlanDTO, signal?: AbortSignal) => {
    if (!roll.uuid) return
    try {
      const preview = await previewProcessOrderAppendPlan(
        session.orderUuid, session.sessionUuid, roll.uuid,
        { expectedSessionVersion: version, plan }, signal,
      )
      setPreviews((currentPreviews) => ({ ...currentPreviews, [roll.localId]: preview }))
    } catch (error) {
      await recoverConflict(error)
      throw error
    }
  }
  const savePlan = async (roll: RollDraft, plan: ProcessPlanDTO) => {
    if (!roll.uuid) return false
    let response: ProcessOrderAppendSessionVO
    try {
      response = await saveProcessOrderAppendPlan(session.orderUuid, session.sessionUuid, roll.uuid, {
        expectedSessionVersion: version, rollUuid: roll.uuid, config: toAppendConfig(plan),
      })
    } catch (error) {
      if (await recoverConflict(error)) return false
      throw error
    }
    const preview = response.rolls?.find((item) => item.uuid === roll.uuid)?.preview
    if (!preview) throw new Error('后端未返回追加工艺预览')
    setVersion(response.sessionVersion ?? version + 1)
    setPreviews((currentPreviews) => ({ ...currentPreviews, [roll.localId]: preview }))
    setConfiguredIds((ids) => ids.includes(roll.localId) ? ids : [...ids, roll.localId])
    return { applied: true, preview }
  }
  const savePlanBatch = async (targets: RollDraft[], plan: ProcessPlanDTO) => {
    const appliedIds: string[] = []
    let nextVersion = version
    for (const target of targets) {
      if (!target.uuid) continue
      let response: ProcessOrderAppendSessionVO
      try {
        response = await saveProcessOrderAppendPlan(session.orderUuid, session.sessionUuid, target.uuid, {
          expectedSessionVersion: nextVersion, rollUuid: target.uuid, config: toAppendConfig(plan),
        })
      } catch (error) {
        if (await recoverConflict(error)) {
          return { appliedIds, failedIds: targets.map((item) => item.localId), savedIds: appliedIds }
        }
        throw error
      }
      const preview = response.rolls?.find((item) => item.uuid === target.uuid)?.preview
      if (!preview) throw new Error('后端未返回追加工艺预览')
      nextVersion = response.sessionVersion ?? nextVersion + 1
      setPreviews((currentPreviews) => ({ ...currentPreviews, [target.localId]: preview }))
      appliedIds.push(target.localId)
    }
    setVersion(nextVersion)
    setConfiguredIds((ids) => Array.from(new Set([...ids, ...appliedIds])))
    return { appliedIds, failedIds: [], savedIds: appliedIds }
  }
  const saveServiceSteps = async (changes: Record<string, import('../../types/processOrder').ProcessStep[]>) => {
    const nextRolls = rolls.map((roll) => changes[roll.localId]
      ? { ...roll, serviceSteps: changes[roll.localId] }
      : roll)
    let response: ProcessOrderAppendSessionVO
    try {
      response = await saveProcessOrderAppendRolls(session.orderUuid, session.sessionUuid, {
        expectedSessionVersion: version,
        rolls: nextRolls.map(toRollDto),
      })
    } catch (error) {
      if (await recoverConflict(error)) return
      throw error
    }
    applyRollResponse(response)
  }
  const submit = () => run(async () => {
    const ready = await previewProcessOrderAppend(session.orderUuid, session.sessionUuid,
      { expectedSessionVersion: version })
    setVersion(ready.sessionVersion ?? version + 1)
    await commitProcessOrderAppend(session.orderUuid, session.sessionUuid, {
      expectedOrderVersion: session.baseOrderVersion ?? 1, requestId: crypto.randomUUID(),
    })
    await queryClient.invalidateQueries({ queryKey: queries.processOrderDetail._def })
    message.success('母卷及工艺已追加到原加工单')
    clearAppendOrderDraft(session.sessionUuid)
    navigate(`/process-orders/${session.orderUuid}`, { replace: true })
  })

  return (
    <div className="mes-scroll-page mes-form-page">
      <MesPageHeader backText="返回加工单" eyebrow="加工单变更" title={`追加母卷 ${session.orderNo ?? ''}`}
        onBack={() => navigate(`/process-orders/${session.orderUuid}`)} />
      <Card className="mes-form-page__steps"><Steps current={current} items={stepItems} /></Card>
      {current === 0 && <RollInputStep rolls={rolls} loading={pending} onChange={setRolls}
        onImportPreview={(file) => previewOriginalRollImport(session.orderUuid, file)}
        onPrev={() => navigate(`/process-orders/${session.orderUuid}`)} onNext={saveRolls} />}
      {current === 1 && <ProcessModeStep rolls={rolls} machines={machines} selectedId={selectedId}
        loading={pending} onSelect={setSelectedId} onChange={setRolls} onPrev={() => setCurrent(0)} onNext={saveSettings} />}
      {current === 2 && <ConfigStep autoFinishConfigEnabled={false} configuredPlanIds={configuredIds}
        orderUuid={session.orderUuid} draftVersion={version} machines={machines} plans={plans} previews={previews} rolls={rolls}
        routePreviews={{}} saving={pending} selectedId={selectedId} onSelect={setSelectedId}
        serviceStepsByRoll={serviceStepsByRoll(rolls)} onServiceStepsChange={saveServiceSteps}
        onPlanChange={(id, plan) => setPlans((currentPlans) => ({ ...currentPlans, [id]: plan }))}
        onPreviewPlan={previewPlan} onSavePlan={savePlan} onSavePlanBatch={savePlanBatch}
        onOpenRouteDesigner={() => message.info('追加母卷暂不支持链式工艺，请使用单工艺配置')}
        onPendingChange={setPending} onServiceDirtyChange={() => undefined} onDraftVersionChange={setVersion}
        onPrev={() => setCurrent(1)} onNext={() => setCurrent(3)} />}
      {current === 3 && <AppendOrderPreviewStep rolls={rolls} configuredIds={configuredIds}
        previews={previews} submitting={pending} onPrev={() => setCurrent(2)} onSubmit={submit} />}
    </div>
  )
}

interface ContentProps {
  session: ProcessOrderAppendSessionVO
  machines: Machine[]
  onReloadSession: () => Promise<ProcessOrderAppendSessionVO | undefined>
}

function restorePlans(rolls: ProcessOrderAppendRollVO[] = []) {
  return Object.fromEntries(rolls.map((source) => {
    const roll = rollDraftFromOriginal(source)
    return [roll.localId, fromAppendConfig(source) ?? defaultPlanForRoll(roll)]
  }))
}

function restorePreviews(rolls: ProcessOrderAppendRollVO[] = []) {
  return Object.fromEntries(rolls.flatMap((roll) => {
    const preview = appendPreview(roll)
    return preview && roll.uuid ? [[roll.uuid, preview]] : []
  }))
}

function configuredRollIds(rolls: ProcessOrderAppendRollVO[] = []) {
  return rolls.filter((roll) => roll.configStatus === 1 && roll.uuid).map((roll) => roll.uuid!)
}

function serviceStepsByRoll(rolls: RollDraft[]) {
  return Object.fromEntries(rolls
    .filter((roll) => roll.uuid && roll.serviceSteps?.length)
    .map((roll) => [roll.uuid!, roll.serviceSteps!]))
}
