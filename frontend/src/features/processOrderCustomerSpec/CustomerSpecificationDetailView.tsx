import type { FinishedProductRow } from '../processOrderDetail/components/finishedProductRows'
import CustomerSpecificationComparisonTable from './CustomerSpecificationComparisonTable'
import CustomerSpecificationSummaryTable from './CustomerSpecificationSummaryTable'
import type { FinishCustomerSpec } from './customerSpecTypes'
import type { FinishedProductsSortState } from '../processOrderDetail/components/useFinishedProductsSortState'

interface Props { rows: FinishedProductRow[]; specs?: FinishCustomerSpec[]; sortState?: FinishedProductsSortState }

export default function CustomerSpecificationDetailView({ rows, specs, sortState }: Props) {
  return (
    <div className="customer-specification-detail-view">
      <section aria-labelledby="customer-specification-summary-title">
        <h3 id="customer-specification-summary-title">规格汇总</h3>
        <CustomerSpecificationSummaryTable rows={rows} specs={specs} sortState={sortState?.customerSummary} />
      </section>
      <section aria-labelledby="customer-specification-items-title">
        <h3 id="customer-specification-items-title">逐件明细</h3>
        <CustomerSpecificationComparisonTable rows={rows} specs={specs} sortState={sortState?.customerItems} />
      </section>
    </div>
  )
}
