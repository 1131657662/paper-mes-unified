import type { ReactNode } from 'react'
import { BarChartOutlined, ClusterOutlined, SafetyCertificateOutlined, TeamOutlined } from '@ant-design/icons'
import AppLogoMark from '../../components/brand/AppLogoMark'
import { APP_BRAND } from '../../config/brand'
import loginHeroScene from '../../assets/login-paper-hero.webp'
import './LoginHero.css'

interface FeatureItem {
  description: string
  icon: ReactNode
  title: string
}

const FEATURE_ITEMS: FeatureItem[] = [
  { icon: <ClusterOutlined />, title: '加工闭环', description: '工艺配置 / 卷号追踪' },
  { icon: <TeamOutlined />, title: '现场协同', description: '机台作业 / 生产回录' },
  { icon: <BarChartOutlined />, title: '经营分析', description: '应收金额 / 产出损耗' },
  { icon: <SafetyCertificateOutlined />, title: '全程追溯', description: '从开单到结算可追溯' },
]

export function LoginHero() {
  return (
    <section className="login-hero" aria-label={`${APP_BRAND.name} 品牌展示`}>
      <div className="login-hero__content">
        <BrandBlock />
        <HeroCopy />
        <FeatureList />
      </div>
      <div className="login-hero__visual" aria-hidden="true">
        <img src={loginHeroScene} alt="" />
      </div>
    </section>
  )
}

function BrandBlock() {
  return (
    <div className="login-hero__brand">
      <span className="login-hero__brand-mark">
        <AppLogoMark size={32} />
      </span>
      <div>
        <strong>{APP_BRAND.name}</strong>
        <span>{APP_BRAND.productLine}</span>
      </div>
    </div>
  )
}

function HeroCopy() {
  return (
    <div className="login-hero__copy">
      <span className="login-hero__kicker">{APP_BRAND.tagline}</span>
      <h1>
        <span>生产、出库、结算</span>
        <span>回到同一条业务<em>链路</em></span>
      </h1>
      <p>面向卷筒纸加工现场，聚合工艺配置、机台作业、卷号追踪、出库签收和加工应收。</p>
    </div>
  )
}

function FeatureList() {
  return (
    <div className="login-hero__features" aria-label="核心能力">
      {FEATURE_ITEMS.map((item) => (
        <FeatureRow key={item.title} item={item} />
      ))}
    </div>
  )
}

function FeatureRow({ item }: { item: FeatureItem }) {
  return (
    <div className="login-hero__feature">
      <span className="login-hero__feature-icon">{item.icon}</span>
      <span>
        <strong>{item.title}</strong>
        <small>{item.description}</small>
      </span>
    </div>
  )
}
