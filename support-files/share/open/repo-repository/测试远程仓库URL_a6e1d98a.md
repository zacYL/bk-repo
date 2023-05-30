# 测试远程仓库URL
功能描述：测试远程仓库URL

### 请求地址
```
/repo-repository/api/repo/testremote
```

### 请求方法
`POST`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| credentials     | RemoteCredentialsConfigurationReq   |  |
| network     | RemoteNetworkConfigurationReq   |  |
| type     | string   |  |
| url     | string   |  |
#### RemoteCredentialsConfigurationReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| password     | string   |  |
| username     | string   |  |
#### RemoteNetworkConfigurationReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| connectTimeout     | integer , format:int64  |  |
| proxy     | NetworkProxyConfigurationReq   |  |
| readTimeout     | integer , format:int64  |  |
#### NetworkProxyConfigurationReq

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| host     | string   |  |
| password     | string   |  |
| port     | integer , format:int32  |  |
| username     | string   |  |

### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | ConnectionStatusInfo   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### ConnectionStatusInfo
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| message     | string   |  |
| success     | boolean   |  |

