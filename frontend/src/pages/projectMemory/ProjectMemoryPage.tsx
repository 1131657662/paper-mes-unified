import { Alert, Button, Skeleton, Tabs } from 'antd'
import { SyncOutlined } from '@ant-design/icons'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import MemoryDocumentViewer from '../../features/projectMemory/components/MemoryDocumentViewer'
import MemoryPatchEditor from '../../features/projectMemory/components/MemoryPatchEditor'
import MemoryStatusBar from '../../features/projectMemory/components/MemoryStatusBar'
import MemoryVersionHistory from '../../features/projectMemory/components/MemoryVersionHistory'
import { useProjectMemory } from '../../features/projectMemory/hooks/useProjectMemory'
import { useProjectMemoryVersions } from '../../features/projectMemory/hooks/useProjectMemoryVersions'
import { useReloadProjectMemory } from '../../features/projectMemory/hooks/useReloadProjectMemory'
import { useHasPermission } from '../../stores/authStore'
import './ProjectMemoryPage.css'

export default function ProjectMemoryPage() {
  const { data: snapshot, isLoading: isLoadingMemory, isError: isMemoryError,
    refetch: refetchMemory } = useProjectMemory()
  const { data: versions = [], isLoading: isLoadingVersions, isError: isVersionsError,
    refetch: refetchVersions } = useProjectMemoryVersions()
  const { mutate: reloadMemory, isPending: isReloading } = useReloadProjectMemory()
  const canManage = useHasPermission(PERMISSIONS.aiMemoryManage)

  if (isLoadingMemory && !snapshot) return <Skeleton active paragraph={{ rows: 12 }} />

  return (
    <main className="project-memory-page">
      <MesPageHeader eyebrow="系统管理" title="项目记忆" actions={(
        <Button icon={<SyncOutlined />} loading={isReloading} onClick={() => reloadMemory()}>重新加载</Button>
      )} />
      {(isMemoryError || isVersionsError) && <LoadError onRetry={() => {
        void refetchMemory(); void refetchVersions()
      }} />}
      {snapshot && <MemoryStatusBar snapshot={snapshot} versionCount={versions.length} />}
      {snapshot && <div className="project-memory-workspace"><Tabs items={[
        { key: 'document', label: '当前文档', children: <MemoryDocumentViewer document={snapshot.document} /> },
        { key: 'history', label: '版本历史', children: <MemoryVersionHistory activeVersion={snapshot.memoryVersion}
          canManage={canManage} items={versions} loading={isLoadingVersions} /> },
        ...(canManage ? [{ key: 'patch', label: '提交补丁',
          children: <MemoryPatchEditor expectedVersion={snapshot.memoryVersion} /> }] : []),
      ]} /></div>}
    </main>
  )
}

function LoadError({ onRetry }: { onRetry: () => void }) {
  return <Alert type="error" showIcon message="项目记忆资料加载失败"
    action={<Button size="small" onClick={onRetry}>重试</Button>} />
}
