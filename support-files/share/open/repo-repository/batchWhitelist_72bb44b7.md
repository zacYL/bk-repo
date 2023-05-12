# batchWhitelist
功能描述：batchWhitelist

### 请求地址
```
/api/remote/whitelist/batch
```

### 请求方法
`PUT`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| CreateRemotePackageWhitelistRequest     | array<CreateRemotePackageWhitelistRequest>   |  |

### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | integer , format:int32  | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

