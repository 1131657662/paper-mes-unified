import { Navigate, useLocation } from 'react-router-dom'
import { resolveLegacyReportLocation } from './reportLegacyRoute'

export default function ReportLegacyRedirectPage() {
  const { search } = useLocation()
  const target = resolveLegacyReportLocation(search)
  return <Navigate replace to={`${target.pathname}${target.search}`} />
}
