export type DetailTab = 'overview' | 'receives' | 'audit' | 'print'

interface Props {
  activeTab: DetailTab
  onChange: (tab: DetailTab) => void
}

export default function SettleDetailTabNav({ activeTab, onChange }: Props) {
  const tabs: Array<{ key: DetailTab; label: string }> = [
    { key: 'overview', label: '概览与费用' },
    { key: 'receives', label: '收款记录' },
    { key: 'audit', label: '业务追踪' },
    { key: 'print', label: '打印预览' },
  ]

  return (
    <nav className="settle-detail-tabs" aria-label="结算详情分区">
      {tabs.map((tab) => (
        <button key={tab.key} type="button" className={activeTab === tab.key ? 'is-active' : undefined}
          aria-current={activeTab === tab.key ? 'page' : undefined} onClick={() => onChange(tab.key)}>
          {tab.label}
        </button>
      ))}
    </nav>
  )
}
