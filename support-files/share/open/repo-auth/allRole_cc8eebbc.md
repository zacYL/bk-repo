# allRole
功能描述：allRole

### 请求地址
```
/api/sys/role/list
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| userId     | string   | false       | userId |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | array<角色>   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

