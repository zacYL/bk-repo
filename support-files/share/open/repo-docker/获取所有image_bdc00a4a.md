# 获取所有image
功能描述：获取所有image

### 请求地址
```
/repo-docker/ext/repo/{projectId}/{repoName}
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
| name     | string   | true       | name |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | docker镜像信息查询结果信息   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### docker镜像信息查询结果信息
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| records     | array<docker镜像信息>   | records |
| totalRecords     | integer , format:int32  | totalRecords |
#### docker镜像信息
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| description     | string   | 镜像描述 |
| downloadCount     | integer , format:int64  | 下载次数 |
| lastModifiedBy     | string   | 最后修改人 |
| lastModifiedDate     | string   | 最后修改时间 |
| logoUrl     | string   | 镜像logo地址 |
| name     | string   | name |

