
# 开发规范指南

为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目环境与基础配置

### 1.1 环境信息
- **操作系统**：Mac OS X
- **工作区路径**：`/Users/luyue/IdeaProjects/Ms/platform-plugins-main`
- **Java 版本**：JDK 21.0.11
- **开发语言**：Java
- **构建工具**：Maven
- **作者**：luyue

### 1.2 目录结构
本项目的目录结构如下：
```text
platform-plugins-main
└── metersphere-jira-plugin
    └── src
        └── main
            ├── java
            │   └── io
            │       └── metersphere
            │           └── plugin
            │               └── jira
            │                   ├── client          # Jira 客户端相关代码
            │                   ├── constants        # 常量定义
            │                   ├── domain            # 领域对象与实体
            │                   ├── enums             # 枚举定义
            │                   └── impl              # 实现类（接口实现）
            └── resources
                ├── script          # 脚本文件
                └── static          # 静态资源文件
```

## 二、技术栈要求

- **主框架**：自定义 Plugin 框架 (Metersphere Platform SDK)
- **语言版本**：Java 21
- **构建工具**：Maven 3.x
- **核心依赖**：
  - `io.metersphere:metersphere-plugin-platform-sdk` (scope: provided)
  - `org.jsoup:jsoup` (用于 HTML 解析)

## 三、编码规范

### 3.1 代码作者
- 所有代码的提交作者应填写为：**luyue**

### 3.2 注释规范
- 所有类、方法、字段需添加 **Javadoc** 注释。
- 代码注释应使用开发者的第一语言（中文）编写，以清晰说明业务逻辑和参数含义。

### 3.3 包结构与命名规范

| 包名/目录                 | 说明                                 | 示例                         |
|--------------------------|--------------------------------------|------------------------------|
| `client`                 | 外部服务（Jira）的客户端调用封装     | `JiraClient`                 |
| `constants`              | 系统常量、配置项定义                 | `JiraConstants`              |
| `domain`                 | 数据领域模型（Entity/DTO）           | `JiraIssue`                  |
| `enums`                  | 枚举类型定义                         | `JiraIssueStatus`            |
| `impl`                   | 业务逻辑实现类（需对应接口）         | `JiraPluginImpl`             |

### 3.4 命名规范

| 类型       | 命名方式             | 示例                  |
|------------|----------------------|-----------------------|
| 类名       | UpperCamelCase       | `JiraClient`          |
| 方法/变量  | lowerCamelCase       | `connectToJira()`     |
| 常量       | UPPER_SNAKE_CASE     | `DEFAULT_TIMEOUT`     |

### 3.5 Maven 构建规范

- **依赖管理**：使用 Maven 统一管理依赖版本，禁止硬编码版本号。
- **打包配置**：项目使用 `maven-assembly-plugin` 打包，生成的 JAR 包需包含依赖（`jar-with-dependencies`）。
- **插件清单**：确保 `pom.xml` 中的 `<properties>` 配置（如 `plugin.id`, `plugin.class`）正确，以便框架识别。

## 四、项目架构规范

### 4.1 分层架构

| 层级        | 职责说明                         | 开发约束与注意事项                                               |
|-------------|----------------------------------|------------------------------------------------------------------|
| **Plugin (入口)** | 实现框架规定的插件接口，作为插件入口 | 需继承框架提供的接口，并在 `pom.xml` 中配置对应的 `plugin.class` |
| **Client**  | 封装与 Jira 系统的 HTTP/REST 交互 | 处理网络请求、认证、序列化/反序列化；建议使用对象池复用连接      |
| **Service/Logic** | 封装具体的业务逻辑               | 处理数据转换、状态判断等逻辑                                     |
| **Domain**  | 定义数据模型                     | 映射 Jira 的 Issue、User 等数据结构                              |

### 4.2 接口与实现分离

- 所有业务逻辑类（如 `JiraClient`、业务服务类）建议定义接口，具体实现放在接口所在包下的 `impl` 子包中。

## 五、开发原则

| 原则       | 说明                                       |
|------------|--------------------------------------------|
| **SOLID**  | 高内聚、低耦合，增强可维护性与可扩展性     |
| **DRY**    | 避免重复代码，提高复用性                   |
| **KISS**   | 保持代码简洁易懂                           |
| **YAGNI**  | 不实现当前不需要的功能                     |
| **OWASP**  | 防范常见安全漏洞（如使用 JSoup 处理 HTML 时的 XSS 风险） |

## 六、安全与性能规范

### 6.1 输入校验与安全

- **XSS 防护**：若使用 JSoup 处理来自用户输入的 HTML 内容，务必使用 `Jsoup.clean()` 方法进行净化，防止 XSS 攻击。
- **依赖安全**：定期检查 `jsoup` 等依赖库的版本，及时更新修复已知漏洞。
