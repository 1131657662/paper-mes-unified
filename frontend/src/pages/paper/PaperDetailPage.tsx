import { Button, Card, Descriptions, Empty, Skeleton, Space, Tag } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getPaper } from '../../api/paper'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import type { Paper } from '../../types/paper'
import '../documentModule.css'
import './PaperProfile.css'

export default function PaperDetailPage() {
  const { uuid } = useParams()
  const navigate = useNavigate()
  const [paper, setPaper] = useState<Paper>()
  const [loading, setLoading] = useState(true)
  const canManageBase = useHasPermission(PERMISSIONS.baseManage)

  useEffect(() => {
    if (!uuid) return
    setLoading(true)
    getPaper(uuid)
      .then(setPaper)
      .finally(() => setLoading(false))
  }, [uuid])

  if (loading) {
    return (
      <div className="document-module-page paper-profile-page">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    )
  }

  if (!paper) {
    return (
      <div className="document-module-page paper-profile-page">
        <Empty description="纸张档案不存在" />
      </div>
    )
  }

  return (
    <div className="document-module-page paper-profile-page">
      <MesPageHeader
        title={paper.paperName}
        eyebrow="纸张档案"
        description={`纸张编码：${text(paper.paperCode)} · 常用克重：${gramWeightText(paper.gramWeight)} · 类型：${text(paper.paperType)}`}
        onBack={() => navigate('/papers')}
        tags={<Tag color="blue">{paper.paperType || '未分类'}</Tag>}
        actions={canManageBase ? (
          <Space>
            <Button icon={<EditOutlined />} type="primary" onClick={() => navigate(`/papers/${paper.uuid}/edit`)}>
              编辑纸张
            </Button>
          </Space>
        ) : undefined}
      />

      <section className="paper-detail-overview">
        <MetricCard label="常用克重" value={gramWeightText(paper.gramWeight)} helper="新建加工单原纸录入参考" />
        <MetricCard label="纸张类型" value={paper.paperType || '-'} helper="用于列表筛选和品类识别" />
        <MetricCard label="纸张编码" value={paper.paperCode || '-'} helper="内部档案编码" />
        <MetricCard label="最近更新" value={dateText(paper.updateTime)} helper="档案维护时间" />
      </section>

      <div className="paper-detail-grid">
        <Card className="document-module-card" title="基础资料">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="纸张编码">{text(paper.paperCode)}</Descriptions.Item>
            <Descriptions.Item label="纸张品名">{text(paper.paperName)}</Descriptions.Item>
            <Descriptions.Item label="常用克重">{gramWeightText(paper.gramWeight)}</Descriptions.Item>
            <Descriptions.Item label="纸张类型">{text(paper.paperType)}</Descriptions.Item>
          </Descriptions>
        </Card>

        <Card className="document-module-card" title="维护信息">
          <Descriptions column={2} size="small">
            <Descriptions.Item label="创建时间">{dateText(paper.createTime)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{dateText(paper.updateTime)}</Descriptions.Item>
            <Descriptions.Item label="备注" span={2}>{text(paper.remark)}</Descriptions.Item>
          </Descriptions>
        </Card>
      </div>
    </div>
  )
}

function MetricCard({ helper, label, value }: { helper: string; label: string; value: string }) {
  return (
    <div className="paper-detail-metric">
      <span>{label}</span>
      <strong>{value}</strong>
      <em>{helper}</em>
    </div>
  )
}

function gramWeightText(value?: number) {
  return value == null ? '-' : `${value} g/㎡`
}

function dateText(value?: string) {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm:ss') : value
}

function text(value?: string) {
  return value || '-'
}
