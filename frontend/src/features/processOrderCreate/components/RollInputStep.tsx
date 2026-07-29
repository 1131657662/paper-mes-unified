import { Button, Card, Modal, Space, Table, Typography, Upload, message } from 'antd'
import { DownloadOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import { useState } from 'react'
import { mesTablePagination } from '../../../components/biz/mesPaginationUtils'
import type { OriginalRollImportPreviewVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import { newRollDraft, rollDraftFromDto, totalWeight } from '../draftMappers'
import { mergeImportedRolls } from '../rollInputModel'
import RollInputTable from './RollInputTable'
import './CreateOrderEditors.css'

interface Props {
  rolls: RollDraft[]
  loading: boolean
  onChange: (rolls: RollDraft[]) => void
  onImportPreview: (file: File) => Promise<OriginalRollImportPreviewVO>
  onPrev: () => void
  onNext: () => void
}

export default function RollInputStep({ rolls, loading, onChange, onImportPreview, onPrev, onNext }: Props) {
  const [preview, setPreview] = useState<OriginalRollImportPreviewVO>()

  const handleImport = async (file: File) => {
    const result = await onImportPreview(file)
    setPreview(result)
    return false
  }

  const applyImport = () => {
    const imported = (preview?.validRows ?? []).map(rollDraftFromDto)
    onChange(mergeImportedRolls(rolls, imported))
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
        <RollInputTable rolls={rolls} onChange={onChange} />
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
