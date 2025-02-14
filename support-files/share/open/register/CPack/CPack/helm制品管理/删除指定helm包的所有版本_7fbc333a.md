# 删除指定helm包的所有版本
功能描述：删除指定helm包的所有版本



## 请求

#### 接口方法

`DELETE`

#### 接口地址

`/api/open/CPack/repo-helm/ext/package/delete/{projectId}/{repoName}`

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

| 字段          | 类型     | 必填  | 描述     |
|-------------|--------|-----|--------|
| packageKey  | String | 是   | 包唯一key |

#### 请求示例

```bash
curl -X 'DELETE' \
    -H 'X-DEVOPS-ACCESS-TOKEN: <your_access_token>' \
    'https://devops.example.com/api/open/CPack/repo-helm/ext/package/delete/{projectId}/{repoName}?packageKey=helm://consul'
```



## 响应

#### 响应示例

```json
{
  "code":0,
  "message":null,
  "data":null,
  "traceId":""
}
```

#### 响应体

| 字段      | 说明     |
|---------|--------|
| code    | 返回码    |
| message | 错误信息   |
| data    | 数据     |
| traceId | 链路追踪id |
