# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend enabling type-aware lint rules by installing `oxlint-tsgolint` and editing `.oxlintrc.json`:

```json
{
  "$schema": "./node_modules/oxlint/configuration_schema.json",
  "plugins": ["react", "typescript", "oxc"],
  "options": {
    "typeAware": true
  },
  "rules": {
    "react/rules-of-hooks": "error",
    "react/only-export-components": ["warn", { "allowConstantExport": true }]
  }
}
```

See the [Oxlint rules documentation](https://oxc.rs/docs/guide/usage/linter/rules) for the full list of rules and categories.

## MES 本地开发

默认开发服务器只接受 `localhost` 和 `127.0.0.1`，API 代理目标为 `http://localhost:8081`。需要通过内网域名或隧道访问时，显式传入逗号分隔的 Host 白名单：

```powershell
$env:VITE_ALLOWED_HOSTS = 'localhost,127.0.0.1,mes-dev.example.com'
$env:VITE_API_PROXY_TARGET = 'http://localhost:8081'
npm run dev -- --host 0.0.0.0
```

不要将 `VITE_ALLOWED_HOSTS` 设置为 `true` 或包含不必要的通配范围。该变量只控制 Vite 开发/预览服务器的 Host 校验，不影响生产 Nginx 配置。

依赖边界与本地 Lighthouse CI 门禁：

```powershell
npm run check:dependencies
npm run check:lighthouse
```

Lighthouse 连续采样 3 次，仅审计无需业务凭据的登录页，报告写入 `artifacts/lighthouse` 且不上传第三方服务。

## OpenAPI / Orval 契约试点

Customer 基础档案的两个只读接口由后端 Springdoc 契约生成，新增、修改、删除接口仍使用手写客户端。生成代码通过 `src/api/orvalRequest.ts` 继续调用统一 `request.ts`，不得绕开 request-id、`R<T>` 解包、业务错误以及 401/403 处理。

```powershell
npm run openapi:generate
npm run openapi:check
```

## First-party anonymous RUM

Web Vitals collection is opt-in. Development builds do not send telemetry. Enable it only for a controlled production build and enable the matching backend property:

```powershell
$env:VITE_RUM_ENABLED = 'true'
npm run build
```

The browser sends only the metric name/value/rating, a route template from `src/router/routeMeta.ts`, browser major version, device tier, and effective network type to `/api/rum`. It never sends query parameters, form values, response bodies, usernames, document identifiers, or raw URLs. The backend keeps no business-table data; accepted events are written to the existing application log and are protected by an in-memory per-client rate limit. Set `PAPER_MES_RUM_ENABLED=true` only after the log collection and retention process has been reviewed.

`openapi:generate` 会运行后端 `OpenApiEnabledContractTest`，抽取 Customer GET 契约并更新 `openapi/customer-readonly.json` 与 `src/api/generated/customerReadOnly.ts`。不要手工编辑生成文件。CI 使用 `openapi:check` 阻止 Schema 或生成代码未同步的提交。

OpenAPI 与 Swagger UI 默认关闭；仅 `dev`、`test`、`internal` profile 开启，`prod` profile 明确关闭。
