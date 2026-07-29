export interface PrintRouteOutput {
  key: string
  finishRollUuid?: string
  layerText?: string
  name: string
  spec: string
  weight: string
  actualWeight?: string
  weightValue?: number
  width?: number
  status: 'next' | 'final' | 'trim'
  annotations?: PrintAnnotation[]
}

export interface PrintRouteStage {
  key: string
  stepType?: number
  title: string
  source: string
  metric: string
  requirement: string
  outputs: PrintRouteOutput[]
}

export interface PrintRollBlock {
  key: string
  title: string
  sourceItems: Array<{ label: string; value: string }>
  remark?: string
  routeStages: PrintRouteStage[]
  annotations?: PrintAnnotation[]
}

export type PrintAnnotationField = 'paperName' | 'gramWeight' | 'finishWidth'

export interface PrintAnnotation {
  field: PrintAnnotationField
  value: string
}

export interface PrintSheetModel {
  blocks: PrintRollBlock[]
  orderAnnotations: PrintAnnotation[]
}

export interface PrintSummaryItem {
  label: string
  value: string
}
