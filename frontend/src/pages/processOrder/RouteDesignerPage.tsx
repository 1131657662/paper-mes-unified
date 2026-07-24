import { Button, Empty, Spin } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useGetDraft } from '../../features/processOrderCreate/hooks/useGetDraft'
import { useCustomers, useMachines } from '../../features/processOrderCreate/hooks/useReferenceData'
import RouteDesignerSession from './RouteDesignerSession'
import './RouteDesignerPage.css'

export default function RouteDesignerPage() {
  const { uuid, rollUuid } = useParams()
  const navigate = useNavigate()
  const draftQuery = useGetDraft(uuid)
  const machineQuery = useMachines()
  const customerQuery = useCustomers()
  const backToDraft = () => navigate(uuid ? `/process-orders/create?draft=${uuid}` : '/process-orders')

  if (!uuid || !rollUuid) {
    return <RouteDesignerState title="工艺路线地址无效" description="缺少加工单草稿或母卷标识，请返回加工单重新进入。" onBack={backToDraft} />
  }
  if (draftQuery.isError) {
    return (
      <RouteDesignerError
        description="当前空白不代表草稿或母卷不存在，加载成功前不会保存工艺路线。"
        message="加工单草稿加载失败"
        onBack={backToDraft}
        onRetry={() => void draftQuery.refetch()}
      />
    )
  }
  if (machineQuery.isError || customerQuery.isError) {
    return (
      <RouteDesignerError
        description="机台或客户价格资料未完整加载，为避免使用错误默认值，当前暂停路线配置。"
        message="工艺基础资料加载失败"
        onBack={backToDraft}
        onRetry={() => void Promise.all([machineQuery.refetch(), customerQuery.refetch()])}
      />
    )
  }
  if (draftQuery.isLoading || machineQuery.isLoading || customerQuery.isLoading) {
    return <RouteDesignerLoading onBack={backToDraft} />
  }

  const draft = draftQuery.data
  if (!draft) {
    return <RouteDesignerState title="未取得加工单草稿" description="请返回新建加工单页面重新加载草稿。" onBack={backToDraft} />
  }
  const roll = draft.rolls?.find((item) => item.uuid === rollUuid)
  if (!roll) {
    return <RouteDesignerState title="未找到草稿母卷" description="该母卷可能已被删除或草稿已更新，请返回后重新选择。" onBack={backToDraft} />
  }
  const config = draft.configs?.find((item) => item.originalUuid === rollUuid)
  const customer = customerQuery.data?.records?.find((item) => item.uuid === draft.order?.customerUuid)
  return (
    <RouteDesignerSession
      key={`${uuid}:${rollUuid}`}
      config={config}
      customer={customer}
      draft={draft}
      machines={machineQuery.data?.records ?? []}
      roll={roll}
      uuid={uuid}
      onBack={backToDraft}
    />
  )
}

function RouteDesignerLoading({ onBack }: { onBack: () => void }) {
  return (
    <div className="route-draft-page">
      <MesPageHeader backText="返回新建单" eyebrow="链式工艺" title="工艺路线设计" onBack={onBack} />
      <Spin spinning>
        <div className="route-draft-state" aria-label="工艺路线加载中" />
      </Spin>
    </div>
  )
}

function RouteDesignerError(props: {
  description: string
  message: string
  onBack: () => void
  onRetry: () => void
}) {
  return (
    <div className="route-draft-page">
      <MesPageHeader backText="返回新建单" eyebrow="链式工艺" title="工艺路线设计" onBack={props.onBack} />
      <QueryLoadErrorAlert description={props.description} message={props.message} onRetry={props.onRetry} />
    </div>
  )
}

function RouteDesignerState({ description, onBack, title }: {
  description: string
  onBack: () => void
  title: string
}) {
  return (
    <div className="route-draft-page">
      <MesPageHeader backText="返回新建单" eyebrow="链式工艺" title="工艺路线设计" onBack={onBack} />
      <div className="route-draft-state">
        <Empty description={<><strong>{title}</strong><span>{description}</span></>}>
          <Button type="primary" onClick={onBack}>返回新建单</Button>
        </Empty>
      </div>
    </div>
  )
}
