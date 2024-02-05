# 根据路径查看文件或目录详情

功能描述：根据路径查看文件或目录详情

### 请求地址

```
/repo-repository/api/node/detail/{projectId}/{repoName}/{fullPath}
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

| 字段        | 类型     | 必填  | 描述            |
|-----------|--------|-----|---------------|
| projectId | String | 是   | 项目id          |
| repoName  | String | 是   | 仓库名称          |
| fullPath  | String | 否   | 完整路径（可能为多级路径） |

#### 查询参数
无

#### 请求示例

```http
GET {{devops-domain}}/api/open/CPack/repo-repository/api/node/detail/myproject/generic-local/folder1/file1
```

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
    "nodeInfo": {
      "createdBy": "",
      "createdDate": "2023-07-07T10:59:58.137",
      "lastModifiedBy": "",
      "lastModifiedDate": "2023-07-07T10:59:58.137",
      "recentlyUseDate": null,
      "lastAccessDate": null,
      "folder": true,
      "path": "/",
      "name": "",
      "fullPath": "/",
      "size": 0,
      "sha256": null,
      "md5": null,
      "metadata": {},
      "nodeMetadata": [],
      "projectId": "test-xiaoyushen",
      "repoName": "scan_tool",
      "copyFromCredentialsKey": null,
      "copyIntoCredentialsKey": null
    },
    "createdBy": "",
    "createdDate": "2023-07-07T10:59:58.137",
    "lastModifiedBy": "",
    "lastModifiedDate": "2023-07-07T10:59:58.137",
    "lastAccessDate": null,
    "folder": true,
    "path": "/",
    "name": "",
    "fullPath": "/",
    "size": 0,
    "sha256": null,
    "md5": null,
    "metadata": {},
    "nodeMetadata": [],
    "projectId": "test-xiaoyushen",
    "repoName": "scan_tool"
  },
  "traceId": ""
}
```
#### data 字段说明

节点详细信息

| 字段                     | 说明                                     |
|------------------------|----------------------------------------|
| nodeInfo               | 冗余信息，nodeInfo信息已包含在其余字段中，nodeInfo将来会删除 |
| createdBy              | 创建者                                    |
| createdDate            | 创建时间                                   |
| lastModifiedBy         | 修改者                                    |
| lastModifiedDate       | 修改时间                                   |
| recentlyUseDate        | 最近使用时间时间                               |
| lastAccessDate         | 访问时间                                   |
| folder                 | 是否为文件夹                                 |
| path                   | 路径                                     |
| name                   | 资源名称                                   |
| fullPath               | 完整路径                                   |
| size                   | 文件大小, 单位byte                           |
| sha256                 | 文件sha256                               |
| md5                    | 文件md5                                  |
| metadata               | 元数据                                    |
| nodeMetadata           | 节点元数据(推荐使用)                            |
| projectId              | 所属项目id                                 |
| repoName               | 所属仓库名称                                 |
