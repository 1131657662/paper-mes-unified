import { useEffect, useRef } from 'react'
import { Card, Spin, Steps } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
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
  const createAnother = () => navigate(`/process-orders/create?fresh=${Date.now()}`, { replace: true })

  useEffect(() => {
    scrollCreatePageToTop(pageRef.current)
  }, [state.current, state.submitResult?.orderUuid])

  return (
    <Spin wrapperClassName="mes-spin-fill process-order-create-spin" spinning={state.loadingDraft}>
      <div ref={pageRef} className="mes-scroll-page mes-form-page">
        <MesPageHeader
          backText="返回列表"
          eyebrow="加工单"
          onBack={() => navigate('/process-orders')}
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
            onChange={state.handleBaseInfoChange}
            onNext={state.handleBaseNext}
          />
        )}
        {state.current === 1 && (
          <RollInputStep
            rolls={state.rolls}
            loading={state.savingRolls}
            onChange={state.setRolls}
            onImportPreview={state.handleImportPreview}
            onPrev={() => state.setCurrent(0)}
            onNext={state.handleRollsNext}
          />
        )}
        {state.current === 2 && (
          <ProcessModeStep
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            loading={state.updatingRolls}
            onSelect={state.setSelectedId}
            onChange={state.setRolls}
            onPrev={() => state.setCurrent(1)}
            onNext={state.handleProcessNext}
          />
        )}
        {state.current === 3 && (
          <ConfigStep
            defaultSpareCount={state.defaultSpareCount}
            defaultPlanOptions={state.defaultPlanOptions}
            orderUuid={state.orderUuid}
            machines={state.machines}
            rolls={state.rolls}
            selectedId={state.selectedId}
            plans={state.plans}
            previews={state.previews}
            routePreviews={state.routePreviews}
            saving={state.savingWorkbench}
            onOpenRouteDesigner={(roll) => {
              if (state.orderUuid && roll.uuid) navigate(`/process-orders/create/${state.orderUuid}/routes/${roll.uuid}`)
            }}
            onSelect={state.setSelectedId}
            onPlanChange={state.handlePlanChange}
            onPreviewPlan={state.handlePreviewPlan}
            onSavePlan={state.handleSavePlan}
            onSavePlanBatch={state.handleSavePlanBatch}
            onPrev={() => state.setCurrent(2)}
            onNext={state.handleConfigNext}
          />
        )}
        {state.current === 4 && (
          <PreviewStep
            rolls={state.rolls}
            plans={state.plans}
            previews={state.previews}
            routePreviews={state.routePreviews}
            submitting={state.submitting}
            submitResult={state.submitResult}
            onBackToList={() => navigate('/process-orders')}
            onCreateAnother={createAnother}
            onPrev={() => state.setCurrent(3)}
            onEditRoll={(localId) => {
              state.setSelectedId(localId)
              state.setCurrent(3)
            }}
            onSubmit={state.handleSubmit}
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
