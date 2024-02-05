# 获取指定image的所有tag
功能描述：获取指定image的所有tag

### 请求地址
```
/repo-docker/ext/tag/{projectId}/{repoName}/{imageName}
```

### 请求方法
`GET`
### 请求参数

#### 请求头参数

| 字段                  | 类型   | 必填 | 描述                  |
| --------------------- | ------ | ---- | --------------------- |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | OpenAPI认证token      |
| Authorization         | String | 是   | 制品库basic认证请求头 |

#### 路径参数

| 字段        | 类型     | 必填  | 描述         |
|-----------|--------|-----|------------|
| projectId | String | 是   | 项目id       |
| repoName  | String | 是   | 仓库名称       |
| imageName | String | 是   | 镜像名称(可能为多级路径) |

#### 查询参数

| 字段         | 类型     | 必填  | 描述    |
|------------|--------|-----|-------|
| pageNumber | Int    | 是   | 页码数   |
| pageSize   | Int    | 是   | 每页大小  |
| tag        | String | 是   | tag名称 |

### 返回结果

| 字段      | 说明     |
|---------|--------|
| code    | 返回码    |
| message | 错误信息   |
| data    | 数据     |
| traceId | 链路追踪id |

#### 响应体示例

```json
{
  "code":0,
  "message":null,
  "data":{
    "toatalRecords":100,
    "records":[
      {
        "tag":"latest",
        "stageTag":"",
        "size":524,
        "lastModifiedBy":"admin",
        "lastModifiedDate":"2020-09-10T14:48:22.846",
        "downloadCount":0,
        "registryUrl":"bkrepo.example.com/test/test/php:latest"
      },
      {
        "tag":"v1",
        "stageTag":"",
        "size":524,
        "lastModifiedBy":"admin",
        "lastModifiedDate":"2020-09-10T14:49:37.904",
        "downloadCount":0,
        "registryUrl":"bkrepo.example.com/test/test/php:v1"
      }
    ]
  },
  "traceId":""
}
```

#### data 字段说明

镜像tag信息

| 字段           | 描述    |
|--------------|-------|
| records      | tag列表 |
| totalRecords | tag数量 |

#### records 字段说明

| 字段               | 描述    |
|------------------|-------|
| tag              | tag名称 |
| stageTag         | 制品状态  |
| size             | 镜像大小  |
| lastModifiedBy   | 更新人   |
| lastModifiedDate | 更新时间  |
| downloadCount    | 下载次数  |
| registryUrl      | 镜像地址  |
