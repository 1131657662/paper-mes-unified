import { useEffect } from 'react'
import { buildDocumentTitle } from '../config/brand'

export function useDocumentTitle(pageTitle?: string): void {
  useEffect(() => {
    document.title = buildDocumentTitle(pageTitle)
  }, [pageTitle])
}
