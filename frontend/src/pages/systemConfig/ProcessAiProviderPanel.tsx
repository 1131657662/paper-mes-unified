import { useActionState, useRef } from 'react'
import { Alert, Button, Descriptions, Input, Popconfirm, Space, Tag, Typography } from 'antd'
import { DeleteOutlined, KeyOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import { useDeleteProcessAiProviderKey } from '../../features/processAi/hooks/useDeleteProcessAiProviderKey'
import { useProcessAiProviderSettings } from '../../features/processAi/hooks/useProcessAiProviderSettings'
import { useUpdateProcessAiProviderKey } from '../../features/processAi/hooks/useUpdateProcessAiProviderKey'
import type { ProcessAiManagedProvider, ProcessAiProviderSettings } from '../../features/processAi/types'
import './ProcessAiProviderPanel.css'

interface ProviderDefinition {
  provider: ProcessAiManagedProvider
  title: string
  description: string
}

const PROVIDERS: ProviderDefinition[] = [
  {
    provider: 'deepseek',
    title: 'DeepSeek 主模型',
    description: '工艺解析优先使用此模型；不可用时自动切换到 GLM 兜底模型。',
  },
  {
    provider: 'zhipu',
    title: 'GLM-4.7-Flash 兜底模型',
    description: '仅在 DeepSeek 请求失败或不可用时使用，不会改变主模型配置。',
  },
]

export default function ProcessAiProviderPanel() {
  return (
    <div className="process-ai-provider-panel">
      <div className="process-ai-provider-panel__header">
        <div>
          <Typography.Title level={4}>工艺解析模型</Typography.Title>
          <Typography.Text type="secondary">
            密钥加密保存在服务器，浏览器不会回显明文。主模型失败时才会使用兜底模型。
          </Typography.Text>
        </div>
      </div>
      <div className="process-ai-provider-panel__cards">
        {PROVIDERS.map((definition) => (
          <ProviderCard key={definition.provider} definition={definition} />
        ))}
      </div>
    </div>
  )
}

function ProviderCard({ definition }: { definition: ProviderDefinition }) {
  const formRef = useRef<HTMLFormElement>(null)
  const { data: settings, isError, isFetching, refetch } = useProcessAiProviderSettings(definition.provider)
  const { mutateAsync: updateKey } = useUpdateProcessAiProviderKey()
  const { mutate: deleteKey, isPending: isDeleting } = useDeleteProcessAiProviderKey()
  const [formError, saveAction, isSaving] = useActionState(async (_: string | null, data: FormData) => {
    const apiKey = String(data.get('apiKey') ?? '').trim()
    if (apiKey.length < 8) return 'API Key 至少需要 8 个字符'
    try {
      await updateKey({ provider: definition.provider, apiKey })
      formRef.current?.reset()
      return null
    } catch {
      return '保存失败，请检查配置后重试'
    }
  }, null)

  return (
    <section className="process-ai-provider-panel__card" aria-labelledby={`${definition.provider}-title`}>
      <div className="process-ai-provider-panel__card-header">
        <div>
          <Typography.Title id={`${definition.provider}-title`} level={5}>{definition.title}</Typography.Title>
          <Typography.Text type="secondary">{definition.description}</Typography.Text>
        </div>
        <Button
          icon={<ReloadOutlined />}
          loading={isFetching}
          onClick={() => void refetch()}
          aria-label={`刷新${definition.title}状态`}
        >
          刷新
        </Button>
      </div>
      {isError ? (
        <Alert
          type="error"
          showIcon
          message="无法读取模型配置"
          action={<Button onClick={() => void refetch()}>重试</Button>}
        />
      ) : (
        <>
          <ProviderStatus settings={settings} />
          <form ref={formRef} action={saveAction} className="process-ai-provider-panel__form">
            <label htmlFor={`process-ai-${definition.provider}-api-key`}>API Key</label>
            <Space.Compact block>
              <Input.Password
                id={`process-ai-${definition.provider}-api-key`}
                name="apiKey"
                autoComplete="new-password"
                maxLength={512}
                prefix={<KeyOutlined />}
                placeholder="输入新密钥，保存后仅显示末四位"
              />
              <Button htmlType="submit" type="primary" icon={<SaveOutlined />} loading={isSaving}>
                保存密钥
              </Button>
            </Space.Compact>
            {formError && <Typography.Text type="danger">{formError}</Typography.Text>}
          </form>
          {settings?.source === 'DATABASE' && (
            <Popconfirm
              title="移除数据库密钥？"
              description="如果服务器环境变量已配置，将自动回退使用环境变量。"
              okText="移除"
              cancelText="取消"
              onConfirm={() => deleteKey({ provider: definition.provider })}
            >
              <Button danger icon={<DeleteOutlined />} loading={isDeleting}>
                移除数据库密钥
              </Button>
            </Popconfirm>
          )}
        </>
      )}
    </section>
  )
}

function ProviderStatus({ settings }: { settings?: ProcessAiProviderSettings }) {
  const sourceText = settings?.source === 'DATABASE'
    ? '数据库加密存储'
    : settings?.source === 'ENVIRONMENT' ? '服务器环境变量' : '未配置'
  return (
    <>
      <Alert
        showIcon
        type={settings?.configured ? 'success' : 'warning'}
        message={settings?.configured ? '模型凭据已配置' : '尚未配置可用的 API Key'}
        description={!settings?.databaseStorageReady ? '数据库密钥存储暂不可用，请检查服务端主密钥配置。' : undefined}
      />
      <Descriptions size="small" bordered column={2}>
        <Descriptions.Item label="服务商">{settings?.provider ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="模型">{settings?.model ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="密钥来源"><Tag>{sourceText}</Tag></Descriptions.Item>
        <Descriptions.Item label="当前密钥">{settings?.maskedApiKey ?? '未配置'}</Descriptions.Item>
        <Descriptions.Item label="功能开关">{settings?.enabled ? '已启用' : '未启用'}</Descriptions.Item>
        <Descriptions.Item label="存储状态">{settings?.databaseStorageReady ? '可用' : '不可用'}</Descriptions.Item>
      </Descriptions>
    </>
  )
}
