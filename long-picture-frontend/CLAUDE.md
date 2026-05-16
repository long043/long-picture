# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

- `npm install` — install dependencies
- `npm run dev` — start Vite dev server
- `npm run build` — type-check + production build (runs `vue-tsc` then `vite build`)
- `npm run pure-build` — production build without type-check
- `npm run type-check` — TypeScript type checking only (`vue-tsc --build`)
- `npm run lint` — ESLint with auto-fix
- `npm run format` — Prettier formatting on `src/`
- `npm run openapi` — regenerate API client code from backend OpenAPI spec (requires backend running on localhost:8123)

## Architecture

Vue 3 + TypeScript + Ant Design Vue 4 + Pinia + Vue Router. ECharts for data visualization (via `vue-echarts`). Axios for HTTP.

### Entry & Bootstrap
- `src/main.ts` — creates app, installs Pinia, Router, Ant Design Vue, VueCropper. Imports `access.ts` for route guards.
- `src/App.vue` — wraps `BasicLayout` in `a-config-provider` with Chinese locale (`zhCN`).

### Layout
- `src/layouts/BasicLayout.vue` — app shell with `GlobalHeader` (top), `GlobalSider` (left sidebar), and `<router-view>` content area.

### Routing & Auth
- `src/router/index.ts` — flat route definitions (no nested routes). Admin pages under `/admin/*`. Space detail under `/space/:id`. Picture detail under `/picture/:id`.
- `src/access.ts` — global `router.beforeEach` guard. Fetches login user on first navigation. Blocks `/admin/*` routes for non-admin users, redirecting to login with a `redirect` query param.

### State Management
- `src/stores/useLoginUserStore.ts` — single Pinia store holding `loginUser` (type `API.LoginUserVO`). Calls `getLoginUserUsingGet` from generated API code.

### API Layer (Auto-Generated)
- `openapi.config.js` — configures `@umijs/openapi` to generate TypeScript API clients from the backend Swagger endpoint (`localhost:8123/api/v2/api-docs`).
- `src/api/` — auto-generated files. Do not edit manually. Re-generate with `npm run openapi`.
  - `typings.d.ts` — all `API.*` type definitions (e.g., `API.PictureVO`, `API.Space`, `API.LoginUserVO`)
  - `*Controller.ts` — one file per backend controller, exporting typed request functions
  - `index.ts` — re-exports all controllers
- `src/request.ts` — Axios instance with `baseURL` pointing to backend, `withCredentials: true`. Response interceptor handles code `40100` (not logged in) by redirecting to `/user/login`.

### WebSocket
- `src/utils/pictureEditWebSocket.ts` — `PictureEditWebSocket` class for real-time collaborative picture editing. Connects to `ws://{host}/api/ws/picture/edit?pictureId={id}`. Event-driven: use `.on(type, handler)` to listen for message types (defined in `src/constants/picture.ts` as `PICTURE_EDIT_MESSAGE_TYPE_ENUM`).

### Constants
- `src/constants/picture.ts` — picture review status, edit message types, edit actions (zoom/rotate).
- `src/constants/space.ts` — space levels, types, roles, and permission constants. Each enum has a corresponding `_MAP` for display text and `_OPTIONS` for form selects.

### Pages
- `src/pages/` — route-level components. Admin pages in `admin/`, user auth pages in `user/`.
- `src/components/` — reusable components: `PictureUpload`, `UrlPictureUpload`, `ImageCropper`, `ImageOutPainting`, `PictureList`, `PictureSearchForm`, `BatchEditPictureModal`, `ShareModal`. Analyze chart components in `analyze/`.

### Path Alias
- `@` maps to `src/` (configured in `vite.config.ts`).

## Coding Conventions

- Chinese locale throughout the UI; commit messages in Chinese (e.g., `功能制作：图片清晰`)
- Auto-generated `src/api/` files must not be edited manually — run `npm run openapi` to regenerate
- Backend API responses follow `BaseResponse<T>` with `code` field: `0` = success, `40100` = not logged in
- Enum pattern: define `_ENUM` (values), `_MAP` (display text), `_OPTIONS` (form select items) in constants files
- Route `name` values are in Chinese (e.g., `name: '用户管理'`)
