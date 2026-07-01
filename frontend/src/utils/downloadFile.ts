interface DownloadResponse {
  data: Blob
  headers: Record<string, string>
}

export interface DownloadResult {
  filename: string
  size: number
}

export async function downloadFileFromResponse(
  response: DownloadResponse,
  fallbackFilename: string,
): Promise<DownloadResult> {
  await throwIfErrorBlob(response.data, response.headers)

  if (response.data.size <= 0) {
    throw new Error('导出文件为空，请调整筛选条件后重试')
  }

  const filename = filenameFromDisposition(response.headers['content-disposition']) || fallbackFilename
  downloadBlob(response.data, filename)
  return { filename, size: response.data.size }
}

async function throwIfErrorBlob(blob: Blob, headers: Record<string, string>) {
  const contentType = headers['content-type'] || blob.type
  if (!contentType.includes('json') && !contentType.includes('text')) {
    return
  }

  const text = await blob.text()
  if (!text) {
    return
  }

  let body: { code?: number; message?: string } | null = null
  try {
    body = JSON.parse(text) as { code?: number; message?: string }
  } catch {
    throw new Error(text)
  }

  throw new Error(body.message || '导出失败，接口未返回文件')
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function filenameFromDisposition(disposition?: string) {
  if (!disposition) return undefined
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  return match?.[1] ? decodeURIComponent(match[1]) : undefined
}
