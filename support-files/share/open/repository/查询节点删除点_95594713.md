# 查询节点删除点
功能描述：查询节点删除点

### 请求地址
```
/api/node/list-deleted/{projectId}/{repoName}/**
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| artifactFullPath     | string   | false       |  |
| artifactName     | string   | false       |  |
| artifactVersion     | string   | false       |  |
| projectId     | string   | false       |  |
| repoIdentify     | string   | false       |  |
| repoName     | string   | false       |  |
| responseName     | string   | false       |  |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | array<节点删除点>   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 节点删除点
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| deletedBy     | string   | 删除用户 |
| deletedTime     | string , format:date-time  | 删除时间 |
| fullPath     | string   | 完整路径 |
| id     | integer , format:int64  | 记录id |
| metadata     |    | 元数据 |
| sha256     | string   | 文件sha256 |
| size     | integer , format:int64  | 文件大小，单位byte |

