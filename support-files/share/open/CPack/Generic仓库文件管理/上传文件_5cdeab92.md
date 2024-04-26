# 上传文件
功能描述：上传文件



## 请求

#### 接口方法

`PUT`

#### 接口地址

`/api/open/CPack/repo-generic/{projectId}/{repoName}/{fullPath}`

#### 请求头

| 字段                  | 类型   | 必填 | 描述                                                         |
| --------------------- | ------ | ---- | ------------------------------------------------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌                                           |
| X-BKREPO-OVERWRITE    | String | 否   | 值为true时，将覆盖已有文件。默认值：false                    |
| X-BKREPO-MD5          | String | 否   | 校验接收到的文件是否为该MD5摘要                              |
| X-BKREPO-SHA256       | String | 否   | 校验接收到的文件是否为该SHA256摘要                           |
| X-BKREPO-META-{key}   | String | 否   | 附带key-value形式的文件元数据。将请求头名称的{key} 替换为元数据key，请求头的值填入元数据value。可同时传入多个元数据。 |

#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | String  | 是   | 项目ID |
| repoName     | String | 是  | 仓库名 |
| fullPath | String | 是  | 文件上传到仓库后的全路径，包含新的文件名，可以和源文件名不同。<br/>如果包含中文，使用curl命令时需要将中文转换为URL编码后的字符。<br/>所在目录不存在时将会自动递归创建。 |

#### 查询参数

无

#### 请求体

文件

(使用Postman时，请求体选择`binary`；使用`curl`命令时，使用`-T`参数指定文件)

#### 请求示例

```bash
curl -X 'PUT' \
    -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    -T <本地文件>
    'https://devops.example.com/api/open/CPack/repo-repogeneric/{projectId}/{repoName}/{fullPath}'
```



## 响应

#### 响应示例

```json
{
    "code": 0,
    "message": null,
    "data": {
        "nodeInfo": {
            "createdBy": "admin",
            "createdDate": "2023-07-31T15:28:25.931",
            "lastModifiedBy": "admin",
            "lastModifiedDate": "2023-07-31T15:28:25.931",
            "recentlyUseDate": null,
            "lastAccessDate": "2023-07-31T15:28:25.931",
            "folder": false,
            "path": "/",
            "name": "test.txt",
            "fullPath": "/test.txt",
            "size": 320506,
            "sha256": "3799fa815351fea3a5e96ac7e503a96fa51cc9942c3753cda7651b93c1cfa362",
            "md5": "6142519f8510591650e31a47b9c50244",
            "metadata": {},
            "nodeMetadata": [],
            "projectId": "test",
            "repoName": "generic-local",
            "copyFromCredentialsKey": null,
            "copyIntoCredentialsKey": null
        },
        "createdBy": "admin",
        "createdDate": "2023-07-31T15:28:25.931",
        "lastModifiedBy": "admin",
        "lastModifiedDate": "2023-07-31T15:28:25.931",
        "lastAccessDate": "2023-07-31T15:28:25.931",
        "folder": false,
        "path": "/",
        "name": "test.txt",
        "fullPath": "/test.txt",
        "size": 320506,
        "sha256": "3799fa815351fea3a5e96ac7e503a96fa51cc9942c3753cda7651b93c1cfa362",
        "md5": "6142519f8510591650e31a47b9c50244",
        "metadata": {},
        "nodeMetadata": [],
        "projectId": "test",
        "repoName": "generic-local"
    },
    "traceId": ""
}
```

#### 响应体

| 字段    | 说明               |
| ------- | ------------------ |
| code    | 返回码             |
| message | 错误信息           |
| data    | 上传完成的节点数据 |
| traceId | 链路追踪id         |

##### data 字段说明

上传完成的文件详情

| 字段             | 说明                                                       |
| ---------------- | ---------------------------------------------------------- |
| nodeInfo         | （已废弃，后期可能删除该字段）兼容早期版本的节点信息       |
| createdBy        | 创建人                                                     |
| createdDate      | 创建时间                                                   |
| lastModifiedBy   | 最后修改人                                                 |
| lastModifiedDate | 最后修改时间                                               |
| lastAccessDate   | 最后访问时间                                               |
| folder           | 是否为目录                                                 |
| path             | 所在目录的路径                                             |
| name             | 文件/目录节点名称                                          |
| fullPath         | 节点全路径                                                 |
| size             | 节点大小（字节）                                           |
| sha256           | 节点sha256值                                               |
| md5              | 节点md5值                                                  |
| metadata         | （已废弃，后期可能删除该字段）兼容早期版本的节点元数据信息 |
| nodeMetadata     | 节点元数据                                                 |
| projectId        | 项目ID                                                     |
| repoName         | 仓库名                                                     |
