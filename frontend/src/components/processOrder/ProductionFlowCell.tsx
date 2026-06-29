import { Typography } from 'antd'
import type { DisplayRow } from './shared/types'
import { buildProcessingFlow } from './shared/detailHelpers'

const { Text } = Typography

interface Props {
  row: DisplayRow
}

export default function ProductionFlowCell({ row }: Props) {
  const flow = buildProcessingFlow(row.mainProduction)

  if (flow.length === 0) {
    return <Text type="secondary">-</Text>
  }

  return (
    <div>
      {flow.map((step, i) => (
        <div key={i} style={{ marginBottom: i < flow.length - 1 ? 6 : 0 }}>
          <Text strong style={{ fontSize: 13 }}>
            {step.header}
          </Text>
          {step.details.length > 0 && (
            <div style={{ paddingLeft: 8, marginTop: 1 }}>
              {step.details.map((d, j) => (
                <div key={j}>
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {d}
                  </Text>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
