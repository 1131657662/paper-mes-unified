import { useState } from 'react'

interface ProcessOrderPagination {
  current: number
  pageSize: number
  total: number
}

export function useProcessOrderPagination() {
  const [pagination, setPagination] = useState<ProcessOrderPagination>({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  const resetPage = () => {
    setPagination((prev) => ({ ...prev, current: 1 }))
  }

  const changePage = (current: number, pageSize: number) => {
    setPagination((prev) => ({ ...prev, current, pageSize }))
  }

  const updateTotal = (total: number) => {
    setPagination((prev) => ({ ...prev, total }))
  }

  return {
    pagination,
    resetPage,
    changePage,
    updateTotal,
  }
}
