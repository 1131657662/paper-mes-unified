import type { PrintAnnotation } from './printPreviewTypes'

interface Props {
  annotations?: PrintAnnotation[]
}

export function PrintSpecification({ spec, annotations }: Props & { spec: string }) {
  if (!annotations?.length) return <span>{spec}</span>
  return (
    <span className="print-specification-with-annotations">
      <span>{spec}</span>
      <PrintAnnotationTags annotations={annotations} />
    </span>
  )
}

export function PrintAnnotationTags({ annotations }: Props) {
  if (!annotations?.length) return null
  return (
    <span className="print-annotation-tags">
      {formatCompactAnnotations(annotations)}
    </span>
  )
}

export function PrintOrderAnnotation({ annotations }: Props) {
  if (!annotations?.length) return null
  const content = annotations.length === 1
    ? `整单成品${formatOrderAnnotation(annotations[0])}`
    : `整单成品标注：${annotations.map(formatOrderAnnotation).join('；')}`
  return <strong className="print-order-annotation">{content}</strong>
}

function formatCompactAnnotations(annotations: PrintAnnotation[]) {
  if (annotations.length === 1) return formatCompactAnnotation(annotations[0])
  return `标注：${annotations.map(formatShortAnnotation).join('；')}`
}

function formatCompactAnnotation(annotation?: PrintAnnotation) {
  if (!annotation) return ''
  return `${formatLabel(annotation)}：${annotation.value}`
}

function formatShortAnnotation(annotation: PrintAnnotation) {
  return `${formatLabel(annotation).replace('标注', '')} ${annotation.value}`
}

function formatOrderAnnotation(annotation?: PrintAnnotation) {
  if (!annotation) return ''
  const unit = annotation.field === 'gramWeight' ? ' g/m²' : annotation.field === 'finishWidth' ? ' mm' : ''
  return `${formatLabel(annotation)}：${annotation.value}${unit}`
}

function formatLabel(annotation: PrintAnnotation) {
  if (annotation.field === 'paperName') return '标注品名'
  if (annotation.field === 'gramWeight') return '标注克重'
  return '标注门幅'
}
