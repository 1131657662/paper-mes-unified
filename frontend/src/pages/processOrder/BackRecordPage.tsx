import { Tag } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import BackRecordWorkspace from './backRecord/BackRecordWorkspace'

export default function BackRecordPage() {
  const { uuid } = useParams<{ uuid: string }>()
  const navigate = useNavigate()
  const goDetail = () => navigate(`/process-orders/${uuid}`)

  return (
    <div className="back-record-page mes-scroll-page">
      <MesPageHeader
        actions={<Tag color="warning">待回录</Tag>}
        backText="返回详情"
        description="按母卷核对原纸复称、成品实重、损耗闭合和现场变更。"
        eyebrow="生产回录"
        onBack={goDetail}
        title="回录工作台"
      />
      <BackRecordWorkspace
        uuid={uuid}
        cancelText="返回详情"
        mode="page"
        onClose={goDetail}
        onSuccess={goDetail}
      />
    </div>
  )
}
