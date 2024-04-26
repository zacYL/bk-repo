# 查询指定包的所有版本
功能描述：查询指定包的所有版本



## 请求

#### 接口方法

`GET`

#### 接口地址

`/api/open/CPack/repo-repository/api/version/list/{projectId}/{repoName}`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | 是      | projectId |
| repoName     | string   | 是      | repoName |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| pageNumber     | integer   | 否      | 当前页 |
| pageSize     | integer   | 否      | 分页大小 |
| stageTag     | string   | 否      | 晋级tag, 多个tag以逗号分隔 |
| version     | string   | 否       | 版本 |
| packageKey     | string   | 是      | packageKey |

#### 请求体

无

#### 请求示例

```bash
curl -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' 'https://devops.example.com/api/open/CPack/repo-repository/api/version/list/{projectId}/{repoName}?pageNumber=1&pageSize=20&packageKey=
gav://org.junit:junit-bom'
```



## 响应

#### 响应示例

```json
{
    "code": 0,
    "message": null,
    "data": [
        {
            "createdBy": "admin",
            "createdDate": "2023-12-25T11:20:00.119",
            "lastModifiedBy": "admin",
            "lastModifiedDate": "2023-12-25T11:20:00.222",
            "recentlyUseDate": "2023-12-25T11:20:00.262",
            "name": "5.9.1",
            "size": 5630,
            "downloads": 2,
            "stageTag": [],
            "metadata": {
                "scanStatus": "QUALITY_PASS",
                "artifactId": "junit-bom",
                "packaging": "pom",
                "version": "5.9.1",
                "qualityRedLine": true,
                "groupId": "org.junit"
            },
            "packageMetadata": [
                {
                    "key": "scanStatus",
                    "value": "QUALITY_PASS",
                    "system": true,
                    "description": null,
                    "display": false
                },
                {
                    "key": "artifactId",
                    "value": "junit-bom",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "packaging",
                    "value": "pom",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "version",
                    "value": "5.9.1",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "qualityRedLine",
                    "value": true,
                    "system": true,
                    "description": null,
                    "display": false
                },
                {
                    "key": "groupId",
                    "value": "org.junit",
                    "system": true,
                    "description": null,
                    "display": true
                }
            ],
            "tags": [],
            "extension": {},
            "contentPath": "/org/junit/junit-bom/5.9.1/junit-bom-5.9.1.pom",
            "manifestPath": null,
            "ordinal": 5000900019999
        },
        {
            "createdBy": "admin",
            "createdDate": "2023-12-25T10:23:39.863",
            "lastModifiedBy": "admin",
            "lastModifiedDate": "2023-12-25T10:23:39.863",
            "recentlyUseDate": "2023-12-25T10:25:32.312",
            "name": "5.9.2",
            "size": 5630,
            "downloads": 2,
            "stageTag": [],
            "metadata": {
                "scanStatus": "QUALITY_PASS",
                "artifactId": "junit-bom",
                "packaging": "pom",
                "version": "5.9.2",
                "qualityRedLine": true,
                "groupId": "org.junit"
            },
            "packageMetadata": [
                {
                    "key": "scanStatus",
                    "value": "QUALITY_PASS",
                    "system": true,
                    "description": null,
                    "display": false
                },
                {
                    "key": "artifactId",
                    "value": "junit-bom",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "packaging",
                    "value": "pom",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "version",
                    "value": "5.9.2",
                    "system": true,
                    "description": null,
                    "display": true
                },
                {
                    "key": "qualityRedLine",
                    "value": true,
                    "system": true,
                    "description": null,
                    "display": false
                },
                {
                    "key": "groupId",
                    "value": "org.junit",
                    "system": true,
                    "description": null,
                    "display": true
                }
            ],
            "tags": [],
            "extension": {},
            "contentPath": "/org/junit/junit-bom/5.9.2/junit-bom-5.9.2.pom",
            "manifestPath": null,
            "ordinal": 5000900029999
        }
    ],
    "traceId": "c8e62e04b13c94309f2cbae30afc6a98"
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


| 字段             | 说明                                                  |
| ---------------- | ----------------------------------------------------- |
| createdBy        | 创建者                                                |
| createdDate      | 创建时间                                              |
| lastModifiedBy   | 修改者                                                |
| lastModifiedDate | 修改时间                                              |
| recentlyUseDate  | 最近使用时间时间                                      |
| lastAccessDate   | 访问时间                                              |
| name             | 版本名称                                              |
| size             | 大小                                                  |
| downloads        | 下载量                                                |
| stageTag         | 晋级状态                                              |
| metadata         | 元数据（已废弃，后续可能移除，请使用packageMetadata） |
| packageMetadata  | 元数据                                                |
| tags             | 标签                                                  |
| extension        | 扩展字段                                              |
| contentPath      | 主要文件路径                                          |
| manifestPath     | manifest文件路径                                      |
| ordinal          | 语义化版本序号                                        |
