# 查询包的依赖项
功能描述：查询包的依赖项

### 请求地址
```
/ext/dependencies/{projectId}/{repoName}
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
| packageKey     | string   | true       | packageKey |
| version     | string   | true       | version |
| pageNumber     | integer   | true       | pageNumber |
| pageSize     | integer   | true       | pageSize |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 分页数据包装模型«MavenDependency»   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 分页数据包装模型«MavenDependency»
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| count     | integer , format:int64  |  |
| page     | integer , format:int32  |  |
| pageNumber     | integer , format:int32  | 页码(从1页开始) |
| pageSize     | integer , format:int32  | 每页多少条 |
| records     | array<MavenDependency>   | 数据列表 |
| totalPages     | integer , format:int64  | 总页数 |
| totalRecords     | integer , format:int64  | 总记录条数 |
#### MavenDependency
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| artifactId     | string   |  |
| classifier     | string   |  |
| groupId     | string   |  |
| optional     | boolean   |  |
| scope     | string   |  |
| type     | string   |  |
| version     | string   |  |

