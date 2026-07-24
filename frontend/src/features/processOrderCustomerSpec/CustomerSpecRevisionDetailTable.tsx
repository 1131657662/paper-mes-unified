import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import { toDisplayFormula } from './customerWeightFormulaModel'
import type { FinishCustomerRevisionDetail, FinishCustomerRevisionItem } from './customerSpecTypes'

interface Props { detail?: FinishCustomerRevisionDetail; loading: boolean }

export default function CustomerSpecRevisionDetailTable({ detail, loading }: Props) {
  return <Table<FinishCustomerRevisionItem> bordered columns={columns} dataSource={detail?.items ?? []}
    loading={loading} pagination={false} rowKey="finishUuid" scroll={{ x: 1320 }} size="small" />
}

const columns: ColumnsType<FinishCustomerRevisionItem> = [
  { title: '成品卷号', dataIndex: 'finishRollNo', width: 140, fixed: 'left', render: (value) => <Typography.Text strong>{value || '-'}</Typography.Text> },
  { title: '实物规格', width: 200, render: (_, item) => physicalSpec(item) },
  { title: '客户规格', width: 200, render: (_, item) => customerSpec(item) },
  { title: '实物重量', width: 120, align: 'right', render: (_, item) => formatKg(item.physicalWeightSnapshot) },
  { title: '客户重量', width: 125, align: 'right', render: (_, item) => formatWholeKg(item.customerDisplayWeight) },
  { title: '重量方式', width: 95, render: (_, item) => <Tag>{modeText[item.calculationMode] ?? item.calculationMode}</Tag> },
  { title: '计算依据', width: 300, render: (_, item) => calculationAudit(item) },
  { title: '备注', dataIndex: 'remark', width: 180, render: (value) => value || '-' },
]

function calculationAudit(item: FinishCustomerRevisionItem) {
  const details = calculationDetails(item)
  return <div><Typography.Text>{details.primary}</Typography.Text>{details.secondary && <><br /><Typography.Text type="secondary">{details.secondary}</Typography.Text></>}</div>
}

function calculationDetails(item: FinishCustomerRevisionItem) {
  if (item.calculationMode === 'DELTA') return { primary: `${signed(item.weightOperand)} kg/件` }
  if (item.calculationMode === 'RATIO') return { primary: `× ${item.weightOperand ?? '-'}` }
  if (item.calculationMode !== 'FORMULA') return { primary: modeText[item.calculationMode] ?? item.calculationMode }
  const variables = Object.entries(item.formulaVariables ?? {}).map(([key, value]) => `${toDisplayFormula(key)}=${value}`).join('，')
  const rounding = `${item.roundingScale ?? 0}位 · ${roundingText[item.roundingMode ?? ''] ?? item.roundingMode ?? '-'}`
  return { primary: toDisplayFormula(item.formulaExpression), secondary: [variables, rounding, zeroText[item.zeroPolicy ?? '']].filter(Boolean).join('；') }
}

function signed(value?: number) {
  if (value == null) return '-'
  return value > 0 ? `+${value}` : String(value)
}

function physicalSpec(item: FinishCustomerRevisionItem) {
  return `${item.physicalPaperName ?? '-'} / ${item.physicalGramWeight ?? '-'}g / ${item.physicalFinishWidth ?? '-'}mm`
}

function customerSpec(item: FinishCustomerRevisionItem) {
  return `${item.customerPaperName ?? '-'} / ${item.customerGramWeight ?? '-'}g / ${item.customerFinishWidth ?? '-'}mm`
}

const modeText: Record<string, string> = {
  KEEP: '保持', FIXED: '固定', DELTA: '加减', RATIO: '比例', FORMULA: '公式', MANUAL: '手工',
}
const roundingText: Record<string, string> = { HALF_UP: '四舍五入', UP: '向上取整', DOWN: '向下取整' }
const zeroText: Record<string, string> = { SKIP: '零值保留原重量', ERROR: '零值报错', USE_ZERO: '允许零值参与计算' }
