import { useState } from 'react'
import type { PageQuery } from '../../../types/common'

const DEFAULT_DETAIL_PAGE: Required<PageQuery> = {
  current: 1,
  size: 20,
}

export function useReportDetailPagination() {
  const [pagination, setPagination] = useState(DEFAULT_DETAIL_PAGE)

  const changePage = (current: number, size: number) => {
    setPagination({ current, size })
  }

  const resetPage = () => {
    setPagination((current) => ({ ...current, current: 1 }))
  }

  return { changePage, pagination, resetPage }
}
