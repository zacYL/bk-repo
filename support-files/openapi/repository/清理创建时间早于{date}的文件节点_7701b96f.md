# 清理创建时间早于{date}的文件节点
功能描述：清理创建时间早于{date}的文件节点

### 请求地址
```
/api/node/clean/{projectId}/{repoName}/**
```

### 请求方法
`DELETE`
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
| date     | string   | true       | date |


#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |

### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 节点删除结果   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 节点删除结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| deletedNumber     | integer , format:int64  | 删除节点数量 |
| deletedSize     | integer , format:int64  | 删除节点大小 |

