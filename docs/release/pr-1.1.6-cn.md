# Pull Request: dev-1.1.6 → master

[English](pr-1.1.6.md)

## 概述 / Overview

本 PR 将 `dev-1.1.6` 分支合并至 `master`，包含流程执行与缓存优化、上下文传递机制修复、异步线程池参数传递与清理、以及 1.1.6 正式版发布配置等多项增强。

---

## ✨ 主要变更 / Key Changes

### 🔄 流程引擎 (Flow Engine)

- **执行与缓存**：优化流程执行与结果缓存机制
- **运行控制**：支持停止流程实例
- **Engine 重构**：`FlowEngine` 重构，并发与等待逻辑优化
- **循环结构**：循环条件由 `BiFunction` 调整为 `Function`，配合 `ContextBus.get()` 使用
- **节点行为**：优化节点执行超时与空节点 ID 的返回结构

### ⚡ 异步与线程池 (Async & Thread Pool)

- **参数传递**：优化线程池中异步任务执行时的参数传递与清理逻辑
- **任务装饰**：`TheadHelper.getDecoratorAsync` 装饰线程池任务，支持 ThreadLocal 传播
- **Shutdown hook**：简化线程池关闭钩子实现
- **ThreadLocalBase**：新增抽象类，支持设置值后通过 `initExtra` 执行额外初始化

### 📋 上下文 (Context)

- **复制与传递**：优化上下文复制与传递机制（Notify 相关 BUG 修复）

### 📚 文档与构建 (Documentation & Build)

- **README**：`README.md`、`README_CN.md` 版本号与 Maven/Gradle 依赖示例更新
- **pom.xml**：新增 `<name>` 元素，配置说明完善
- **发布配置**：切换至阿里云 Maven 仓库，去除 SNAPSHOT，版本定为 **1.1.6**，清理 `distributionManagement` 冗余配置

---

## 🧪 测试 (Testing)

- `mvn -q test` 单元测试通过
- 建议运行现有流程示例做冒烟验证

---

## 📦 版本 (Version)

- 发布版本：**1.1.6**
- Java 8+，Maven 3.x

---

## ✅ 检查清单 (Checklist)

- [ ] CI（单元测试 / 构建）在 `dev-1.1.6` 上通过
- [ ] 版本号确认为 **1.1.6**（非 SNAPSHOT）
- [ ] 发布到 Maven Central / 私服所需的 `pom`、签名、账号等已与当前策略一致
- [ ] 合并后打 Tag：`v1.1.6`
