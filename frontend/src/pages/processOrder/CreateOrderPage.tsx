import { useEffect, useRef } from 'react'
import { Card, Spin, Steps } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import BaseInfoStep from '../../features/processOrderCreate/components/BaseInfoStep'
import ConfigStep from '../../features/processOrderCreate/components/ConfigStep'
import PreviewStep from '../../features/processOrderCreate/components/PreviewStep'
import ProcessModeStep from '../../features/processOrderCreate/components/ProcessModeStep'
import RollInputStep from '../../features/processOrderCreate/components/RollInputStep'
import { useCreateOrderPage } from '../../features/processOrderCreate/hooks/useCreateOrderPage'

const steps = ['基础信息', '原纸录入', '加工方式', '工艺配置', '预览确认']

export default function CreateOrderPage() {
  const [searchParams] = useSearchParams()
  const draftUuid = searchParams.get('draft') ?? undefined
  const freshToken = searchParams.get('fresh')
  const pageKey = draftUuid ?? freshToken ?? 'new'

  return <CreateOrderContent key={pageKey} draftUuid={draftUuid} resetLocalDraft={Boolean(freshToken)} />
}

function CreateOrderContent({ draftUuid, resetLocalDraft }: { draftUuid?: string; resetLocalDraft: boolean }) {
  const navigate = useNavigate()
  const pageRef = useRef<HTMLDivElement | null>(null)
  const state = useCreateOrderPage(draftUuid, { resetLocalDraft })
  const discardedSnapshot = useRef<ReturnType<typeof state.captureSnapshot>>()
  const discardChanges = () => {
    if (!discardedSnapshot.current) return
    state.restoreSnapshot(discardedSnapshot.current)
    discardedSnapshot.current = undefined
  }
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard({ onDiscard: discardChanges })
  const markEdited = () => {
    if (!discardedSnapshot.current) discardedSnapshot.current = state.captureSnapshot()
    markDirty()
  }
  const clearDirtyAfterSuccess = () => {
    discardedSnapshot.current = undefined
    clearDirty()
  }
  const createAnother = () => navigate(`/process-orders/create?fresh=${Date.now()}`, { replace: true })
  const handleSubmit = async () => {
    if (await state.handleSubmit()) clearDirtyAfterSuccess()
  }

  useEffect(() => {
    scrollCreatePageToTop(pageRef.current)
  }, [state.current, state.submitResult?.orderUuid])

  if (state.loadError) {
    return (
      <CreateOrderLoadError
        kind={state.loadError}
        onBack={() => navigate('/process-orders')}
        onRetry={() => void state.retryLoad()}
      />
    )
  }

  if (state.loadingPage) {
    return <Spin wrapperClassName="mes-spin-fill process-order-create-spin" spinning />
  }

  return (
    <Spin wrapperClassName="mes-spin-fill process-order-create-spin" spinning={state.loadingPage}>
      <div ref={pageRef} className="mes-scroll-page mes-form-page">
        <MesPageHeader
          backText="返回列表"
          eyebrow="加工单"
          onBack={() => runIfClean(() => navigate('/process-orders'))}
          title="新建加工单"
        />
        <Card className="mes-form-page__steps">
          <Steps current={state.current} items={steps.map((title) => ({ title }))} />
        </Card>
        {state.current === 0 && (
          <BaseInfoStep
            customers={state.customerOptions}
            warehouses={state.warehouseOptions}
            initialValue={state.baseInfo}
            loading={state.creatingDraft || state.savingBase}
            onChange={(value) => { markEdited(); state.handleBaseInfoChange(value) }}
            onNext={async (value) => { if (await state.handleBaseNext(value)) clearDirtyAfterSuccess() }}
          />
        )}
        {state.current === 1 && (
          <RollInputStep
            rolls={state.rolls}
            loading={state.savingRolls}
            onChange={(value) => { markEdited(); state.setRolls(value) }}
            onImportPreview={state.handleImportPreview}
            onPrev={() => runIfClean(() => state.setCurrent(0))}
            onNext={async () => { if (await state.handleRollsNext()) clearDirtyAfterSuccess() }}
          />
        )}
        {state.current === 2 && (
          <ProcessModeStep
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            loading={state.updatingRolls}
            onSelect={(value) => runIfClean(() => state.setSelectedId(value))}
            onChange={(value) => { markEdited(); state.setRolls(value) }}
            onPrev={() => runIfClean(() => state.setCurrent(1))}
            onNext={async () => { if (await state.handleProcessNext()) clearDirtyAfterSuccess() }}
          />
        )}
        {state.current === 3 && (
          <ConfigStep
            defaultSpareCount={state.defaultSpareCount}
            defaultPlanOptions={state.defaultPlanOptions}
            orderUuid={state.orderUuid}
            customerPrices={state.customerProcessPrices}
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            plans={state.plans}
            previews={state.previews}
            routePreviews={state.routePreviews}
            saving={state.savingWorkbench}
            onOpenRouteDesigner={(roll) => {
              if (state.orderUuid && roll.uuid) {
                runIfClean(() => navigate(`/process-orders/create/${state.orderUuid}/routes/${roll.uuid}`))
              }
            }}
            onSelect={(value) => runIfClean(() => state.setSelectedId(value))}
            onPlanChange={(localId, plan) => { markEdited(); state.handlePlanChange(localId, plan) }}
            onPreviewPlan={state.handlePreviewPlan}
            onSavePlan={async (roll, plan) => { if (await state.handleSavePlan(roll, plan)) clearDirtyAfterSuccess() }}
            onSavePlanBatch={async (rolls, plan) => { if (await state.handleSavePlanBatch(rolls, plan)) clearDirtyAfterSuccess() }}
            onPrev={() => runIfClean(() => state.setCurrent(2))}
            onNext={async () => { if (await state.handleConfigNext()) clearDirtyAfterSuccess() }}
          />
        )}
        {state.current === 4 && (
          <PreviewStep
            rolls={state.rolls}
            plans={state.plans}
            previews={state.previews}
            routePreviews={state.routePreviews}
            serviceConfigured={state.serviceConfigured}
            submitting={state.submitting}
            submitResult={state.submitResult}
            onBackToList={() => runIfClean(() => navigate('/process-orders'))}
            onCreateAnother={createAnother}
            onPrev={() => runIfClean(() => state.setCurrent(3))}
            onEditRoll={(localId) => {
              runIfClean(() => {
                state.setSelectedId(localId)
                state.setCurrent(3)
              })
            }}
            onSubmit={() => void handleSubmit()}
            onViewDetail={(orderUuid) => navigate(`/process-orders/${orderUuid}`)}
          />
        )}
      </div>
    </Spin>
  )
}

export function CreateOrderLoadError({ kind, onBack, onRetry }: {
  kind: 'draft' | 'reference' | 'settings'
  onBack: () => void
  onRetry: () => void
}) {
  const isDraft = kind === 'draft'
  const isSettings = kind === 'settings'
  return (
    <div className="mes-scroll-page mes-form-page">
      <MesPageHeader backText="返回列表" eyebrow="加工单" title="新建加工单" onBack={onBack} />
      <QueryLoadErrorAlert
        message={isDraft
          ? '加工单草稿加载失败'
          : isSettings
            ? '加工单运行参数加载失败'
            : '新建加工单基础资料加载失败'}
        description={isDraft
          ? '当前空白不代表草稿不存在，重新加载成功前不会覆盖或保存草稿。'
          : isSettings
            ? '自动成品配置和备用卷号参数未完整加载，为避免使用错误默认值，当前暂停录入。'
            : '客户、仓库或机台资料未完整加载，为避免使用错误默认值，当前暂停录入。'}
        onRetry={onRetry}
      />
    </div>
  )
}

function scrollCreatePageToTop(element: HTMLElement | null) {
  const scroller = element?.closest('.app-shell__content--edge-scroll') ?? element
  scroller?.scrollTo({ top: 0, left: 0 })
}
