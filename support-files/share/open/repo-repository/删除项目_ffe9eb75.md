# 删除项目

功能描述：删除项目

### 请求地址

```
/repo-repository/api/project/delete/{name}
```

### 请求方法

`DELETE`

### 请求参数

#### 路径参数

| 字段   | 类型     | 必填  | 描述   |
|------|--------|-----|------|
| name | String | 是   | 项目ID |

#### 查询参数

| 字段          | 类型     | 必填  | 描述              |
|-------------|--------|-----|-----------------|
| confirmName | String | 是   | 用户输入的确认信息(项目id) |

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
