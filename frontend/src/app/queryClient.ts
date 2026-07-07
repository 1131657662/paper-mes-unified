import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { notifyErrorOnce } from '../api/request'

export const queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onError: (error) => notifyErrorOnce(error, '操作失败，请稍后重试'),
  }),
  queryCache: new QueryCache({
    onError: (error) => notifyErrorOnce(error, '数据加载失败，请刷新后重试'),
  }),
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
})
