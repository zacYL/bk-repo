# getWhitelist
功能描述：getWhitelist

### 请求地址
```
/api/remote/whitelist/{id}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| id     | string   | true       | id |




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | RemotePackageWhitelist   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

