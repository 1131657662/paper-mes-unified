import './DashboardSkeleton.css'

const METRIC_KEYS = ['orders', 'stock', 'receivable', 'loss']
const WORK_KEYS = ['draft', 'issue', 'processing']
const RANK_KEYS = ['first', 'second', 'third', 'fourth']
const ORDER_KEYS = ['recent-a', 'recent-b', 'recent-c']

export default function DashboardSkeleton() {
  return (
    <div className="dashboard-page__content dashboard-skeleton" aria-hidden="true">
      <div className="dashboard-skeleton__metrics">
        {METRIC_KEYS.map((key) => (
          <div className="dashboard-skeleton__metric" key={key}>
            <span className="dashboard-skeleton__line dashboard-skeleton__line--short" />
            <span className="dashboard-skeleton__line dashboard-skeleton__line--strong" />
            <span className="dashboard-skeleton__line" />
          </div>
        ))}
      </div>
      <div className="dashboard-page__analysis-grid">
        <div className="dashboard-skeleton__panel dashboard-skeleton__panel--chart">
          <SkeletonPanelHead />
          <div className="dashboard-skeleton__summary">
            <span />
            <span />
            <span />
          </div>
          <div className="dashboard-skeleton__chart" />
        </div>
        <div className="dashboard-skeleton__panel">
          <SkeletonPanelHead />
          <div className="dashboard-skeleton__shortcuts">
            {WORK_KEYS.map((key) => <span key={key} />)}
          </div>
          <div className="dashboard-skeleton__stack">
            {ORDER_KEYS.map((key) => <span key={key} />)}
          </div>
        </div>
      </div>
      <div className="dashboard-page__work-grid">
        {WORK_KEYS.map((key) => (
          <div className="dashboard-skeleton__panel" key={key}>
            <SkeletonPanelHead />
            <div className="dashboard-skeleton__stack">
              {ORDER_KEYS.map((rowKey) => <span key={`${key}-${rowKey}`} />)}
            </div>
          </div>
        ))}
      </div>
      <div className="dashboard-page__bottom-grid">
        <DashboardListSkeleton />
        <DashboardRankSkeleton />
        <DashboardRankSkeleton />
      </div>
    </div>
  )
}

function SkeletonPanelHead() {
  return (
    <div className="dashboard-skeleton__head">
      <span />
      <span />
    </div>
  )
}

function DashboardListSkeleton() {
  return (
    <div className="dashboard-skeleton__panel">
      <SkeletonPanelHead />
      <div className="dashboard-skeleton__list">
        {ORDER_KEYS.map((key) => <span key={key} />)}
      </div>
    </div>
  )
}

function DashboardRankSkeleton() {
  return (
    <div className="dashboard-skeleton__panel">
      <SkeletonPanelHead />
      <div className="dashboard-skeleton__rank">
        {RANK_KEYS.map((key) => <span key={key} />)}
      </div>
    </div>
  )
}
