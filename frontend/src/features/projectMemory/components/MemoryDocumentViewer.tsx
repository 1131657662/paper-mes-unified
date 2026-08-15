import { Typography } from 'antd'

export default function MemoryDocumentViewer({ document }: { document: unknown }) {
  const json = JSON.stringify(document, null, 2) ?? 'null'
  return (
    <section className="project-memory-document">
      <div className="project-memory-section-head">
        <strong>当前 JSON</strong>
        <Typography.Text copyable={{ text: json }}>复制</Typography.Text>
      </div>
      <pre>{json}</pre>
    </section>
  )
}
