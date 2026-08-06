import request from '../../../api/request'
import type { RuntimeVersion } from '../../../types/runtime'

export const runtimeService = {
  current: () => request<RuntimeVersion>({
    url: '/api/system/runtime/version',
    method: 'get',
    silentError: true,
  }),
}
