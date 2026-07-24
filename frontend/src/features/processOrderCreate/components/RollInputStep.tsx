import { Button, Card, Input, InputNumber, Modal, Space, Table, Typography, Upload, message } from 'antd'
import { CopyOutlined, DeleteOutlined, DownloadOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import type { ColumnType } from 'antd/es/table'
import { useState } from 'react'
import ResizableTable from '../../../components/ResizableTable'
import MesTooltip from '../../../components/biz/MesTooltip'
import { mesTablePagination } from '../../../components/biz/mesPaginationUtils'
import type { OriginalRollImportPreviewVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import { newRollDraft, rollDraftFromDto, totalWeight } from '../draftMappers'
import './CreateOrderEditors.css'

interface Props {
  rolls: RollDraft[]
  loading: boolean
  onChange: (rolls: RollDraft[]) => void
  onImportPreview: (file: File) => Promise<OriginalRollImportPreviewVO>
  onPrev: () => void
  onNext: () => void
}

function updateField<K extends keyof RollDraft>(
  rolls: RollDraft[],
  localId: string,
  key: K,
  value: RollDraft[K],
) {
  return rolls.map((roll) => (roll.localId === localId ? { ...roll, [key]: value } : roll))
}

export default function RollInputStep({ rolls, loading, onChange, onImportPreview, onPrev, onNext }: Props) {
  const [preview, setPreview] = useState<OriginalRollImportPreviewVO>()
  const columns: ColumnType<RollDraft>[] = [
    {
      title: '品名',
      dataIndex: 'paperName',
      width: 130,
      render: (_, roll, index) => (
        <Input aria-label={`母卷 ${index + 1} 品名`} value={roll.paperName} onChange={(event) => onChange(updateField(rolls, roll.localId, 'paperName', event.target.value))} />
      ),
    },
    {
      title: '克重(g)',
      dataIndex: 'gramWeight',
      width: 90,
      render: (_, roll, index) => (
        <InputNumber aria-label={`母卷 ${index + 1} 克重`} min={1} value={roll.gramWeight} onChange={(value) => onChange(updateField(rolls, roll.localId, 'gramWeight', value ?? 1))} />
      ),
    },
    {
      title: '门幅(mm)',
      dataIndex: 'originalWidth',
      width: 132,
      minWidth: 132,
      render: (_, roll, index) => (
        <InputNumber className="roll-width-input" aria-label={`母卷 ${index + 1} 门幅`} min={1} value={roll.originalWidth} onChange={(value) => onChange(updateField(rolls, roll.localId, 'originalWidth', value ?? 1))} />
      ),
    },
    {
      title: '直径(in)',
      dataIndex: 'originalDiameter',
      width: 95,
      render: (_, roll, index) => (
        <InputNumber aria-label={`母卷 ${index + 1} 直径`} min={0} value={roll.originalDiameter} onChange={(value) => onChange(updateField(rolls, roll.localId, 'originalDiameter', value ?? undefined))} />
      ),
    },
    {
      title: '纸芯(in)',
      dataIndex: 'coreDiameter',
      width: 90,
      render: (_, roll, index) => (
        <InputNumber aria-label={`母卷 ${index + 1} 纸芯`} min={0} value={roll.coreDiameter} onChange={(value) => onChange(updateField(rolls, roll.localId, 'coreDiameter', value ?? undefined))} />
      ),
    },
    {
      title: '母卷号',
      dataIndex: 'rollNo',
      width: 120,
      render: (_, roll, index) => (
        <Input aria-label={`母卷 ${index + 1} 卷号`} value={roll.rollNo} onChange={(event) => onChange(updateField(rolls, roll.localId, 'rollNo', event.target.value))} />
      ),
    },
    {
      title: '编号',
      dataIndex: 'extraNo',
      width: 110,
      render: (_, roll, index) => (
        <Input aria-label={`母卷 ${index + 1} 编号`} value={roll.extraNo} onChange={(event) => onChange(updateField(rolls, roll.localId, 'extraNo', event.target.value))} />
      ),
    },
    {
      title: '件数',
      dataIndex: 'pieceNum',
      width: 80,
      render: (_, roll, index) => (
        <InputNumber aria-label={`母卷 ${index + 1} 件数`} min={1} value={roll.pieceNum} onChange={(value) => onChange(updateField(rolls, roll.localId, 'pieceNum', value ?? 1))} />
      ),
    },
    {
      title: '单重(kg)',
      dataIndex: 'rollWeight',
      width: 100,
      render: (_, roll, index) => (
        <InputNumber aria-label={`母卷 ${index + 1} 单重`} min={0} precision={3} value={roll.rollWeight} onChange={(value) => onChange(updateField(rolls, roll.localId, 'rollWeight', value ?? 0))} />
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 140,
      render: (_, roll, index) => (
        <Input aria-label={`母卷 ${index + 1} 备注`} value={roll.remark} onChange={(event) => onChange(updateField(rolls, roll.localId, 'remark', event.target.value))} />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 88,
      render: (_, roll) => (
        <Space>
          <MesTooltip title="复制原纸">
            <Button
              aria-label="复制原纸"
              icon={<CopyOutlined />}
              size="small"
              onClick={() => onChange([...rolls, newRollDraft(roll)])}
            />
          </MesTooltip>
          <MesTooltip title={rolls.length <= 1 ? '至少保留一条原纸' : '删除原纸'}>
            <Button
              danger
              aria-label="删除原纸"
              icon={<DeleteOutlined />}
              size="small"
              disabled={rolls.length <= 1}
              onClick={() => onChange(rolls.filter((item) => item.localId !== roll.localId))}
            />
          </MesTooltip>
        </Space>
      ),
    },
  ]

  const handleImport = async (file: File) => {
    const result = await onImportPreview(file)
    setPreview(result)
    return false
  }

  const applyImport = () => {
    const imported = (preview?.validRows ?? []).map(rollDraftFromDto)
    onChange([...rolls, ...imported])
    setPreview(undefined)
    message.success(`已导入 ${imported.length} 条有效原纸`)
  }

  const downloadTemplate = () => {
    const csv = '\uFEFF品名,克重,门幅,卷号,单重,编号,批次,直径,纸芯,件数,损伤,备注\n'
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = '原纸导入模板.csv'
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return (
    <>
      <Card
        title="原纸录入"
        extra={
          <Space>
            <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载模板</Button>
            <Upload showUploadList={false} beforeUpload={(file) => handleImport(file)}>
              <Button icon={<UploadOutlined />}>批量导入</Button>
            </Upload>
            <Button icon={<PlusOutlined />} onClick={() => onChange([...rolls, newRollDraft()])}>添加原纸</Button>
          </Space>
        }
      >
        <ResizableTable storageKey="unified_order_rolls" rowKey="localId" size="small" pagination={false} columns={columns} dataSource={rolls} />
        <div className="create-editor-footer">
          <Typography.Text strong>合计：{rolls.length} 卷 / {formatKg(totalWeight(rolls))}</Typography.Text>
          <Space>
            <Button onClick={onPrev}>上一步</Button>
            <Button type="primary" loading={loading} onClick={onNext}>下一步：加工方式</Button>
          </Space>
        </div>
      </Card>
      <Modal
        title="导入预览"
        open={Boolean(preview)}
        onCancel={() => setPreview(undefined)}
        onOk={applyImport}
        okButtonProps={{ disabled: !(preview?.validRows?.length) }}
        okText="应用有效行"
      >
        <Typography.Paragraph>
          有效 {preview?.validRows?.length ?? 0} 行，错误 {preview?.errors?.length ?? 0} 行
        </Typography.Paragraph>
        <Table
          className="mes-inline-pagination-table"
          size="small"
          rowKey={(record) => `${record.rowNumber}-${record.field}`}
          pagination={mesTablePagination(10)}
          columns={[
            { title: '行号', dataIndex: 'rowNumber', width: 70 },
            { title: '字段', dataIndex: 'field', width: 90 },
            { title: '错误', dataIndex: 'message' },
          ]}
          dataSource={preview?.errors ?? []}
        />
      </Modal>
    </>
  )
}
