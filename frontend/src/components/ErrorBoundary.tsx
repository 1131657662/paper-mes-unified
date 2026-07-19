import React, { Component } from 'react'
import type { ReactNode } from 'react'
import { Result, Button } from 'antd'
import { HomeOutlined, ReloadOutlined } from '@ant-design/icons'
import './ErrorBoundary.css'

interface Props {
  children: ReactNode
  mode?: 'app' | 'page'
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      hasError: false,
      error: null,
    }
  }

  static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      error,
    }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('ErrorBoundary caught error:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: null })
  }

  handleGoHome = () => {
    window.location.href = '/'
  }

  render() {
    if (this.state.hasError) return this.renderFallback()

    return this.props.children
  }

  private renderFallback() {
    const pageMode = this.props.mode === 'page'
    return (
      <div className={pageMode ? 'mes-error-boundary mes-error-boundary--page' : 'mes-error-boundary'}>
        <Result
          status="error"
          title={pageMode ? '当前页面暂时无法显示' : '页面加载失败'}
          subTitle="页面内容发生异常，其他业务数据不受影响。"
          extra={pageMode ? this.pageActions() : this.appActions()}
        />
      </div>
    )
  }

  private pageActions() {
    return <Button type="primary" icon={<ReloadOutlined />} onClick={this.handleRetry}>重试当前页面</Button>
  }

  private appActions() {
    return [
      <Button type="primary" icon={<ReloadOutlined />} key="reload" onClick={this.handleReload}>刷新页面</Button>,
      <Button icon={<HomeOutlined />} key="home" onClick={this.handleGoHome}>返回首页</Button>,
    ]
  }
}
