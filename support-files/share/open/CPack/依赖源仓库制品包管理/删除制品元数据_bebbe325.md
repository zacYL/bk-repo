# 删除制品元数据
功能描述：删除依赖源仓库包版本元数据



## 请求

#### 接口方法

`DELETE`

#### 接口地址

`/api/open/CPack/repo-repository/api/metadata/package/{projectId}/{repoName}`

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

无

#### 请求体

| 字段         | 类型     | 必填  | 描述           |
|------------|--------|-----|--------------|
| packageKey | String | 是   | 包唯一key       |
| version    | String | 是   | 包版本          |
| keyList    | List   | 是   | 待删除的元数据key列表 |

#### 请求示例

```bash
curl -X 'DELETE' -H 'Content-Type: application/json' -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' 'https://devops.example.com/api/open/CPack/repo-repository/api/metadata/package/{projectId}/{repoName}' -d '{
    "packageKey": "docker://zookeeper",
    "version": "latest",
    "keyList": [
        "refreshed"
    ]
}'
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
