# 创建/更新元数据列表
功能描述：创建/更新元数据列表

### 请求地址
```
/repo-repository/api/metadata/{projectId}/{repoName}/**
```

### 请求方法
`POST`
### 请求参数

#### 路径参数

| 字段        | 类型     | 必填  | 描述     |
|-----------|--------|-----|--------|
| projectId | String | 是   | 项目id   |
| repoName  | String | 是   | 仓库名称   |
| fullPath  | String | 是   | 节点完整路径 |

### 请求参数

| 字段           | 类型   | 必填  | 描述                     |
|--------------|------|-----|------------------------|
| metadata     | Map  | 否   | 已废弃, 由`nodeMetadata`代替 |
| nodeMetadata | List | 否   | 元数据                    |

> 若携带`nodeMetadata`, 参考以下说明

| 字段          | 类型      | 必填  | 描述       |
|-------------|---------|-----|----------|
| key         | String  | 是   | 元数据键     |
| value       | Any     | 是   | 元数据值     |
| system      | Boolean | 否   | 是否为系统元数据 |
| description | String  | 否   | 元数据描述信息  |

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
