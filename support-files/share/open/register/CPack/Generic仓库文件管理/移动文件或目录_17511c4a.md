# 移动文件或目录

功能描述：移动文件或目录



## 请求

#### 接口方法

`POST`

#### 接口地址

`/api/open/CPack/repo-repository/api/node/move`

#### 请求头

| 字段                  | 类型   | 必填 | 描述               |
| --------------------- | ------ | ---- | ------------------ |
| X-DEVOPS-ACCESS-TOKEN | String | 是   | Devops用户访问令牌 |

#### 路径参数

无

#### 查询参数

无

#### 请求体

| 字段          | 类型    | 必填 | 描述                               |
| ------------- | ------- | ---- | ---------------------------------- |
| srcProjectId  | String  | 是   | 源项目ID                           |
| srcRepoName   | String  | 是   | 源仓库名称                         |
| srcFullPath   | String  | 是   | 源节点路径                         |
| destProjectId | String  | 否   | 目的项目ID（默认值为源项目ID）     |
| destRepoName  | String  | 否   | 目的仓库名称（默认值为源仓库名称） |
| destFullPath  | String  | 是   | 目的路径                           |
| overwrite     | Boolean | 否   | 同名文件是否覆盖（默认值：false）  |

#### 请求示例

```bash
curl -X 'POST' \
    -H 'Content-Type: application/json' \
    -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    --data '{
    "srcProjectId": "myproject",
    "srcRepoName": "generic1",
    "srcFullPath": "/folder/file",
    "destProjectId": "myproject",
    "destRepoName": "generic2",
    "destFullPath": "/folder2/file",
    "overwrite": false
}' \
    'https://devops.example.com/api/open/CPack/repo-repository/api/node/move'
```



## 响应

#### 响应示例

```json
{
  "code": 0,
  "message": null,
  "data": null,
  "traceId": null
}
```

#### 响应体

| 字段      | 说明        |
|---------|-----------|
| code    | 返回码       |
| message | 错误信息      |
| data    | 数据, 为null |
| traceId | 链路追踪id    |
