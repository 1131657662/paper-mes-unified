import { ClockCircleOutlined } from '@ant-design/icons'
import { Alert, Card, Skeleton, Tag } from 'antd'
import { findReportNavigation } from './reportNavigation'
import './ReportPendingPage.css'

interface Props {
  path: string
}

export default function ReportPendingPage({ path }: Props) {
  const page = findReportNavigation(path)
  const title = page?.label ?? '统计报表'
  const description = page?.description ?? '专题分析页面'

  return (
    <main className="report-pending mes-workbench">
      <header className="report-pending__header">
        <div>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
        <Tag icon={<ClockCircleOutlined />}>数据契约接入中</Tag>
      </header>
      <Alert
        showIcon
        type="info"
        message="专题数据尚未开放"
        description="本页面已从旧的通用报表中独立，领域接口和指标口径通过数据审查后再展示结果。"
      />
      <section className="report-pending__metrics" aria-label="指标区域占位">
        {[1, 2, 3, 4, 5, 6].map((key) => (
          <Card key={key} size="small"><Skeleton active paragraph={false} /></Card>
        ))}
      </section>
      <Card className="report-pending__content" title="分析结果">
        <Skeleton active paragraph={{ rows: 8 }} />
      </Card>
    </main>
  )
}
