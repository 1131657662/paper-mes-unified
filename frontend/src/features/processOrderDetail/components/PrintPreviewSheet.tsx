import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { PrintViewVersion } from '../../../types/processOrder'
import { PRIORITY } from '../../../constants/processOrder'
import { CONFIG_KEYS } from '../../systemConfig/configFallbacks'
import { useSystemConfigValue } from '../../systemConfig/hooks/useSystemConfigValue'
import {
  buildPrintSheetModel,
  buildPrintSummary,
  type PrintRollBlock,
  type PrintRouteOutput,
  type PrintRouteStage,
} from './printPreviewModel'
import PrintDenseTable from './PrintDenseTable'
import { PrintAnnotationTags, PrintSpecification } from './PrintAnnotationText'
import { OrderRemarkBlock, SummaryStrip } from './PrintPreviewRemarks'
import { orderRemark } from './printPreviewRemarkModel'
import { PrintFooter } from './PrintPreviewFooter'
import './PrintPreviewSheet.css'
import './PrintPreviewSheet.print.css'

interface Props {
  detail: ProcessOrderDetailVO
  snapshotTime?: string
  snapshotUser?: string
  versionLabel?: string
  version?: PrintViewVersion
  historical?: boolean
}

export default function PrintPreviewSheet({ detail, historical, snapshotTime, snapshotUser, version, versionLabel }: Props) {
  const { blocks, orderAnnotations } = buildPrintSheetModel(detail)
  const summary = buildPrintSummary(detail)
  const { value: printTitle } = useSystemConfigValue(CONFIG_KEYS.processOrderTitle, '车间加工单')
  const remark = orderRemark(detail)

  return (
    <div className={`print-preview-sheet${historical ? ' print-preview-sheet--historical' : ''}`}>
      {historical && <div aria-hidden className="print-preview-sheet__historical-watermark">历史版本 - 禁止作为当前生产指令</div>}
      <PrintHeader detail={detail} title={printTitle} snapshotTime={snapshotTime} snapshotUser={snapshotUser} versionLabel={versionLabel} />
      <OrderRemarkBlock remark={remark} annotations={orderAnnotations} />
      <SummaryStrip items={summary} />
      <section className="print-preview-sheet__routes">
        {blocks.map((block) => <RollBlock block={block} key={block.key} showActuals={version === 'FINISHED'} />)}
      </section>
      <PrintDenseTable blocks={blocks} showActuals={version === 'FINISHED'} />
      <PrintFooter />
    </div>
  )
}

function PrintHeader({ detail, snapshotTime, snapshotUser, title, versionLabel }: Props & { title: string }) {
  const { order } = detail
  const priorityText = PRIORITY[order.priority ?? 1] ?? '普通'
  const highlighted = (order.priority ?? 1) >= 2
  return (
    <header className="print-preview-sheet__header">
      <div className="print-preview-sheet__title">
        <h1>{title}</h1>
        {highlighted && <strong className="print-priority-mark">{priorityText}</strong>}
      </div>
      <div className="print-preview-sheet__meta">
        <span>单号：{order.orderNo ?? '-'}</span>
        <span>客户：{order.customerName ?? '-'}</span>
        <span>日期：{order.orderDate ?? '-'}</span>
        {versionLabel && <span>版本：{versionLabel}</span>}
        {snapshotTime && <span>版本时间：{snapshotTime}</span>}
        {snapshotUser && <span>版本操作人：{snapshotUser}</span>}
      </div>
    </header>
  )
}

function RollBlock({ block, showActuals }: { block: PrintRollBlock; showActuals: boolean }) {
  return (
    <article className="print-roll-block">
      <aside className="print-roll-block__source">
        <h2>{block.title}</h2>
        <SourceList block={block} />
      </aside>
      <div className="print-roll-block__main">
        <RouteStages stages={block.routeStages} showActuals={showActuals} />
      </div>
    </article>
  )
}

function SourceList({ block }: { block: PrintRollBlock }) {
  return (
    <dl className="print-roll-source-list">
      {block.sourceItems.map((item) => (
        <div key={item.label}>
          <dt>{item.label}</dt>
          <dd>{item.value}</dd>
        </div>
      ))}
      {block.annotations?.length ? <PrintAnnotationTags annotations={block.annotations} /> : null}
      {block.remark && (
        <div className="print-roll-source-list__remark">
          <dt>明细备注</dt>
          <dd>{block.remark}</dd>
        </div>
      )}
    </dl>
  )
}

function RouteStages({ stages, showActuals }: { stages: PrintRouteStage[]; showActuals: boolean }) {
  if (!stages.length) return <div className="print-route-empty">未配置加工路线</div>
  return (
    <div className="print-route-stage-list">
      {stages.map((stage) => <RouteStage stage={stage} key={stage.key} showActuals={showActuals} />)}
    </div>
  )
}

function RouteStage({ stage, showActuals }: { stage: PrintRouteStage; showActuals: boolean }) {
  return (
    <section className="print-route-stage">
      <div className="print-route-stage__head">
        <strong>{stage.title}</strong>
        <span>来源：{stage.source}</span>
        <span>{stage.metric}</span>
      </div>
      <p className="print-route-stage__requirement">
        <strong>工艺要求：</strong>
        {stage.requirement}
      </p>
      <OutputList outputs={stage.outputs} showActuals={showActuals} />
    </section>
  )
}

function OutputList({ outputs, showActuals }: { outputs: PrintRouteOutput[]; showActuals: boolean }) {
  if (!outputs.length) return <div className="print-route-output-empty">暂无产出</div>
  return (
    <table className="print-route-output-table">
      <thead>
        <tr>
          <th>产物</th>
          <th>规格</th>
          <th>预估重量</th>
          <th>状态</th>
          <th>实际重量</th>
          <th>异常说明</th>
        </tr>
      </thead>
      <tbody>
        {outputs.map((output) => <OutputRow output={output} key={output.key} showActuals={showActuals} />)}
      </tbody>
    </table>
  )
}

function OutputRow({ output, showActuals }: { output: PrintRouteOutput; showActuals: boolean }) {
  const fillable = output.status === 'final'
  return (
    <tr className={`print-route-output-row print-route-output-row--${output.status}`}>
      <td><OutputName output={output} /></td>
      <td><PrintSpecification spec={output.spec} annotations={output.annotations} /></td>
      <td>{output.weight}</td>
      <td><strong>{outputStatusText(output.status)}</strong></td>
      <td className={fillable ? 'print-write-cell' : 'print-muted-cell'}>{showActuals ? output.actualWeight ?? '-' : fillable ? '' : '-'}</td>
      <td className={fillable ? 'print-write-cell' : 'print-muted-cell'}>{fillable ? '' : '-'}</td>
    </tr>
  )
}

function OutputName({ output }: { output: PrintRouteOutput }) {
  return (
    <span className="print-route-output-name">
      {output.layerText && <span className="print-route-output-layer">{output.layerText}</span>}
      <span>{output.name}</span>
    </span>
  )
}

function outputStatusText(status: PrintRouteOutput['status']) {
  if (status === 'next') return '进入下道'
  if (status === 'trim') return '修边'
  return '最终交付'
}
