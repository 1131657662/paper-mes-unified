import type { CSSProperties } from 'react'
import type { AuthUser } from '../../types/auth'
import './GeneratedUserAvatar.css'

interface GeneratedUserAvatarProps {
  className?: string
  size?: number
  user: Pick<AuthUser, 'realName' | 'roleCode' | 'username' | 'uuid'> | null
}

interface AvatarPalette {
  accent: string
  background: string
  face: string
  line: string
  soft: string
  shade: string
}

interface AvatarSpec {
  cheekX: number
  hairPath: string
  mouthPath: string
  palette: AvatarPalette
}

interface AvatarStyle extends CSSProperties {
  '--user-avatar-size': string
}

const ROLE_PALETTES: Record<string, AvatarPalette> = {
  admin: { accent: '#1677ff', background: '#e8f2ff', face: '#ffe5d0', line: '#17324d', shade: '#4f74c7', soft: '#f8fbff' },
  finance: { accent: '#7c3aed', background: '#f1eafe', face: '#ffe4d6', line: '#31224a', shade: '#8b5cf6', soft: '#fbf8ff' },
  operator: { accent: '#0891b2', background: '#e7f9fb', face: '#ffe8d3', line: '#17353d', shade: '#38bdf8', soft: '#f7fdff' },
  warehouse: { accent: '#0f9f6e', background: '#e8f8ef', face: '#ffe7d1', line: '#153b2d', shade: '#34d399', soft: '#f8fffb' },
}

const EXTRA_PALETTES: readonly [AvatarPalette, AvatarPalette] = [
  { accent: '#dc6803', background: '#fff4e5', face: '#ffe8d5', line: '#3b2a1f', shade: '#f59e0b', soft: '#fffaf2' },
  { accent: '#475569', background: '#eef2f7', face: '#ffe6d5', line: '#1f2937', shade: '#94a3b8', soft: '#fbfdff' },
]

const HAIR_PATHS: readonly [string, string, string] = [
  'M25 35c2-12 11-19 24-19 12 0 22 7 24 19-5-8-13-11-24-11-10 0-18 3-24 11Z',
  'M24 36c1-13 11-21 25-21 11 0 19 6 23 16-6-3-12-3-18-1-10 3-20 2-30 6Z',
  'M24 35c3-12 13-20 26-19 12 1 20 9 21 20-7-6-14-8-22-8s-16 2-25 7Z',
]

const MOUTH_PATHS: readonly [string, string, string] = [
  'M42 53c4 3 10 3 14 0',
  'M43 54c3 2 8 2 11 0',
  'M42 52c4 4 11 4 15 0',
]

export default function GeneratedUserAvatar({ className, size = 40, user }: GeneratedUserAvatarProps) {
  const spec = createAvatarSpec(user)
  const label = `${user?.realName || user?.username || '当前用户'}头像`
  const style: AvatarStyle = { '--user-avatar-size': `${size}px` }

  return (
    <span aria-label={label} className={['generated-user-avatar', className].filter(Boolean).join(' ')} role="img" style={style}>
      <svg aria-hidden="true" focusable="false" viewBox="0 0 80 80">
        <rect width="80" height="80" rx="40" fill={spec.palette.background} />
        <path d="M10 61c12-11 28-13 47-6 7 3 12 7 17 13v12H10Z" fill={spec.palette.soft} />
        <circle cx="62" cy="19" r="8" fill={spec.palette.accent} opacity=".16" />
        <path d="M16 25c7-7 16-10 27-8" fill="none" stroke={spec.palette.accent} strokeLinecap="round" strokeWidth="3" opacity=".24" />
        <path d="M25 64c3-9 11-14 24-14s21 5 25 14v8H25Z" fill={spec.palette.shade} opacity=".92" />
        <circle cx="49" cy="39" r="20" fill={spec.palette.face} />
        <path d={spec.hairPath} fill={spec.palette.line} opacity=".9" />
        <circle cx="42" cy="41" r="2.2" fill={spec.palette.line} />
        <circle cx="56" cy="41" r="2.2" fill={spec.palette.line} />
        <path d="M49 44c-1 3-2 5-1 7" fill="none" stroke={spec.palette.line} strokeLinecap="round" strokeWidth="2" opacity=".58" />
        <path d={spec.mouthPath} fill="none" stroke={spec.palette.line} strokeLinecap="round" strokeWidth="2.4" opacity=".72" />
        <circle cx={spec.cheekX} cy="47" r="3.5" fill={spec.palette.accent} opacity=".12" />
      </svg>
    </span>
  )
}

function createAvatarSpec(user: GeneratedUserAvatarProps['user']): AvatarSpec {
  const seed = `${user?.uuid || user?.username || user?.realName || 'guest'}:${user?.roleCode || 'guest'}`
  const hash = hashSeed(seed)

  return {
    cheekX: 36 + (hash % 5),
    hairPath: pick(HAIR_PATHS, hash, 11),
    mouthPath: pick(MOUTH_PATHS, hash, 23),
    palette: resolvePalette(user?.roleCode, hash),
  }
}

function resolvePalette(roleCode: string | undefined, hash: number): AvatarPalette {
  const rolePalette = roleCode ? ROLE_PALETTES[roleCode] : undefined
  if (rolePalette) return rolePalette
  return pick(EXTRA_PALETTES, hash, 7)
}

function pick<T>(items: readonly [T, ...T[]], hash: number, offset: number): T {
  const item = items[Math.abs(hash + offset) % items.length]
  if (item !== undefined) return item
  return items[0]
}

function hashSeed(seed: string): number {
  let hash = 5381
  for (const char of seed) {
    hash = ((hash << 5) + hash + char.charCodeAt(0)) >>> 0
  }
  return hash
}
