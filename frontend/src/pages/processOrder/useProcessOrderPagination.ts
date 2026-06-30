import { useEffect, useState } from 'react'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'

interface ProcessOrderPagination {
  current: number
  pageSize: number
  total: number
}

export function useProcessOrderPagination(defaultPageSize = 10) {
  const [configuredPageSize, setConfiguredPageSize] = useConfiguredPageSize(defaultPageSize)
  const [pagination, setPagination] = useState<ProcessOrderPagination>({
    current: 1,
    pageSize: configuredPageSize,
    total: 0,
  })

  useEffect(() => {
    setPagination((prev) => ({ ...prev, current: 1, pageSize: configuredPageSize }))
  }, [configuredPageSize])

  const resetPage = () => {
    setPagination((prev) => ({ ...prev, current: 1 }))
  }

  const changePage = (current: number, pageSize: number) => {
    setConfiguredPageSize(pageSize)
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
