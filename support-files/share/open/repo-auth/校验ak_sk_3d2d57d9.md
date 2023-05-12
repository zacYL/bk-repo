# 校验ak/sk
功能描述：校验ak/sk

### 请求地址
```
/api/account/credential/{accesskey}/{secretkey}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| accesskey     | string   | true       | accesskey |
| secretkey     | string   | true       | secretkey |




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | string   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

