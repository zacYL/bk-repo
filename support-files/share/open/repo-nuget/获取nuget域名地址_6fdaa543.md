# 获取nuget域名地址
功能描述：获取nuget域名地址

### 请求地址
```
/ext/address
```

### 请求方法
`GET`




### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | NugetDomainInfo   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### NugetDomainInfo
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| domain     | string   |  |

