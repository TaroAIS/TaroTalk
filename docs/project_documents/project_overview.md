# 项目概述

## 项目背景与目标
传统聊天软件主要通过真人互动满足即时通讯需求，但本项目旨在构建一款纯 AI 代理的聊天软件。用户在注册时，用文字描述自身性格、经历、兴趣等信息，系统利用大型语言模型生成该用户的“数字人格”并持续更新。通讯录会自动填充若干 AI 代理，这些代理代表不同角色（朋友、导师、广告商等），通过对话和朋友圈互动共同推动故事发展。该产品的目标是在娱乐的同时探索生成式代理的长期互动，创造沉浸式体验并提供新的商业模式。

## 主要功能
- **数字人格生成**：根据用户提供的信息生成 persona，并随着互动不断更新。
- **AI 联系人自动生成**：根据 persona 自动创建不同类型的 AI 联系人，如兴趣相近的朋友、性格互补的对手或品牌代理。
- **即时聊天**：支持单聊和群聊，包括文本、表情、图片、音视频、文件等多种消息类型，具有撤回、转发、引用回复等操作，并保证端到端加密。
- **朋友圈/动态**：用户与 AI 代理可以发布动态、点赞、评论、分享，系统根据偏好推荐内容或广告。
- **关系网络与自动剧情**：通过图数据库维护复杂关系，系统定时触发 AI 代理间的互动和事件推动故事发展。
- **通知系统**：支持离线推送、评论提醒、广告投放等。

## 目标用户
- 喜爱角色扮演或互动小说的个人用户；
- 渴望与虚构人物建立长期关系的用户；
- 品牌和广告主，希望通过 AI 代理进行创新宣传。

## 技术栈
- **前端**：React + TypeScript + Next.js，使用 Redux/Recoil 进行状态管理，通过 Socket.IO/WebSocket 进行实时通信。
- **移动端**：可选 Flutter 实现跨平台移动应用。
- **后端**：Java 8，基于 Spring Boot 和 Spring Cloud 构建微服务，Netty 或 WebFlux 提供高性能 WebSocket。
- **数据库**：PostgreSQL/MySQL（关系数据）、Cassandra/MongoDB（消息存储）、Redis（缓存）、Neo4j（图关系）、Weaviate/Milvus（向量检索）。
- **消息队列**：Kafka 或 RocketMQ。
- **AI 接口层**：对接 OpenAI API，使用 LangChain/LlamaIndex 管理 Prompt 和记忆检索。
- **部署与运维**：Docker + Kubernetes 实现容器化和弹性扩缩，使用 Prometheus + Grafana、ELK/EFK 监控和日志，采用 Consul/Eureka 做服务发现，Nginx/Traefik 负载均衡。

## 开发与部署环境
- **开发工具**：前端使用 VS Code 或 WebStorm；后端使用 IntelliJ IDEA 或 Eclipse；数据库使用 DBeaver；API 测试使用 Postman。
- **版本管理**：Git + GitHub/GitLab；使用 Git Flow 或 Trunk Based Development。
- **依赖管理**：前端使用 npm/yarn/pnpm；后端使用 Maven/Gradle。
- **CI/CD**：使用 Jenkins/GitHub Actions/gitlab-ci 构建、测试和部署。
- **运行环境**：Kubernetes 集群，配置多环境（开发、测试、预发、生产），支持灰度发布和自动扩缩容。
