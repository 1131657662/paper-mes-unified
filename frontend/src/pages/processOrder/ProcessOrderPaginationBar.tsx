import MesPaginationBar from '../../components/biz/MesPaginationBar'

interface Props {
  current: number
  pageSize: number
  total: number
  onChange: (page: number, pageSize: number) => void
}

export default function ProcessOrderPaginationBar({
  current,
  onChange,
  pageSize,
  total,
}: Props) {
  return (
    <MesPaginationBar
      className="process-order-pagination"
      current={current}
      pageSize={pageSize}
      total={total}
      onChange={onChange}
    />
  )
}
