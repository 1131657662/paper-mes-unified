import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { PrintViewVersion } from '../../../types/processOrder'
import { CONFIG_KEYS } from '../../systemConfig/configFallbacks'
import { useSystemConfigValue } from '../../systemConfig/hooks/useSystemConfigValue'
import {
  buildPrintRollBlocks,
  buildPrintSummary,
  type PrintRollBlock,
  type PrintRouteOutput,
  type PrintRouteStage,
  type PrintSummaryItem,
} from './printPreviewModel'
import PrintDenseTable from './PrintDenseTable'
import './PrintPreviewSheet.css'
import './PrintPreviewSheet.print.css'

interface Props {
  detail: ProcessOrderDetailVO
  snapshotTime?: string
  snapshotUser?: string
  versionLabel?: string
  version?: PrintViewVersion
}

export default function PrintPreviewSheet({ detail, snapshotTime, snapshotUser, version, versionLabel }: Props) {
  const blocks = buildPrintRollBlocks(detail)
  const summary = buildPrintSummary(detail)
  const { value: printTitle } = useSystemConfigValue(CONFIG_KEYS.processOrderTitle, '车间加工单')

  return (
    <div className="print-preview-sheet">
      <PrintHeader detail={detail} title={printTitle} snapshotTime={snapshotTime} snapshotUser={snapshotUser} versionLabel={versionLabel} />
      {orderRemark(detail) && <OrderRemarkBlock remark={orderRemark(detail)} />}
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
  return (
    <header className="print-preview-sheet__header">
      <h1>{title}</h1>
      <div className="print-preview-sheet__meta">
        <span>单号：{order.orderNo ?? '-'}</span>
        <span>客户：{order.customerName ?? '-'}</span>
        <span>日期：{order.orderDate ?? '-'}</span>
        <span>打印：{order.printStatus === 1 ? `${order.printCount ?? 1} 次` : '未打印'}</span>
        {versionLabel && <span>版本：{versionLabel}</span>}
        {snapshotTime && <span>版本时间：{snapshotTime}</span>}
        {snapshotUser && <span>版本操作人：{snapshotUser}</span>}
      </div>
    </header>
  )
}

function OrderRemarkBlock({ remark }: { remark: string }) {
  return (
    <section className="print-preview-sheet__remark-block">
      <strong>重要备注</strong>
      <span>{remark}</span>
    </section>
  )
}

function SummaryStrip({ items }: { items: PrintSummaryItem[] }) {
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

function RollBlock({ block, showActuals }: { block: PrintRollBlock; showActuals: boolean }) {
  return (
    <article className="print-roll-block">
      <aside className="print-roll-block__source">
        <h2>{block.title}</h2>
        <SourceList block={block} />
        <WriteGrid />
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
      <td>{output.spec}</td>
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

function WriteGrid() {
  return (
    <div className="print-write-grid">
      <span>实克</span>
      <i />
      <span>实幅</span>
      <i />
      <span>复重</span>
      <i />
      <span>异常</span>
      <i />
    </div>
  )
}

function PrintFooter() {
  return (
    <footer className="print-preview-sheet__footer">
      <span>操作工：</span>
      <span>复核人：</span>
      <span>班组长：</span>
      <span>完工日期：</span>
    </footer>
  )
}

function orderRemark(detail: ProcessOrderDetailVO): string {
  return [detail.order.remark, detail.order.remarkLong]
    .map((item) => item?.trim())
    .filter(Boolean)
    .join('；')
}
