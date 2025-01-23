# 创建分享链接
功能描述：创建分享链接

### 请求地址
```
/api/share/{projectId}/{repoName}/**
```

### 请求方法
`POST`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| artifactFullPath     | string   | false       |  |
| artifactName     | string   | false       |  |
| artifactVersion     | string   | false       |  |
| projectId     | string   | false       |  |
| repoIdentify     | string   | false       |  |
| repoName     | string   | false       |  |
| responseName     | string   | false       |  |


#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| authorizedIpList     | array<string>   | 分享IP |
| authorizedUserList     | array<string>   | 分享用户 |
| expireSeconds     | integer , format:int64  | 有效时间，单位秒 |

### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 分享记录信息   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

