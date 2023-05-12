# page
功能描述：page

### 请求地址
```
/api/remote/whitelist/page
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| type     | string   | true       | type;枚举：[COMPOSER, DOCKER, GENERIC, GIT, HELM, MAVEN, NONE, NPM, NUGET, OCI, PYPI, RDS, RPM] |
| packageKey     | string   | true       | packageKey |
| version     | string   | true       | version |
| pageNumber     | integer   | true       | 当前页 |
| pageSize     | integer   | true       | 分页大小 |
| regex     | boolean   | true       | 是否正则匹配 |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 分页数据包装模型«RemotePackageWhitelist»   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 分页数据包装模型«RemotePackageWhitelist»
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| count     | integer , format:int64  |  |
| page     | integer , format:int32  |  |
| pageNumber     | integer , format:int32  | 页码(从1页开始) |
| pageSize     | integer , format:int32  | 每页多少条 |
| records     | array<RemotePackageWhitelist>   | 数据列表 |
| totalPages     | integer , format:int64  | 总页数 |
| totalRecords     | integer , format:int64  | 总记录条数 |
#### RemotePackageWhitelist
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| createdBy     | string   |  |
| createdDate     | string , format:date-time  |  |
| id     | string   |  |
| lastModifiedBy     | string   |  |
| lastModifiedDate     | string , format:date-time  |  |
| packageKey     | string   |  |
| type     | string   |  |
| versions     | array<string>   |  |

