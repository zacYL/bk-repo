# 更新外部权限
功能描述：更新外部权限

### 请求地址
```
/api/ext-permission
```

### 请求方法
`PUT`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| enabled     | boolean   | 是否启用 |
| headers     |    | 请求头 |
| id     | string   | id |
| platformWhiteList     | array<string>   | 平台账号白名单，白名单内不会校验外部权限 |
| projectId     | string   | 项目id |
| repoName     | string   | 仓库名 |
| scope     | string   | 生效微服务 |
| url     | string   | 外部权限回调地址 |

### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

