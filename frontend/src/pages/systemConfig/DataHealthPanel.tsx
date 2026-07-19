import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Statistic } from 'antd'
import { CheckCircleOutlined, ReloadOutlined, WarningOutlined } from '@ant-design/icons'
import { useDataHealthSummary } from '../../features/dataHealth/hooks/useDataHealthSummary'
import type { DataHealthIssue } from '../../types/dataHealth'
import DataHealthIssueTable from './DataHealthIssueTable'
import DataHealthRepairModal from './DataHealthRepairModal'
import './DataHealthPanel.css'

export default function DataHealthPanel() {
  const navigate = useNavigate()
  const { data: summary, isLoading, isFetching, refetch } = useDataHealthSummary()
  const [repairIssue, setRepairIssue] = useState<DataHealthIssue>()
  const total = (summary?.criticalCount ?? 0) + (summary?.warningCount ?? 0)
  const handleIssueAction = (issue: DataHealthIssue) => {
    if (issue.repairAction === 'OPEN_INVENTORY_WAREHOUSE_REPAIR') {
      navigate('/delivery-orders/inventory?open=unassigned')
      return
    }
    setRepairIssue(issue)
  }

  return (
    <div className="data-health-panel">
      <div className="data-health-header">
        <div className="data-health-metrics">
          <Statistic title="严重异常" value={summary?.criticalCount ?? 0} prefix={<WarningOutlined />} />
          <Statistic title="待核对" value={summary?.warningCount ?? 0} />
          <Statistic title="检查结果" value={total === 0 ? '正常' : `${total} 项`} prefix={total === 0 ? <CheckCircleOutlined /> : undefined} />
        </div>
        <Button icon={<ReloadOutlined />} loading={isFetching} onClick={() => void refetch()}>重新扫描</Button>
      </div>
      <Alert
        type={summary?.criticalCount ? 'error' : total ? 'warning' : 'success'}
        showIcon
        message={summary?.criticalCount ? '存在会影响结算或库存的数据异常' : total ? '存在需要补录或核对的数据' : '核心业务数据一致性正常'}
        description="修复操作必须填写原因并输入业务单号确认；无法自动修复的记录应由业务人员核对来源数据。"
      />
      <DataHealthIssueTable issues={summary?.issues ?? []} loading={isLoading} onRepair={handleIssueAction} />
      <DataHealthRepairModal issue={repairIssue} onClose={() => setRepairIssue(undefined)} />
    </div>
  )
}
