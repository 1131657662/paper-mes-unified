# 浏览器冒烟测试

默认验证未登录路由保护：

```powershell
npm run test:e2e
```

未设置 `PAPER_MES_E2E_BASE_URL` 时，测试会自动启动并回收本地 `5176` Vite 服务。

首次运行前安装 Chromium：

```powershell
npm run test:e2e:install
```

登录后核心页面测试只从环境变量读取测试账号，不在仓库保存凭据：

```powershell
$env:PAPER_MES_E2E_USERNAME='测试账号'
$env:PAPER_MES_E2E_PASSWORD='测试密码'
$env:PAPER_MES_E2E_BASE_URL='http://127.0.0.1:5176'
npm run test:e2e
```
