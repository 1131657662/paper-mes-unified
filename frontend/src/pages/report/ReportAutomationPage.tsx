import { AlertOutlined, ClockCircleOutlined, SlidersOutlined } from '@ant-design/icons'
import { Statistic, Tabs } from 'antd'
import { useSearchParams } from 'react-router-dom'
import { useReportAlertEvents } from '../../features/reportAlert/hooks/useReportAlertEvents'
import { useReportAlertRules } from '../../features/reportAlert/hooks/useReportAlertRules'
import { useReportSubscriptions } from '../../features/reportSubscription/hooks/useReportSubscriptions'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import ReportAlertEventManagement from './ReportAlertEventManagement'
import ReportAlertRuleManagement from './ReportAlertRuleManagement'
import ReportSubscriptionManagement from './ReportSubscriptionManagement'
import './ReportManagementPage.css'

type AutomationTab = 'subscriptions' | 'events' | 'rules'

export default function ReportAutomationPage() {
  const [params, setParams] = useSearchParams()
  const canConfigure = useHasPermission(PERMISSIONS.systemConfig)
  const focusedEventUuid = params.get('eventId') ?? params.get('alertEvent') ?? undefined
  const activeTab = readTab(params, canConfigure, focusedEventUuid)
  const subscriptions = useReportSubscriptions(true)
  const events = useReportAlertEvents({ page: 1, size: 1, status: 1 }, true)
  const rules = useReportAlertRules(canConfigure)
  const changeTab = (tab: string) => {
    const next = new URLSearchParams(params)
    next.set('tab', tab)
    if (tab !== 'events') {
      next.delete('eventId')
      next.delete('alertEvent')
    }
    setParams(next, { replace: true })
  }
  const items = [
    { key: 'subscriptions', label: <span><ClockCircleOutlined /> 报表订阅</span>, children: <ReportSubscriptionManagement /> },
    { key: 'events', label: <span><AlertOutlined /> 预警事件</span>, children: <ReportAlertEventManagement focusedUuid={focusedEventUuid} /> },
    ...(canConfigure ? [{ key: 'rules', label: <span><SlidersOutlined /> 阈值规则</span>, children: <ReportAlertRuleManagement /> }] : []),
  ]
  return <main className="report-management mes-workbench">
    <header className="report-management__header">
      <div><h1>订阅与预警</h1><p>集中管理定时报表、异常阈值和待处理预警。</p></div>
    </header>
    <section className="report-management__summary" aria-label="订阅与预警概况">
      <Statistic title="我的订阅" value={subscriptions.data?.length ?? 0} suffix="项" />
      <Statistic title="启用中" value={subscriptions.data?.filter((item) => item.isEnabled === 1).length ?? 0} suffix="项" />
      <Statistic title="活动预警" value={events.data?.activeCount ?? 0} suffix="项" />
      <Statistic title="阈值规则" value={canConfigure ? rules.data?.length ?? 0 : '-'} suffix={canConfigure ? '项' : undefined} />
    </section>
    <section className="report-management__panel">
      <Tabs className="report-management__tabs" activeKey={activeTab} items={items} onChange={changeTab} />
    </section>
  </main>
}

function readTab(params: URLSearchParams, canConfigure: boolean, focusedEventUuid?: string): AutomationTab {
  if (focusedEventUuid) return 'events'
  const value = params.get('tab')
  if (value === 'events' || value === 'subscriptions') return value
  if (value === 'rules' && canConfigure) return value
  return 'subscriptions'
}
