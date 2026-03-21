# Pull Request: dev-1.1.6 → master

[中文](pr-1.1.6-cn.md)

## Overview

This PR merges branch `dev-1.1.6` into `master`, including flow execution and result cache improvements, context replication/delivery fixes, async thread-pool parameter lifecycle improvements, and the **1.1.6** stable release configuration.

---

## ✨ Key Changes

### 🔄 Flow Engine

- **Execution & cache:** Optimized process execution and result caching
- **Runtime control:** Added support for stopping flow instances
- **Engine refactor:** `FlowEngine` refactor with concurrency and waiting logic improvements
- **Loop API:** Loop condition changed from `BiFunction` to `Function`, used with `ContextBus.get()`
- **Nodes:** Improved return structure for node execution timeout and empty node IDs

### ⚡ Async & Thread Pool

- **Parameter passing:** Optimized parameter passing and cleanup for async tasks on thread pools
- **Task decoration:** `TheadHelper.getDecoratorAsync` decorates thread-pool tasks with ThreadLocal propagation
- **Shutdown hook:** Simplified thread-pool shutdown hook implementation
- **ThreadLocalBase:** New abstract class with `initExtra` for post-value-set initialization

### 📋 Context

- **Replication & delivery:** Improved context replication and propagation (Notify-related bug fix)

### 📚 Documentation & Build

- **README:** Updated `README.md` and `README_CN.md` version and Maven/Gradle dependency examples
- **pom.xml:** Added `<name>` element, config notes updated
- **Release config:** Switched to Aliyun Maven repository, removed SNAPSHOT, set version to **1.1.6**, cleaned `distributionManagement` and redundant repo blocks

---

## 🧪 Testing

- Unit tests pass (`mvn -q test`)
- Suggested smoke: run existing flow examples

---

## 📦 Version

- **1.1.6**
- Java 8+, Maven 3.x

---

## ✅ Checklist

- [ ] CI (tests / build) passes on `dev-1.1.6`
- [ ] Version is **1.1.6** (not SNAPSHOT)
- [ ] Maven Central / private registry requirements (signing, credentials) match current strategy
- [ ] Tag after merge: `v1.1.6`
