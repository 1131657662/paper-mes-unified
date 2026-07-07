import { rawRequest } from './request'

export function protectedFileApiUrl(path: string): string {
  const value = path.trim()
  if (!value) return value
  if (/^https?:\/\//i.test(value)) {
    throw new Error('不支持外部文件地址')
  }
  if (value.startsWith('/api/files/')) return value
  const relativePath = value
    .replace(/^\/files\//, '')
    .replace(/^files\//, '')
    .replace(/^\/+/, '')
  return `/api/files/${relativePath}`
}

export async function fetchProtectedFileBlob(path: string): Promise<Blob> {
  const response = await rawRequest.request<Blob, { data: Blob }>({
    url: protectedFileApiUrl(path),
    method: 'get',
    responseType: 'blob',
  })
  return response.data
}
