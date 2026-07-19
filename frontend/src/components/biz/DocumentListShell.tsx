import { useState } from 'react'
import { Button, Card } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import DocumentListSkeleton from './DocumentListSkeleton'
import { TableToolbarHostProvider } from './TableToolbarPortal'
import './DocumentListShell.css'

interface QueueOption<T extends string> {
  label: string
  value: T
}

interface Props<T extends string> {
  title: string
  createText: string
  queue: T
  queueOptions: QueueOption<T>[]
  canCreate?: boolean
  children: React.ReactNode
  extra?: React.ReactNode
  leftActions?: React.ReactNode
  summary?: React.ReactNode
  loading?: boolean
  search?: React.ReactNode
  onCreate: () => void
  onQueueChange: (value: T) => void
}

export default function DocumentListShell<T extends string>({
  canCreate = true,
  children,
  createText,
  extra,
  leftActions,
  summary,
  loading = false,
  onCreate,
  onQueueChange,
  queue,
  queueOptions,
  search,
  title,
}: Props<T>) {
  const [toolsHost, setToolsHost] = useState<HTMLDivElement | null>(null)

  return (
    <Card title={title} className="document-list-shell">
      {search && <div className="document-list-shell__search">{search}</div>}
      {summary && <div className="document-list-shell__summary">{summary}</div>}
      <div className="document-list-shell__toolbar">
        <div className="document-list-shell__actions">
          {canCreate && <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>{createText}</Button>}
          {leftActions}
        </div>
        <div className="document-list-shell__queue">
          <div className="document-list-shell__queue-tabs">
            {queueOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={option.value === queue ? 'is-active' : undefined}
                onClick={() => onQueueChange(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
        <div className="document-list-shell__tools" ref={setToolsHost}>
          {extra}
        </div>
      </div>
      <TableToolbarHostProvider host={toolsHost}>
        {loading ? <DocumentListSkeleton /> : children}
      </TableToolbarHostProvider>
    </Card>
  )
}
