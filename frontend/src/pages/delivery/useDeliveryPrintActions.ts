import { message } from 'antd'
import { useEffect, useRef } from 'react'

interface Options {
  detailReady: boolean
  documentReady: boolean
  shouldAutoPrint: boolean
}

export function useDeliveryPrintActions(options: Options) {
  const printPreviewRef = useRef<HTMLDivElement>(null)
  const autoPrintDoneRef = useRef(false)

  const requestPrint = () => {
    if (!options.documentReady) {
      message.warning('客户单据口径尚未加载，请重试或切换到仓库实物视图')
      return
    }
    scrollToPrint(printPreviewRef.current)
    window.setTimeout(() => window.print(), 220)
  }

  useEffect(() => {
    if (!options.detailReady || !options.documentReady
      || !options.shouldAutoPrint || autoPrintDoneRef.current) return
    autoPrintDoneRef.current = true
    scrollToPrint(printPreviewRef.current)
    const timer = window.setTimeout(() => window.print(), 220)
    return () => window.clearTimeout(timer)
  }, [options.detailReady, options.documentReady, options.shouldAutoPrint])

  return { printPreviewRef, requestPrint }
}

function scrollToPrint(target: HTMLDivElement | null) {
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
