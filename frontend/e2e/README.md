# 浏览器冒烟测试

默认验证未登录路由保护：

```powershell
npm run test:e2e
```

配置登录凭据后，测试还会在 1366×768 下逐页验收结算、回款、库存和出库专题报表，
包括 KPI、趋势与结构区、库存口径提示、控制台错误及页面横向溢出。

未设置 `PAPER_MES_E2E_BASE_URL` 时，测试会自动启动并回收本地 `5176` Vite 服务。

首次运行前安装 Chromium：

```powershell
npm run test:e2e:install
```

WCAG 关键路径门禁使用 axe 检查 `serious` 和 `critical` 级问题：

```powershell
npm run test:e2e:a11y
```

登录页始终执行。未配置测试账号时，登录后的仪表盘、加工单列表和个人中心用例会明确标记为 skipped，不能计为通过。

登录后核心页面测试只从环境变量读取测试账号，不在仓库保存凭据：

```powershell
$env:PAPER_MES_E2E_USERNAME='测试账号'
$env:PAPER_MES_E2E_PASSWORD='测试密码'
$env:PAPER_MES_E2E_BASE_URL='http://127.0.0.1:5176'
npm run test:e2e
```
