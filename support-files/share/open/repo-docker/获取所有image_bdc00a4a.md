# 获取所有image
功能描述：获取所有image

### 请求地址
```
/repo-docker/ext/repo/{projectId}/{repoName}
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

#### 查询参数

| 字段         | 类型     | 必填  | 描述     |
|------------|--------|-----|--------|
| pageNumber | Int    | 是   | 页码数    |
| pageSize   | Int    | 是   | 每页大小   |
| name       | String | 是   | 镜像名称   |

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
        "name":"hello-world",
        "lastModifiedBy":"admin",
        "lastModifiedDate":"2020-09-10T14:48:22.846",
        "downloadCount":0,
        "logoUrl":"",
        "description":""
      },
      {
        "name":"mongo",
        "lastModifiedBy":"admin",
        "lastModifiedDate":"2020-08-28T12:07:12.672",
        "downloadCount":0,
        "logoUrl":"",
        "description":""
      }
    ]
  },
  "traceId":""
}
```

#### data 字段说明

docker镜像信息查询结果信息

| 字段           | 描述     |
|--------------|--------|
| records      | 镜像列表   |
| totalRecords | 镜像数量   |

#### records 字段说明

| 字段               | 描述       |
|------------------|----------|
| name             | 镜像名称     |
| lastModifiedBy   | 最后修改人    |
| lastModifiedDate | 最后修改时间   |
| downloadCount    | 下载次数     |
| logoUrl          | 镜像logo地址 |
| description      | 镜像描述     |
