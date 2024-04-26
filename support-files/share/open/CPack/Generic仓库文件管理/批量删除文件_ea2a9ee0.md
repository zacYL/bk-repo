# 批量删除文件

功能描述：批量删除文件（支持目录）



## 请求

#### 接口方法

`DELETE`

#### 接口地址

`/api/open/CPack/repo-repository/api/node/batch/{projectId}/{repoName}`

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

| 字段      | 类型 | 必填 | 描述                                     |
| --------- | ---- | ---- | ---------------------------------------- |
| fullPaths | List | 是   | 文件(或目录)的完整路径, 个数在0和200之间 |

#### 请求示例

```bash
curl -X 'DELETE' -H 'Content-Type: application/json' -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' 'https://devops.example.com/api/open/CPack/repo-repository/api/node/batch/{projectId}/{repoName}' -d '[
    "/a/b/c/file8",
    "/a/b/c/file2",
    "/a/b/c/file3"
]'
```



## 响应

#### 响应示例

```json
{
  "code" : 0,
  "message" : null,
  "data" : null,
  "traceId" : ""
}
```

#### 响应体

| 字段    | 说明       |
| ------- | ---------- |
| code    | 返回码     |
| message | 错误信息   |
| data    | 固定为null |
| traceId | 链路追踪id |

