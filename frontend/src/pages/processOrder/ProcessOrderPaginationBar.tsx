import { Pagination, Select } from 'antd'

const pageSizeOptions = [10, 20, 50, 100, 200, 500, 1000]

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
    <div className="process-order-pagination">
      <div className="process-order-pagination__size">
        <span>每页</span>
        <Select
          value={pageSize}
          options={pageSizeOptions.map((value) => ({ value, label: value }))}
          onChange={(value) => onChange(1, value)}
          popupMatchSelectWidth={false}
          size="middle"
        />
        <span>条，共 {formatTotal(total)} 条</span>
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
