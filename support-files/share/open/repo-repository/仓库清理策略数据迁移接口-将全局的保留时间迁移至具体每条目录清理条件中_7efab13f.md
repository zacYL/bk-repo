# 仓库清理策略数据迁移接口：将全局的保留时间迁移至具体每条目录清理条件中

功能描述：仓库清理策略数据迁移接口：将全局的保留时间迁移至具体每条目录清理条件中

### 请求地址

```
/repo-repository/api/repo/migrate/cleanStrategy
```

### 请求方法

`GET`

### 请求参数

无

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
  "data": [
    "test/generic-test:GENERIC",
    "public-global/trivy:GENERIC",
    "bkrepo2/generic-repo:GENERIC"
  ],
  "traceId": ""
}
```

#### data 字段说明

完成迁移的仓库列表
