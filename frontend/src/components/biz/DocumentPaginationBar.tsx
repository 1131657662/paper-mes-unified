import MesPaginationBar from './MesPaginationBar'

interface Props {
  current: number
  pageSize: number
  total: number
  onChange: (page: number, pageSize: number) => void
}

export default function DocumentPaginationBar({ current, onChange, pageSize, total }: Props) {
  return (
    <MesPaginationBar
      className="document-pagination"
      current={current}
      pageSize={pageSize}
      total={total}
      onChange={onChange}
    />
  )
}
