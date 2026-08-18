import { Alert, Button, Skeleton } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import MesPageHeader from '../../components/layout/MesPageHeader'
import MemoryCandidateReview from '../../features/projectMemory/components/MemoryCandidateReview'
import { useProjectMemory } from '../../features/projectMemory/hooks/useProjectMemory'
import './AiMemoryReviewPage.css'
import './ProjectMemoryPage.css'

export default function AiMemoryReviewPage() {
  const memory = useProjectMemory()
  if (memory.isLoading && !memory.data) return <Skeleton active paragraph={{ rows: 12 }} />

  return <main className="ai-memory-review-page">
    <MesPageHeader eyebrow="系统管理" title="AI记忆审核" />
    {memory.isError && <Alert type="error" showIcon message="项目记忆版本加载失败"
      action={<Button size="small" icon={<ReloadOutlined />}
        onClick={() => void memory.refetch()}>重试</Button>} />}
    {memory.data && <MemoryCandidateReview currentMemoryVersion={memory.data.memoryVersion} />}
  </main>
}
