import MesPageHeader from '../../components/layout/MesPageHeader'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'

interface Props {
  kind: 'draft' | 'reference' | 'settings'
  onBack: () => void
  onRetry: () => void
}

export default function CreateOrderLoadError({ kind, onBack, onRetry }: Props) {
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
