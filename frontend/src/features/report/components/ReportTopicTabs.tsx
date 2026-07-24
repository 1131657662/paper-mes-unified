import { AppstoreOutlined, BarChartOutlined, DollarOutlined, InboxOutlined } from '@ant-design/icons'
import { Tabs } from 'antd'
import type { ReportTopicCode } from '../../../pages/report/reportNavigation'

interface Props {
  value: ReportTopicCode
  onChange: (value: ReportTopicCode) => void
}

const items = [
  { key: 'overview', label: '经营总览', icon: <AppstoreOutlined /> },
  { key: 'production', label: '生产分析', icon: <BarChartOutlined /> },
  { key: 'settlement', label: '结算分析', icon: <DollarOutlined /> },
  { key: 'inventory', label: '库存流转', icon: <InboxOutlined /> },
] as const

export default function ReportTopicTabs({ value, onChange }: Props) {
  return <Tabs className="report-topic-tabs" activeKey={value} items={[...items]}
    onChange={(key) => onChange(key as ReportTopicCode)} size="small" />
}
