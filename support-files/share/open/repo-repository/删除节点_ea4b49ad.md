# 删除节点

功能描述：删除节点

### 请求地址

```
/repo-repository/api/node/delete/{projectId}/{repoName}/{fullPath}
```

### 请求方法

`DELETE`

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
| fullPath  | String | 否   | 完整路径, 可能为多层路径 |

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
  "code" : 0,
  "message" : null,
  "data" : {
    "deletedNumber" : 1,
    "deletedSize" : 169
  },
  "traceId" : ""
}
```

#### data 字段说明

节点删除结果

| 字段            | 说明     |
|---------------|--------|
| deletedNumber | 删除节点数量 |
| deletedSize   | 删除节点大小 |
