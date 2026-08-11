import { Profiler, type ProfilerOnRenderCallback, type ReactNode } from 'react'

const MAX_COMMITS = 256
const profilerEnabled = import.meta.env.VITE_PERF_PROFILER_ENABLED === 'true'

export interface PerfProfilerCommit {
  actualDuration: number
  baseDuration: number
  commitTime: number
  id: string
  phase: 'mount' | 'nested-update' | 'update'
  startTime: number
}

declare global {
  interface Window {
    __MES_PERF_PROFILER__?: PerfProfilerCommit[]
  }
}

interface Props {
  children: ReactNode
  id: string
}

export function PerfProfiler({ children, id }: Props) {
  if (!profilerEnabled) return children
  return <Profiler id={id} onRender={recordCommit}>{children}</Profiler>
}

const recordCommit: ProfilerOnRenderCallback = (id, phase, actualDuration, baseDuration, startTime, commitTime) => {
  if (typeof window === 'undefined') return
  const commits = window.__MES_PERF_PROFILER__ ?? []
  commits.push({ actualDuration, baseDuration, commitTime, id, phase, startTime })
  if (commits.length > MAX_COMMITS) commits.splice(0, commits.length - MAX_COMMITS)
  window.__MES_PERF_PROFILER__ = commits
}
