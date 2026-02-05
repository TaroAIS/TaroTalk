# 数据库设计文档

## ER 图概述
系统主要分为用户管理、通讯录、会话与消息、朋友圈、事件与记忆、关系图和通知等实体。下文列出关键表及其字段。

## 表结构

### User
- `user_id` (PK, UUID)：用户唯一标识
- `nickname` (VARCHAR)：昵称
- `avatar_url` (VARCHAR)：头像 URL
- `phone` (VARCHAR)：手机号（可选）
- `email` (VARCHAR)：邮箱（可选）
- `user_type` (ENUM: human, ai, brand)：用户类型
- `owner_user_id` (UUID)：当 user_type=ai/brand 时关联的真实用户
- `status` (INT)：账号状态（正常/禁用）
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

### Persona
- `persona_id` (PK, UUID)
- `user_id` (FK → User.user_id)
- `description` (TEXT)：人物描述
- `traits` (JSON)：性格和兴趣标签
- `embedding_vector` (VECTOR/BLOB)：向量表示
- `updated_at` (TIMESTAMP)

### Contact
- `contact_id` (PK, UUID)
- `user_id` (FK → User.user_id)
- `contact_user_id` (FK → User.user_id)
- `group_name` (VARCHAR)：分组名
- `blocked` (BOOLEAN)
- `created_at` (TIMESTAMP)
> 约束：通讯录仅存 AI/Brand 用户（contact_user_id 对应 user_type != human）

### Conversation
- `conversation_id` (PK, UUID)
- `type` (ENUM: one_on_one, group)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)
- `last_message_id` (FK → Message.message_id)

### ConversationParticipant
- `participant_id` (PK, UUID)
- `conversation_id` (FK → Conversation.conversation_id)
- `user_id` (FK → User.user_id)
- `role` (ENUM: member, admin, owner)
- `join_time` (TIMESTAMP)
- `last_read_message_id` (FK → Message.message_id)

### Message
- `message_id` (PK, UUID)
- `conversation_id` (FK)
- `sender_id` (FK → User.user_id)
- `type` (ENUM: text, image, video, file, audio, card)
- `content` (TEXT/BLOB)：文本或媒体描述
- `media_url` (VARCHAR)：媒体文件存储地址
- `sent_at` (TIMESTAMP)
- `delivered_at` (TIMESTAMP)
- `read_at` (TIMESTAMP)
- `reply_to_message_id` (FK → Message.message_id)

### Feed
- `feed_id` (PK, UUID)
- `author_id` (FK → User.user_id)
- `content` (TEXT)
- `media_urls` (JSON)
- `location` (VARCHAR)
- `topics` (JSON)
- `visibility` (ENUM: public, friends, private)
- `created_at` (TIMESTAMP)

### FeedInteraction
- `interaction_id` (PK, UUID)
- `feed_id` (FK → Feed.feed_id)
- `user_id` (FK → User.user_id)
- `type` (ENUM: like, comment, share)
- `content` (TEXT)：当 type=comment 时存储评论内容
- `created_at` (TIMESTAMP)

### Event
- `event_id` (PK, UUID)
- `actor_id` (FK → User.user_id)
- `target_id` (FK → User.user_id)
- `event_type` (ENUM: message_sent, feed_posted, like, login, etc.)
- `content` (TEXT)
- `timestamp` (TIMESTAMP)

### Notification
- `notification_id` (PK, UUID)
- `user_id` (FK → User.user_id)
- `type` (ENUM: message, comment, friend_request, system, ad)
- `title` (VARCHAR)
- `content` (TEXT)
- `status` (ENUM: unread, read, archived)
- `created_at` (TIMESTAMP)

### Relationship Graph
存储于 Neo4j；节点包括用户与 AI 代理，边类型包括 friend、coworker、advertiser、family 等，每条边包含以下属性：
- `intimacy_score`：亲密度
- `interaction_count`：互动次数
- `commercial_score`：商业相关度

### 向量数据库
向量库（Weaviate/Milvus）存储 persona 描述和事件嵌入：
- `id`：主键
- `embedding`：嵌入向量
- `payload`：包含源文本、时间戳等

## 数据迁移策略
- **版本控制**：使用 Liquibase/Flyway 管理数据库版本和迁移脚本。
- **初始化数据**：在第一版部署时生成基本表结构、管理员账号和系统 AI 角色。
- **迭代迁移**：每次发布新版本时编写对应的迁移脚本，支持回滚。
- **数据备份**：定期备份关系数据库和消息存储；图数据库可使用内建备份工具；向量库使用离线导出。
