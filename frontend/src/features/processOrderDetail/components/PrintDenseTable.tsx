import type {
  PrintRollBlock,
  PrintRouteOutput,
} from './printPreviewModel'
import { buildDenseRows, type DenseItemRow, type DenseRow } from './printDenseTableModel'

interface Props {
  blocks: PrintRollBlock[]
}

export default function PrintDenseTable({ blocks }: Props) {
  const rows = buildDenseRows(blocks)
  if (!rows.length) return null
  return (
    <section className="print-dense-section" aria-label="打印紧凑表格">
      <table className="print-dense-table">
        <thead>
          <tr>
            <th>母卷</th>
            <th>原纸信息</th>
            <th>产物</th>
            <th>规格</th>
            <th>预估重</th>
            <th>去向</th>
            <th>实重</th>
            <th>异常</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => <DenseTableRow key={row.key} row={row} />)}
        </tbody>
      </table>
    </section>
  )
}

function DenseTableRow({ row }: { row: DenseRow }) {
  if (row.kind === 'group') {
    return (
      <tr className="print-dense-table__group-row">
        <td colSpan={8}>
          <strong>{row.processTitle}</strong>
          <span>{row.processDetail}</span>
        </td>
      </tr>
    )
  }
  return <DenseItemTableRow row={row} />
}

function DenseItemTableRow({ row }: { row: DenseItemRow }) {
  const fillable = row.output?.status === 'final' || row.output?.status === 'trim'
  return (
    <tr className="print-dense-table__item-row">
      {row.blockRowSpan && (
        <td className="print-dense-table__mother" rowSpan={row.blockRowSpan}>
          <strong>{row.block.title}</strong>
          <span>{sourceValue(row.block, '卷号/编号')}</span>
          {row.block.remark && <em>备注：{row.block.remark}</em>}
        </td>
      )}
      {row.blockRowSpan && (
        <td className="print-dense-table__source" rowSpan={row.blockRowSpan}>
          {sourceInfo(row.block)}
        </td>
      )}
      <td>{row.output ? <DenseOutputName output={row.output} /> : '-'}</td>
      <td>{row.output?.spec ?? '-'}</td>
      <td>{row.output?.weight ?? '-'}</td>
      <td>{row.output ? outputStatusText(row.output.status) : '-'}</td>
      <td className={fillable ? 'print-dense-table__write' : 'print-dense-table__muted'}>
        {fillable ? '' : '-'}
      </td>
      <td className={fillable ? 'print-dense-table__write' : 'print-dense-table__muted'}>
        {fillable ? '' : '-'}
      </td>
    </tr>
  )
}

function DenseOutputName({ output }: { output: PrintRouteOutput }) {
  return (
    <span className="print-dense-table__product">
      {output.layerText && <span className="print-dense-table__layer">{output.layerText}</span>}
      <span>{output.name}</span>
    </span>
  )
}

function sourceInfo(block: PrintRollBlock): string {
  return [
    sourceValue(block, '品名'),
    sourceValue(block, '克重/门幅'),
    `标重 ${sourceValue(block, '标重')}`,
    sourceValue(block, '方式'),
  ].filter((item) => item && !item.endsWith(' -')).join('\n')
}

function sourceValue(block: PrintRollBlock, label: string): string {
  return block.sourceItems.find((item) => item.label === label)?.value ?? '-'
}

function outputStatusText(status: PrintRouteOutput['status']): string {
  if (status === 'next') return '进下道'
  if (status === 'trim') return '修边'
  return '交付'
}
