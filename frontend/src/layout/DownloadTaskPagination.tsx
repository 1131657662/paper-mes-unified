import { Pagination } from 'antd'

interface Props {
  current: number
  onChange: (page: number, pageSize: number) => void
  size: number
  total: number
}

export default function DownloadTaskPagination({ current, onChange, size, total }: Props) {
  if (total === 0) return null
  return <Pagination size="small" current={current} pageSize={size} total={total}
    showSizeChanger pageSizeOptions={[10, 20, 50]} showTotal={(count) => `共 ${count} 条`}
    onChange={onChange} />
}
