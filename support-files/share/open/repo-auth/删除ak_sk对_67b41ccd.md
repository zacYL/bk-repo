# 删除ak/sk对
功能描述：删除ak/sk对

### 请求地址
```
/api/account/credential/{appId}/{accesskey}
```

### 请求方法
`DELETE`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| appId     | string   | true       | appId |
| accesskey     | string   | true       | accesskey |




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | boolean   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

