# 获取repo所有的tag
功能描述：获取repo所有的tag

### 请求地址
```
/ext/tag/{projectId}/{repoName}/**
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| userId     | string   | false       | userId |
| pageNumber     | integer   | true       | pageNumber |
| pageSize     | integer   | true       | pageSize |
| tag     | string   | true       | tag |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | DockerTagResult   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### DockerTagResult
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| records     | array<docker镜像tag信息>   | records |
| totalRecords     | integer , format:int64  | totalRecords |
#### docker镜像tag信息
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| basic     |    | basic |
| history     | array<object>   | history |
| layers     | array<object>   | layers |
| manifest     |    | manifest |
| metadata     | array<MetadataModel>   | metadata |
#### MetadataModel
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| description     | string   |  |
| key     | string   |  |
| system     | boolean   |  |
| value     |    |  |

