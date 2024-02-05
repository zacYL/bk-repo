# pypi 版本详情接口
功能描述：pypi 版本详情接口

### 请求地址
```
/repo-pypi/ext/version/detail/{projectId}/{repoName}
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
| packageKey | String | 是   | 包唯一key |
| version    | String | 否   | 包版本    |

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
  "code": 0,
  "message": null,
  "data": {
    "basic": {
      "name": "http3test",
      "version": "0.0.4",
      "size": 31077,
      "fullPath": "/http3test/0.0.4/http3test-0.0.4.tar.gz",
      "createdBy": "admin",
      "createdDate": "2023-05-17T11:11:09.598",
      "lastModifiedBy": "anonymous",
      "lastModifiedDate": "2023-05-19T11:17:24.752",
      "downloadCount": 0,
      "sha256": "5f75c9b7aa841fa3f25d1a117e53e995be115b42ca1355e437c8532932c6e0c8",
      "md5": "a93b3facc675e6a2e3240ec60e875452",
      "stageTag": [
        "@prerelease",
        "@release"
        ],
      "description": null
    },
    "metadata": [
      {
        "key": "scanStatus",
        "value": "UN_QUALITY",
        "system": true,
        "description": null
      }
    ]
  },
  "traceId": ""
}
```

#### data 字段说明

basic: 基础信息

| 字段               | 说明           |
|------------------|--------------|
| version          | 版本字段         |
| fullPath         | 完整路径         |
| size             | 文件大小, 单位byte |
| sha256           | 文件sha256     |
| md5              | 文件md5        |
| stageTag         | 晋级状态标签       |
| projectId        | 所属项目id       |
| repoName         | 所属仓库名称       |
| downloadCount    | 下载次数         |
| createdBy        | 创建者          |
| createdDate      | 创建时间         |
| lastModifiedBy   | 修改者          |
| lastModifiedDate | 修改时间         |

metadata: 元数据信息

| 字段          | 描述       |
|-------------|----------|
| key         | 元数据键     |
| value       | 元数据值     |
| system      | 是否为系统元数据 |
| description | 元数据描述信息  |

