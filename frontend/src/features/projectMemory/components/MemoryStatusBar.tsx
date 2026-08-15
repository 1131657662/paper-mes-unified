import { Tag, Typography } from 'antd'
import type { ProjectMemorySnapshot } from '../types'

interface Props {
  snapshot: ProjectMemorySnapshot
  versionCount: number
}

export default function MemoryStatusBar({ snapshot, versionCount }: Props) {
  return (
    <section className="project-memory-status" aria-label="项目记忆状态">
      <StatusItem label="当前版本" value={snapshot.memoryVersion} />
      <StatusItem label="Schema" value={snapshot.schemaVersion} />
      <div className="project-memory-status__item">
        <span>运行状态</span>
        <Tag className="mes-status-tag" color={stateColor(snapshot.state)}>{stateLabel(snapshot.state)}</Tag>
      </div>
      <StatusItem label="保留版本" value={`${versionCount} 个`} />
      <div className="project-memory-status__checksum">
        <span>Checksum</span>
        <Typography.Text copyable ellipsis={{ tooltip: snapshot.checksum }}>
          {snapshot.checksum}
        </Typography.Text>
      </div>
    </section>
  )
}

function StatusItem({ label, value }: { label: string; value: string }) {
  return <div className="project-memory-status__item"><span>{label}</span><strong>{value}</strong></div>
}

function stateColor(state: ProjectMemorySnapshot['state']) {
  if (state === 'READY') return 'success'
  if (state === 'DEGRADED') return 'warning'
  return 'error'
}

function stateLabel(state: ProjectMemorySnapshot['state']) {
  if (state === 'READY') return '正常'
  if (state === 'DEGRADED') return '降级'
  return '不可用'
}
