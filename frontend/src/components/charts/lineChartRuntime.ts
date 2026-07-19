import { LineChart } from 'echarts/charts'
import { AriaComponent, GridComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { SVGRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, AriaComponent, SVGRenderer])

export default echarts
