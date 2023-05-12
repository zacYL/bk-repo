# 获取oauth token信息
功能描述：获取oauth token信息

### 请求地址
```
/api/oauth/token
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| accessToken     | string   | false       | accessToken |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | OauthToken   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### OauthToken
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| access_token     | string   |  |
| scope     | string   |  |
| token_type     | string   |  |

