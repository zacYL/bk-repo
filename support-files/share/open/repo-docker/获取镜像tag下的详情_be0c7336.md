# 获取镜像tag下的详情
功能描述：获取镜像tag下的详情

### 请求地址
```
/ext/version/detail/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| userId     | string   | false       | userId |
| packageKey     | string   | true       | packageKey |
| version     | string   | true       | version |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | docker镜像tag信息   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

