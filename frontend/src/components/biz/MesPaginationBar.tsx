import { Pagination, Select } from 'antd'

export const mesPageSizeOptions = [10, 20, 50, 100, 200, 500, 1000]

export function mesPaginationShowTotal(total: number) {
  return `共 ${formatTotal(total)} 条`
}

export function mesPaginationTotalText(total: number) {
  return `，${mesPaginationShowTotal(total)}`
}

interface Props {
  className?: string
  current: number
  pageSize: number
  total: number
  onChange: (page: number, pageSize: number) => void
}

export default function MesPaginationBar({
  className,
  current,
  onChange,
  pageSize,
  total,
}: Props) {
  return (
    <div className={['mes-pagination', className].filter(Boolean).join(' ')}>
      <div className="mes-pagination__size">
        <span>每页</span>
        <Select
          value={pageSize}
          options={mesPageSizeOptions.map((value) => ({ value, label: value }))}
          onChange={(value) => onChange(1, value)}
          popupMatchSelectWidth={false}
          size="middle"
        />
        <span>条{mesPaginationTotalText(total)}</span>
      </div>
      <Pagination
        current={current}
        pageSize={pageSize}
        total={total}
        showSizeChanger={false}
        showTotal={undefined}
        onChange={onChange}
      />
    </div>
  )
}

function formatTotal(total: number) {
  return total.toLocaleString('zh-CN')
}
