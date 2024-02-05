# 删除Generic仓库文件元数据
功能描述：删除Generic仓库文件元数据

### 请求地址
```
/repo-repository/api/metadata/{projectId}/{repoName}/{fullPath}
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

| 字段        | 类型     | 必填  | 描述     |
|-----------|--------|-----|--------|
| projectId | String | 是   | 项目id   |
| repoName  | String | 是   | 仓库名称   |
| fullPath  | String | 是   | 节点完整路径 |

#### 请求体参数

| 字段      | 类型   | 必填  | 描述           |
|---------|------|-----|--------------|
| keyList | List | 是   | 待删除的元数据key列表 |

### 返回结果

| 字段      | 说明        |
|---------|-----------|
| code    | 返回码       |
| message | 错误信息      |
| data    | 数据, 为null |
| traceId | 链路追踪id    |

#### 响应体示例

```json
{
  "code": 0,
  "message": null,
  "data": null,
  "traceId": null
}
```
