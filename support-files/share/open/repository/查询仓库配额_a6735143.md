# 查询仓库配额
功能描述：查询仓库配额

### 请求地址
```
/api/repo/quota/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | 所属项目 |
| repoName     | string   | true       | 仓库名称 |




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 仓库配额信息   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 仓库配额信息
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| quota     | integer , format:int64  | 仓库配额 |
| used     | integer , format:int64  | 已使用容量 |

