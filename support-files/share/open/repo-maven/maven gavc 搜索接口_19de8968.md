# maven gavc 搜索接口
功能描述：maven gavc 搜索接口

### 请求地址
```
/repo-maven/ext/search/gavc/{projectId}/{pageNumber}/{pageSize}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| pageNumber     | integer   | true       | pageNumber |
| pageSize     | integer   | true       | pageSize |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| g     | string   | true       | g |
| a     | string   | true       | a |
| v     | string   | true       | v |
| c     | string   | true       | c |
| repos     | string   | true       | repos |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 分页数据包装模型«UriResult»   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 分页数据包装模型«UriResult»
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| count     | integer , format:int64  |  |
| page     | integer , format:int32  |  |
| pageNumber     | integer , format:int32  | 页码(从1页开始) |
| pageSize     | integer , format:int32  | 每页多少条 |
| records     | array<UriResult>   | 数据列表 |
| totalPages     | integer , format:int64  | 总页数 |
| totalRecords     | integer , format:int64  | 总记录条数 |
#### UriResult
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| uri     | string   |  |

