interface AppLogoMarkProps {
  className?: string
  size?: number
  title?: string
}

/**
 * 品牌图标：卷筒纸螺旋截面 + 向右展开的纸幅。
 * 蓝色渐变圆角方块底 + 白色线条，深色侧边栏与浅色登录页通用。
 */
export default function AppLogoMark({ className, size = 40, title }: AppLogoMarkProps) {
  return (
    <svg
      aria-hidden={title ? undefined : true}
      aria-label={title}
      className={className}
      focusable="false"
      height={size}
      role={title ? 'img' : undefined}
      viewBox="0 0 64 64"
      width={size}
    >
      {title ? <title>{title}</title> : null}
      <defs>
        <linearGradient id="paperMesLogoBg" x1="8" x2="58" y1="6" y2="60" gradientUnits="userSpaceOnUse">
          <stop stopColor="#2F8BFF" />
          <stop offset="1" stopColor="#0E5FD8" />
        </linearGradient>
      </defs>
      <rect x="2" y="2" width="60" height="60" rx="14" fill="url(#paperMesLogoBg)" />
      <path
        d="M38.26 19.72A16 16 0 1 0 26 46L57 46"
        stroke="#FFFFFF"
        strokeLinecap="round"
        strokeWidth="4.6"
      />
      <path
        d="M26 39A9 9 0 1 0 17.54 26.92"
        stroke="#FFFFFF"
        strokeLinecap="round"
        strokeOpacity=".62"
        strokeWidth="4.2"
      />
      <circle cx="26" cy="30" r="3.4" fill="#FFFFFF" />
    </svg>
  )
}
