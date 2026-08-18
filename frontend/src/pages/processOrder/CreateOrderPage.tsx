import { useEffect, useRef, useState } from 'react'
import { Card, Spin, Steps } from 'antd'
import { Navigate, useNavigate, useSearchParams } from 'react-router'
import MesPageHeader from '../../components/layout/MesPageHeader'
import BaseInfoStep from '../../features/processOrderCreate/components/BaseInfoStep'
import ConfigStep from '../../features/processOrderCreate/components/ConfigStep'
import PreviewStep from '../../features/processOrderCreate/components/PreviewStep'
import ProcessModeStep from '../../features/processOrderCreate/components/ProcessModeStep'
import RollInputStep from '../../features/processOrderCreate/components/RollInputStep'
import { useCreateOrderDirtyGuard } from '../../features/processOrderCreate/hooks/useCreateOrderDirtyGuard'
import { useCreateOrderPage } from '../../features/processOrderCreate/hooks/useCreateOrderPage'
import { notifyErrorOnce } from '../../api/request'
import CreateOrderLoadError from './CreateOrderLoadError'
import CreateOrderAiAssistant from '../../features/processOrderCreate/components/CreateOrderAiAssistant'

export { default as CreateOrderLoadError } from './CreateOrderLoadError'

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
  const [serviceWritePending, setServiceWritePending] = useState(false)
  const dirtyGuard = useCreateOrderDirtyGuard({
    captureSnapshot: state.captureSnapshot,
    pending: state.workflowPending || serviceWritePending,
    restoreSnapshot: state.restoreSnapshot,
  })
  const { clearDraftDirty: clearDirtyAfterSuccess, commitPlanChanges,
    markDraftDirty: markEdited, markPlanDirty, reconcilePlanDirty, runIfClean } = dirtyGuard
  const createAnother = () => navigate(`/process-orders/create?fresh=${Date.now()}`, { replace: true })
  const handleSubmit = async () => {
    if (await state.handleSubmit()) clearDirtyAfterSuccess()
  }
  const runPageAction = (action: () => Promise<void>, fallback: string) => {
    void action().catch((error) => notifyErrorOnce(error, fallback))
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

  if (state.nonDraftOrderUuid) {
    return <Navigate replace to={`/process-orders/${state.nonDraftOrderUuid}`} />
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
            onNext={(value) => runPageAction(async () => {
              if (await state.handleBaseNext(value)) clearDirtyAfterSuccess()
            }, '基础信息保存失败')}
          />
        )}
        {state.current === 1 && (
          <RollInputStep
            rolls={state.rolls}
            loading={state.savingRolls}
            onChange={(value) => { markEdited(); state.setRolls(value) }}
            onImportPreview={state.handleImportPreview}
            onPrev={() => runIfClean(() => state.setCurrent(0))}
            onNext={() => runPageAction(async () => {
              if (await state.handleRollsNext()) clearDirtyAfterSuccess()
            }, '原纸明细保存失败')}
          />
        )}
        {state.current === 2 && (
          <ProcessModeStep
            assistantEntry={<CreateOrderAiAssistant currentStep={3} state={state} />}
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            loading={state.updatingRolls}
            onSelect={state.setSelectedId}
            onChange={(value) => { markEdited(); state.setRolls(value) }}
            onPrev={() => runIfClean(() => state.setCurrent(1))}
            onNext={() => runPageAction(async () => {
              if (await state.handleProcessNext()) clearDirtyAfterSuccess()
            }, '加工方式保存失败')}
          />
        )}
        {state.current === 3 && (
          <ConfigStep
            aiPackagingLoading={state.aiPackagingLoading}
            aiPackagingDrafts={state.aiPackagingDrafts}
            assistantEntry={<CreateOrderAiAssistant currentStep={4} state={state} />}
            autoFinishConfigEnabled={state.autoFinishConfigEnabled}
            defaultSpareCount={state.defaultSpareCount}
            defaultPlanOptions={state.defaultPlanOptions}
            orderUuid={state.orderUuid}
            customerPrices={state.customerProcessPrices}
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            configuredPlanIds={state.configuredPlanIds}
            draftVersion={state.draftVersion}
            plans={state.plans}
            previews={state.previews}
            routePreviews={state.routePreviews}
            saving={state.savingWorkbench}
            operation={state.workbenchOperation}
            onOpenRouteDesigner={(roll) => {
              if (state.orderUuid && roll.uuid) {
                runIfClean(() => navigate(`/process-orders/create/${state.orderUuid}/routes/${roll.uuid}`))
              }
            }}
            onSelect={state.setSelectedId}
            onPlanChange={(localId, plan) => {
              markPlanDirty(localId)
              state.handlePlanChange(localId, plan)
              reconcilePlanDirty(localId, plan)
            }}
            onPreviewPlan={state.handlePreviewPlan}
            onSavePlan={async (roll, plan) => {
              const saved = await state.handleSavePlan(roll, plan)
              if (saved && saved.applied) commitPlanChanges([roll.localId])
              return saved
            }}
            onSavePlanBatch={async (rolls, plan) => {
              const result = await state.handleSavePlanBatch(rolls, plan)
              if (result) commitPlanChanges(result.appliedIds)
              return result
            }}
            onServiceDirtyChange={dirtyGuard.setServiceDirty}
            onPendingChange={setServiceWritePending}
            onDraftVersionChange={state.setDraftVersion}
            onAiPackagingDraftConsumed={state.consumeAiPackagingDraft}
            onAiPackagingDraftDismissed={state.dismissAiPackagingDraft}
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
            onSubmit={() => runPageAction(handleSubmit, '加工单提交失败')}
            onViewDetail={(orderUuid) => navigate(`/process-orders/${orderUuid}`)}
          />
        )}
      </div>
    </Spin>
  )
}

function scrollCreatePageToTop(element: HTMLElement | null) {
  const scroller = element?.closest('.app-shell__content--edge-scroll') ?? element
  scroller?.scrollTo({ top: 0, left: 0 })
}
