# 查询NuGet包版本详情
功能描述：查询NuGet包版本详情



## 请求

#### 接口方法

`GET`

#### 接口地址

`/api/open/CPack/repo-nuget/ext/version/detail/{projectId}/{repoName}`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

| 字段      | 类型   | 必填 | 描述     |
| --------- | ------ | ---- | -------- |
| projectId | String | 是   | 项目ID   |
| repoName  | String | 是   | 仓库名称 |

#### 查询参数

| 字段       | 类型   | 必填 | 描述      |
| ---------- | ------ | ---- | --------- |
| packageKey | String | 是   | 包唯一key |
| version    | String | 是   | 包版本    |

#### 请求体

无

#### 请求示例

```bash
curl -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    'https://devops.example.com/api/open/CPack/repo-nuget/ext/version/detail/{projectId}/{repoName}?packageKey=nuget://passsharp&version=1.0.0'
```



## 响应

#### 响应示例

```json
{
  "code": 0,
  "message": null,
  "data": {
    "basic": {
      "version": "1.0.0",
      "fullPath": "/mynuget/mynuget.1.0.0.nupkg",
      "size": 1880,
      "sha256": "0a697b92e4909ded5b4d4b5472199786d7bbfcebd95145505fc6032c9c067fd3",
      "md5": "9b2225b12d15a86c752c368f45b26cc7",
      "stageTag": [
        "@prerelease",
        "@release"
      ],
      "projectId": "bb8d9b",
      "repoName": "nuget",
      "downloadCount": 1,
      "createdBy": "admin",
      "createdDate": "2023-05-19T11:16:24.944",
      "lastModifiedBy": "admin",
      "lastModifiedDate": "2023-05-19T11:16:50.215"
    },
    "metadata": [
      {
        "key": "id",
        "value": "mynuget",
        "system": false,
        "description": null
      },
      {
        "key": "dependency",
        "value": [
          {
            "@type": "PackageDependencyGroup",
            "dependencies": [
              {
                "@type": "PackageDependency",
                "id": "SampleDependency",
                "range": "[1.0.0, )"
              }
            ],
            "targetFramework": ".NETStandard2.1"
          }
        ],
        "system": false,
        "description": null
      }
    ]
  },
  "traceId": ""
}
```

#### 响应体

| 字段      | 说明     |
|---------|--------|
| code    | 返回码    |
| message | 错误信息   |
| data    | 数据     |
| traceId | 链路追踪id |

##### data 字段说明

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
