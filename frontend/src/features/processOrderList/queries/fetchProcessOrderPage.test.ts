import { QueryClient } from '@tanstack/react-query'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { pageProcessOrders } from '../../../api/processOrder'
import type { PageResult } from '../../../types/common'
import type { ProcessOrder } from '../../../types/processOrder'
import { fetchProcessOrderPage } from './fetchProcessOrderPage'

vi.mock('../../../api/processOrder', () => ({
  pageProcessOrders: vi.fn(),
}))

const mockedPageProcessOrders = vi.mocked(pageProcessOrders)

describe('process order list query', () => {
  beforeEach(() => {
    mockedPageProcessOrders.mockReset()
  })

  it('deduplicates concurrent requests with the same parameters', async () => {
    const queryClient = createQueryClient()
    const result = createPageResult()
    let resolveRequest: ((value: PageResult<ProcessOrder>) => void) | undefined
    mockedPageProcessOrders.mockImplementation(() => new Promise((resolve) => {
      resolveRequest = resolve
    }))

    const firstRequest = fetchProcessOrderPage(queryClient, { current: 1, size: 20 })
    const secondRequest = fetchProcessOrderPage(queryClient, { current: 1, size: 20 })
    resolveRequest?.(result)

    await expect(Promise.all([firstRequest, secondRequest])).resolves.toEqual([result, result])
    expect(mockedPageProcessOrders).toHaveBeenCalledTimes(1)
  })

  it('refetches a completed request when ProTable reloads', async () => {
    const queryClient = createQueryClient()
    mockedPageProcessOrders.mockResolvedValue(createPageResult())

    await fetchProcessOrderPage(queryClient, { current: 1, size: 20 })
    await fetchProcessOrderPage(queryClient, { current: 1, size: 20 })

    expect(mockedPageProcessOrders).toHaveBeenCalledTimes(2)
  })

  it('keeps filter and page variants in separate cache entries', async () => {
    const queryClient = createQueryClient()
    mockedPageProcessOrders.mockResolvedValue(createPageResult())

    await Promise.all([
      fetchProcessOrderPage(queryClient, { current: 1, size: 20, keyword: 'A' }),
      fetchProcessOrderPage(queryClient, { current: 2, size: 20, keyword: 'A' }),
      fetchProcessOrderPage(queryClient, { current: 1, size: 20, keyword: 'B' }),
    ])

    expect(queryClient.getQueryCache().getAll()).toHaveLength(3)
  })
})

function createQueryClient(): QueryClient {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } })
}

function createPageResult(): PageResult<ProcessOrder> {
  return { current: 1, records: [], size: 20, total: 0 }
}
