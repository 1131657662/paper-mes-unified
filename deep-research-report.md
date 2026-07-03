# 执行摘要  
基于对现代SaaS登录页的分析，我们建议登录页采用左右分栏布局：左侧主视觉占据绝大部分宽度（约60–70%），右侧登录表单（约30–40%）保证输入域宽度充足。常见桌面宽度（1366px、1440px、1920px、2560px）均应适配。左侧等距3D工业场景插图应填满容器，中间留白确保不被裁切；背景渐变色块（或光斑）置于最底层，使用大半径高斯模糊营造虚化背景。浮层UI（KPI卡、功能点）置于插图前景，以网格布局安放（例如最多3个KPI卡，4条功能列表），透明度和对比度较低，不抢占主视觉。登录卡片居右垂直居中，采用玻璃材质（`backdrop-filter: blur()`）突出层次。以下章节给出详细的像素/百分比参数、层级关系和Codex/GPT-5提示词（中英双版），并以表格与Mermaid图示说明布局断点和层次结构，最后列出3项优化建议与验收标准。

## 左右栏宽度建议  
设计文件常用宽度为1440px或1920px，并依据显示器分辨率分别适配。如TestingBot统计，常见桌面分辨率包括1366×768、1920×1080、2560×1440等。因此建议：  
- **断点划分**：以1366px、1440px、1920px、2560px为设计参照，在这些宽度下使用百分比布局。  
- **占比比例**：左侧主视觉列占比约60–70%，右侧登录列占比30–40%。例如在1366px宽时，可取左60%（约820px）/右40%（约546px）；在1440px时取65%/35%；1920px时取60%/40%；2560px时取60%/40%。这样可保证右侧表单宽度始终在400px以上，左侧画面有充足空间展现细节。  
- **实现方式**：使用弹性布局（Flexbox）或Grid定位。例如：`display: flex; .hero {flex:0 0 65%;} .form {flex:0 0 35%;}`。通过媒体查询（@media）在各断点上调整上述百分比。登录卡片可固定宽度（如宽度360–400px）并左右居中。使用`max-width`或`min-width`限制表单宽度，保证在小屏时适度缩放。  

## 左侧插图及留白比例  
左侧主视觉为等距3D工业场景，建议在容器内占据主要可视区域（宽度约占左栏的80%，高度同样大部分充满）。为保证内容不被裁剪，**安全留白**需考虑：左右和顶部至少各留5–10%的空间（如容器宽度1366px时，左右各留68px以上）。底部可根据登录卡片高度调整。插图前应无其他强对比元素遮挡，以突出沉浸感。实现时可将插图置于相对定位容器中，并使用`object-fit`或`transform: scale()`调整尺寸。

## 背景色块/渐变与层次关系  
在最底层添加抽象色块或光斑背景，通常为大尺寸圆形或渐变形状，用来营造气氛。它们应位于插图背后（更低的`z-index`），并加大**模糊半径**（如`backdrop-filter: blur(100px)`或在Canvas中高斯模糊）以形成朦胧效果。上述背景色块颜色可为左侧主色调的渐变，透明度较低（10%–30%）以避免冲突。层次上：**背景色块层**（最底层）< **3D插图层**（中景）< **KPI/功能层**（前景）< **登录卡片层**（最高）。登录卡片可应用毛玻璃效果：`backdrop-filter: blur(20px)`让其在背景上显得凸出，同时卡片本身设置较高的`z-index`确保悬浮在最前。

## KPI卡片与功能列表布局  
左侧可以放置少量的浮层UI作为装饰或数据提醒：  
- **KPI卡**：最多3张小卡片，尺寸可设为约200×100px或响应式的16%宽*auto高。可用2列×2行网格布局，当只有3张时最后一张可单独占一行。卡片背景半透明（如白色50%+毛玻璃），字体简洁。典型文案示例：今日产量、运行机台数、出库数量等（实际文案留空）。  
- **功能列表**：最多4项垂直列表，图标+文字形式，每项行高约24px，上下间距可设16px。在左栏下方或插图左侧合适位置摆放，保证不遮挡主要插图细节。功能点如“工艺追踪”、“现场协同”等应作为静态标签出现。  
- **视觉优先级**：登录卡片最突出（鲜明对比）；3D插图为次之；KPI卡和功能点透明度更低，作为辅助信息置于前景，但配色柔和，强调不抢主题。例如可使用灰色/蓝色线框或半透明底色。Eleken示例中经典的“分屏布局”即采用了此策略，保持了视觉平衡。

## 布局断点与CSS建议  

| 断点   | 左列宽度（约）     | 右列宽度（约）     | CSS 布局与样式建议                                              |
|:------:|:---------------:|:---------------:|:-----------------------------------------------------------|
| 1366px | 左约820px (60%)  | 右约546px (40%)  | 使用 `display:flex` 实现两列布局，左列 `flex:0 0 60%`，右列 `flex:0 0 40%`；登录卡宽度固定约360px；登录卡 `z-index` 最高；左侧背景块使用 `transform: scale()` 放大后 `filter: blur(20px)`；可用 `backdrop-filter: blur(20px)` 做卡片玻璃效应。 |
| 1440px | 左约936px (65%)  | 右约504px (35%)  | 同上，左列 `flex:0 0 65%`，右列 `flex:0 0 35%`。适当增加登录卡 margin 保持居中。使用相对定位组合 `overflow: hidden` 和 `transform` 控制背景及插图位置。 |
| 1920px | 左约1152px (60%) | 右约768px (40%)  | 左列 `flex:0 0 60%`，右列 `flex:0 0 40%`；可在 CSS Grid 中设置 `grid-template-columns: 60% 40%`；登录卡采用固定宽度并 `align-self: center`；背景色块采用大尺寸 `translate` 位移，`filter: blur(50px)`。 |
| 2560px | 左约1536px (60%) | 右约1024px (40%) | 同1920px策略，左列 `flex:0 0 60%`。此时可适当增大视觉元素，如插图边距和阴影；KPI卡排列可更松散。登录卡仍固定宽度，背景块模糊半径保持。 |

- **示例CSS**：`.container {display:flex;}` + `.hero {flex:0 0 60%;}` + `.login {flex:0 0 40%; width:360px;}`。使用`z-index`控制层级（登录卡≥10，KPI卡≈5，背景≤1）。对登录卡使用`backdrop-filter: blur(20px)`和轻微阴影，插图层使用`transform: rotateY/rotateX`（参考）增强透视效果。所有留白和间距可用百分比或rem单位，保证响应式一致。

## 视觉层次关系图  
```mermaid
flowchart LR
    subgraph 左侧Hero容器
        背景色块(背景色块<br/>(Background Blob)):::back
        插图(3D工业插图):::mid
        功能列表(功能点列表):::front
        KPI卡(KPI卡片):::front
    end
    登录卡(登录卡片):::front
    classDef back fill:#e8f1ff,stroke:#a0c4ff
    classDef mid fill:#d0e8ff,stroke:#80a8ff
    classDef front fill:#ffffff,stroke:#c0c0c0
    class 背景色块,插图 back
    class KPI卡,功能列表,登录卡 front
```
*图：左侧Hero容器中背景色块最底层、3D插图次之，前景为功能列表和KPI卡（半透明）；登录表单卡片独立悬浮在右侧。*

## Codex/GPT-5 提示词  

**English Prompt:**  
```
You are a senior UI/UX designer and layout engine. Create a high-end SaaS login page for an industrial MES (paper manufacturing) system in a two-column layout:

PAGE STRUCTURE: Full-screen (100vh), no header/footer. Left column ~60-70% width (Hero visual area). Right column ~30-40% width (Login panel).

LEFT HERO: A premium isometric 3D industrial scene (paper roll factory, conveyors, warehouse, truck, digital factory). It should fill most of the left area with ample whitespace. Place any background gradients or blobs (soft large shapes) behind the scene. Add at most 3 small KPI stat cards (e.g. Today Production, Machines Running, Outbound) and one feature list (max 4 items) as subtle overlays. **No charts or dashboards.** These UI elements must be semi-transparent/subdued.

RIGHT LOGIN PANEL: A centered card with title, username and password fields, and a login button. Use placeholders for text (do NOT generate real copy). For example: Title: {LOGIN_TITLE}, Username label: {USERNAME}, etc. The card should use a frosted glass effect (`backdrop-filter: blur(20px)`) and soft shadow.

VISUAL STYLE: Minimal Apple-like SaaS style with blue-white gradient theme, consistent radius and shadow. Large whitespace. Prioritize visual hierarchy: login card highest contrast, then 3D scene, then KPI/features muted. 

PLACEHOLDERS (must not be generated): {LOGIN_TITLE}, {USERNAME_FIELD}, {PASSWORD_FIELD}, {LOGIN_BUTTON}, {KPI_TITLE1}, {KPI_TITLE2}, {KPI_TITLE3}, {FEATURE1}, ..., {FEATURE4}.
```

**Chinese Prompt:**  
```
你是资深UI设计师。请生成一个高端工业SaaS系统的登录页，两栏布局。左栏(约占宽度60%-70%)为主视觉，右栏(约占30%-40%)为登录卡片。

左侧(Hero)：一个等距3D工业场景（造纸厂、传送带、仓库、卡车等），占据左栏主要可视区域。背景加大尺寸渐变色块或模糊光斑置于最下层。允许有最多3个KPI小卡片和1个功能列表（4项内），作为前景浮层，需半透明、不抢视觉焦点。**禁止出现图表或仪表盘界面**。

右侧(登录卡)：居中卡片，含标题、用户名输入框、密码输入框、登录按钮等。标题和提示文字必须用占位符表示，不生成实际文案（如：{LOGIN_TITLE}）。卡片使用毛玻璃背景(`backdrop-filter: blur(20px)`)并带软阴影。

风格：苹果级简洁蓝白配色，大量留白，统一圆角和阴影。视觉顺序：登录卡为最突出元素；3D场景次之；KPI/功能为辅助。其中文字使用占位符，如{USERNAME_FIELD}、{PASSWORD_FIELD}、{FEATURE1_CN}等，禁止AI生成中文营销文案。
```

## 优化建议与验收标准  
1. **减少视觉杂项**：删除左侧多余的浮层元素，将KPI卡和功能点控制在3件以内。主图应更突出，KPI/功能仅为次级信息。**验收**：左栏最多3个KPI/功能项，占据主要视野；插图宽度超过左栏60%。  
2. **提升对比度和清晰度**：确保登录卡为视觉焦点，使用高对比度文字和玻璃背景，其它文本颜色柔和。**验收**：登录表单为最醒目元素（按钮显眼，标题易读）；左侧文字与背景对比度低于登录卡。  
3. **统一风格与留白**：保持统一配色、圆角和阴影，使用充足留白营造简洁感。**验收**：卡片边距≥24px；圆角、阴影和字体风格一致；整体无杂色或多余边框。

**参考文献：** 登录页布局和画面设计需遵循现代响应式最佳实践，并使用CSS特性如Flex/Grid分栏布局、`backdrop-filter`玻璃效果、3D `transform`透视等来实现上述效果。 

