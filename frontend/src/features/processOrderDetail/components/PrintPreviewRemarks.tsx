import type { PrintAnnotation, PrintSummaryItem } from './printPreviewTypes'
import { PrintOrderAnnotation } from './PrintAnnotationText'

export function OrderRemarkBlock({ remark, annotations }: { remark?: string; annotations?: PrintAnnotation[] }) {
  if (!remark && !annotations?.length) return null
  return (
    <section className="print-preview-sheet__remark-block">
      <strong>客户/特殊备注</strong>
      <div className="print-preview-sheet__remark-content">
        <PrintOrderAnnotation annotations={annotations} />
        {remark && <span>{remark}</span>}
      </div>
    </section>
  )
}

export function WorkshopInstructionBlock({ instructions }: { instructions: string[] }) {
  if (!instructions.length) return null
  return (
    <section className="print-preview-sheet__instruction-block">
      <strong>生产指令</strong>
      <ol>
        {instructions.map((instruction) => <li key={instruction}>{instruction}</li>)}
      </ol>
    </section>
  )
}

export function SummaryStrip({ items }: { items: PrintSummaryItem[] }) {
  return (
    <section className="print-preview-sheet__summary">
      {items.map((item) => (
        <span key={item.label}>
          <em>{item.label}</em>
          <strong>{item.value}</strong>
        </span>
      ))}
    </section>
  )
}
