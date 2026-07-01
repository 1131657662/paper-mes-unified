import { Pagination, Select } from 'antd'
import { mesPageSizeOptions, mesPaginationTotalText } from './mesPaginationUtils'

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
