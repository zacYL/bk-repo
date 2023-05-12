# 恢复被删除节点
功能描述：恢复被删除节点

### 请求地址
```
/api/node/restore/{projectId}/{repoName}/**
```

### 请求方法
`POST`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| artifactFullPath     | string   | false       |  |
| artifactName     | string   | false       |  |
| artifactVersion     | string   | false       |  |
| conflictStrategy     | string   | false       | 冲突处理策略;枚举：[FAILED, OVERWRITE, SKIP] |
| deletedId     | integer   | false       | 删除时间 |
| projectId     | string   | false       |  |
| repoIdentify     | string   | false       |  |
| repoName     | string   | false       |  |
| responseName     | string   | false       |  |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | NodeRestoreResult   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### NodeRestoreResult
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| conflictCount     | integer , format:int64  | 发生冲突并覆盖的节点数量 |
| fullPath     | string   |  |
| restoreCount     | integer , format:int64  | 实际恢复的节点数量，包含了conflictCount |
| skipCount     | integer , format:int64  | 发生冲突并跳过的节点数量 |

