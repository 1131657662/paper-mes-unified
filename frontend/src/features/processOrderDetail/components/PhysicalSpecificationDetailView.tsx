import type { FinishedProductRow } from './finishedProductRows'
import InternalFinishedProductsTable from './InternalFinishedProductsTable'
import PhysicalSpecificationSummaryTable from './PhysicalSpecificationSummaryTable'
import type { FinishedProductsSortState } from './useFinishedProductsSortState'

interface Props { rows: FinishedProductRow[]; sortState?: FinishedProductsSortState }

export default function PhysicalSpecificationDetailView({ rows, sortState }: Props) {
  return (
    <div className="physical-specification-detail-view">
      <section aria-labelledby="physical-specification-summary-title">
        <h3 id="physical-specification-summary-title">规格汇总</h3>
        <PhysicalSpecificationSummaryTable rows={rows} sortState={sortState?.physicalSummary} />
      </section>
      <section aria-labelledby="physical-specification-items-title">
        <h3 id="physical-specification-items-title">逐件明细</h3>
        <InternalFinishedProductsTable rows={rows} sortState={sortState?.physicalItems} />
      </section>
    </div>
  )
}
