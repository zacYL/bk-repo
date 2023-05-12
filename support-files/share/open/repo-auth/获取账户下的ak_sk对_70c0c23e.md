# 获取账户下的ak/sk对
功能描述：获取账户下的ak/sk对

### 请求地址
```
/api/account/credential/list/{appId}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| appId     | string   | true       | appId |




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | array<账户认证信息>   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

