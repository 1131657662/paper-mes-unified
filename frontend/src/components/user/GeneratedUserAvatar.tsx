import type { CSSProperties } from 'react'
import type { AuthUser } from '../../types/auth'
import './GeneratedUserAvatar.css'

interface GeneratedUserAvatarProps {
  className?: string
  size?: number
  user: Pick<AuthUser, 'realName' | 'roleCode' | 'username' | 'uuid'> | null
}

interface MascotPalette {
  accent: string
  background: string
  body: string
  line: string
}

interface AvatarStyle extends CSSProperties {
  '--user-avatar-size': string
}

const ROLE_PALETTES: Record<string, MascotPalette> = {
  admin: { accent: '#9bb9d6', background: '#eaf2f8', body: '#5f8fba', line: '#24465f' },
  finance: { accent: '#9fc8b8', background: '#edf6f2', body: '#5b9b83', line: '#285345' },
  operator: { accent: '#aebdca', background: '#eef2f5', body: '#6f8497', line: '#34495a' },
  warehouse: { accent: '#d9b58b', background: '#faf1e7', body: '#b77a43', line: '#624326' },
}

const DEFAULT_PALETTE: MascotPalette = {
  accent: '#aebdca',
  background: '#eef2f5',
  body: '#6f8497',
  line: '#34495a',
}

export default function GeneratedUserAvatar({ className, size = 40, user }: GeneratedUserAvatarProps) {
  const palette = ROLE_PALETTES[user?.roleCode ?? ''] ?? DEFAULT_PALETTE
  const label = `${user?.realName || user?.username || '当前用户'}头像`
  const style: AvatarStyle = { '--user-avatar-size': `${size}px` }

  return (
    <span aria-label={label} className={['generated-user-avatar', className].filter(Boolean).join(' ')} role="img" style={style}>
      <svg aria-hidden="true" focusable="false" viewBox="0 0 80 80">
        <rect width="80" height="80" rx="40" fill={palette.background} />
        <circle cx="40" cy="15" r="4" fill={palette.accent} />
        <path d="M40 18v7" stroke={palette.line} strokeLinecap="round" strokeWidth="3" />
        <path d="M18 70V45c0-14 9-23 22-23s22 9 22 23v25Z" fill={palette.body} />
        <path d="M18 56c7 5 14 7 22 7s15-2 22-7v14H18Z" fill={palette.accent} opacity=".48" />
        <circle cx="40" cy="40" r="13" fill="#fff" />
        <circle cx="40" cy="40" r="5.5" fill={palette.line} />
        <circle cx="42" cy="38" r="1.8" fill="#fff" opacity=".9" />
        <path d="M34 55c4 3 8 3 12 0" fill="none" stroke={palette.line} strokeLinecap="round" strokeWidth="3" />
      </svg>
    </span>
  )
}
