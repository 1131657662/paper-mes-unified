import type { ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { ErrorBoundary } from '../components/ErrorBoundary'

export default function RouteErrorBoundary({ children }: { children: ReactNode }) {
  const location = useLocation()
  return <ErrorBoundary key={location.key} mode="page">{children}</ErrorBoundary>
}
