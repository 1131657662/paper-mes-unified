import { Tag } from 'antd'
import { buildDisplayRows } from '../../../components/processOrder/shared/displayRowBuilder'
import type { DisplayRow } from '../../../components/processOrder/shared/types'
import {
  buildConditionText,
  buildLayoutText,
  fmtDiameter,
  rewindModeLabel,
} from '../../../components/processOrder/shared/detailHelpers'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import { CONFIG_KEYS } from '../../systemConfig/configFallbacks'
import { useSystemConfigValue } from '../../systemConfig/hooks/useSystemConfigValue'
import type { FinishProductionVO, ProcessOrderDetailVO, RollProductionVO } from '../../../types/processOrder'
import { buildDetailMetrics, formatKg } from '../orderDetailUtils'
import './PrintPreviewSheet.css'
import './PrintPreviewSheet.print.css'

interface Props {
  detail: ProcessOrderDetailVO
}

export default function PrintPreviewSheet({ detail }: Props) {
  const metrics = buildDetailMetrics(detail)
  const rows = buildDisplayRows(detail.rollProductions ?? [])
  const { value: printTitle } = useSystemConfigValue(CONFIG_KEYS.processOrderTitle, '车间加工单')

  return (
    <div className="print-preview-sheet">
      <header className="print-preview-sheet__header">
        <div>
          <h1>{printTitle}</h1>
          <div className="print-preview-sheet__meta">
            <span>加工单号：{detail.order.orderNo ?? '-'}</span>
            <span>客户：{detail.order.customerName ?? '-'}</span>
            <span>制单日期：{detail.order.orderDate ?? '-'}</span>
          </div>
        </div>
        <div className="print-preview-sheet__summary">
          {detail.order.isMixProcess === 1 && <Tag color="purple">混合工艺</Tag>}
          <div>{metrics.rollCount} 卷 / {formatKg(metrics.totalOriginalWeight)}</div>
          <div>{metrics.finishCount} 个正式号</div>
        </div>
      </header>

      <section>
        <h2>原纸复核区</h2>
        <table className="print-preview-table print-preview-table--roll-check">
          <thead>
            <tr>
              <th>母卷</th>
              <th>品名</th>
              <th>标称克重</th>
              <th>标称门幅</th>
              <th>标称重量</th>
              <th>实际克重</th>
              <th>实际门幅</th>
              <th>复称重量</th>
              <th>复核人</th>
            </tr>
          </thead>
          <tbody>
            {detail.originalRolls.map((roll, index) => (
              <tr key={roll.uuid}>
                <td>{roll.rollNo || roll.extraNo || `母卷 ${index + 1}`}</td>
                <td>{roll.paperName || '-'}</td>
                <td>{roll.gramWeight ? `${roll.gramWeight}g` : '-'}</td>
                <td>{roll.originalWidth ? `${roll.originalWidth}mm` : '-'}</td>
                <td>{formatKg((roll.rollWeight ?? 0) * (roll.pieceNum ?? 1))}</td>
                <td className="print-preview-table__write" />
                <td className="print-preview-table__write" />
                <td className="print-preview-table__write" />
                <td className="print-preview-table__write" />
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section>
        <h2>成品加工明细</h2>
        {rows.map((row) => (
          <ProductionBlock key={row.key} row={row} />
        ))}
      </section>

      <footer className="print-preview-sheet__footer">
        <span>总成品：{metrics.finishCount} 件</span>
        <span>备用号：{metrics.spareCount} 个</span>
        <span>总刀数：{metrics.knifeCount} 刀</span>
        <span>操作工签字：</span>
        <span>完工日期：</span>
      </footer>
    </div>
  )
}

function ProductionBlock({ row }: { row: DisplayRow }) {
  const production = row.mainProduction
  const finishes = row.finishes

  if (finishes.length === 0) {
    return (
      <table className="print-preview-table print-preview-table--production">
        <ProductionHeader />
        <tbody>
          <tr>
            <RollInfoCell title={rollTitle(row)} production={production} />
            <td colSpan={7}>暂无预生成成品，直发卷将在回录后生成出库记录</td>
          </tr>
        </tbody>
      </table>
    )
  }

  return (
    <table className="print-preview-table print-preview-table--production">
      <ProductionHeader />
      <tbody>
        {finishes.map((finish, index) => (
          <tr key={finish.uuid}>
            {index === 0
              ? <RollInfoCell title={rollTitle(row)} production={production} />
              : <RollRepeatCell title={rollTitle(row)} />}
            <td>{finish.finishRollNo || '-'}</td>
            <td>{finish.isSpare === 1 ? '备用' : '正式'}</td>
            <td>{finishSpec(finish)}</td>
            <td>{formatKg(finish.estimateWeight)}</td>
            <td className="print-preview-table__write" />
            <td className="print-preview-table__write" />
            <td className="print-preview-table__write" />
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function ProductionHeader() {
  return (
    <thead>
      <tr>
        <th>母卷信息</th>
        <th>成品卷号</th>
        <th>类型</th>
        <th>预设规格</th>
        <th>预估重量</th>
        <th>实际重量</th>
        <th>异常说明</th>
        <th>签字</th>
      </tr>
    </thead>
  )
}

function RollInfoCell({
  title,
  production,
}: {
  title: string
  production: RollProductionVO
}) {
  return (
    <td className="print-preview-sheet__roll-cell">
      <strong>{title}</strong>
      <span>{production.paperName || '-'} / {production.gramWeight ?? '-'}g / {production.originalWidth ?? '-'}mm</span>
      <span>工艺：{mainProcessLabel(production)}</span>
      <span>加工方式：{PROCESS_MODE[production.processMode ?? 1] ?? '-'}</span>
      <span>条件：{buildConditionText(production)}</span>
      <span>排布：{buildLayoutText(production)}</span>
    </td>
  )
}

function RollRepeatCell({ title }: { title: string }) {
  return (
    <td className="print-preview-sheet__roll-repeat">
      <strong>{title}</strong>
      <span>同卷</span>
    </td>
  )
}

function rollTitle(row: DisplayRow): string {
  if (row.isMergeGroup) return `合并复卷 ${row.rollProductions.length} 卷`
  const production = row.mainProduction
  return production.rollNo || production.extraNo || `母卷 ${row.seq}`
}

function mainProcessLabel(production: RollProductionVO): string {
  if (production.processMode === 3) return '直发'
  if (production.mainStepType === 2) return rewindModeLabel(production) ?? '复卷'
  return STEP_TYPE[production.mainStepType ?? 1] ?? '锯纸'
}

function finishSpec(finish: FinishProductionVO): string {
  const width = finish.finishWidth ? `${finish.finishWidth}mm` : '-'
  const diameter = finish.finishDiameter ? ` / ${fmtDiameter(finish.finishDiameter, 'φ')}` : ''
  return `${width}${diameter}`
}
