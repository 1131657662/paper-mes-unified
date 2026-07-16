import type { ReactNode } from 'react'
import { Button } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'

interface MesPageHeaderProps {
  actions?: ReactNode
  backText?: string
  className?: string
  description?: ReactNode
  eyebrow?: ReactNode
  onBack?: () => void
  tags?: ReactNode
  title: ReactNode
  titleExtra?: ReactNode
}

export default function MesPageHeader({
  actions,
  backText = '返回',
  className,
  description,
  eyebrow,
  onBack,
  tags,
  title,
  titleExtra,
}: MesPageHeaderProps) {
  const mergedClassName = className ? `mes-page-header ${className}` : 'mes-page-header'

  return (
    <div className={mergedClassName}>
      <div className="mes-page-header__main">
        {eyebrow && <span className="mes-page-header__eyebrow">{eyebrow}</span>}
        <div className="mes-page-header__title-row">
          {onBack && (
            <Button className="mes-back-btn" type="text" icon={<ArrowLeftOutlined />} onClick={onBack}>
              {backText}
            </Button>
          )}
          <h1>{title}</h1>
          {titleExtra}
          {tags}
        </div>
        {description && <p className="mes-page-header__description">{description}</p>}
      </div>
      {actions && <div className="mes-page-header__actions">{actions}</div>}
    </div>
  )
}
