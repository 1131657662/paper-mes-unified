import { message } from 'antd'
import { useEffect, useRef } from 'react'

interface Options {
  detailReady: boolean
  documentReady: boolean
  documentError?: string
  shouldAutoPrint: boolean
}

export function useDeliveryPrintActions(options: Options) {
  const printPreviewRef = useRef<HTMLDivElement>(null)
  const autoPrintDoneRef = useRef(false)

  const requestPrint = () => {
    if (!options.documentReady) {
      message.warning(options.documentError || '司机单据尚未准备完成，请稍后重试')
      return
    }
    scrollToPrint(printPreviewRef.current)
    schedulePrint(printPreviewRef.current)
  }

  useEffect(() => {
    if (!options.detailReady || !options.documentReady
      || !options.shouldAutoPrint || autoPrintDoneRef.current) return
    autoPrintDoneRef.current = true
    scrollToPrint(printPreviewRef.current)
    const frame = schedulePrint(printPreviewRef.current)
    return () => window.cancelAnimationFrame(frame)
  }, [options.detailReady, options.documentReady, options.shouldAutoPrint])

  return { printPreviewRef, requestPrint }
}

function scrollToPrint(target: HTMLDivElement | null) {
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function schedulePrint(target: HTMLDivElement | null) {
  return window.requestAnimationFrame(() => printFromIsolatedRoot(target))
}

function printFromIsolatedRoot(target: HTMLDivElement | null) {
  const source = target?.querySelector('.document-print-area--delivery')
  if (!source) return void window.print()
  const printRoot = document.createElement('div')
  printRoot.className = 'delivery-print-root'
  printRoot.append(source.cloneNode(true))
  const deliveryNo = printRoot.querySelector('.delivery-print-page-footer span:first-child')?.textContent || ''
  const printedAt = printRoot.querySelector('.delivery-print-page-footer span:last-child')?.textContent || ''
  document.documentElement.style.setProperty('--delivery-print-number', JSON.stringify(deliveryNo))
  document.documentElement.style.setProperty('--delivery-print-time', JSON.stringify(printedAt))
  document.body.append(printRoot)
  try {
    window.print()
  } finally {
    document.documentElement.style.removeProperty('--delivery-print-number')
    document.documentElement.style.removeProperty('--delivery-print-time')
    printRoot.remove()
  }
}
