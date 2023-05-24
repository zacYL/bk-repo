# maven jar 版本详情接口
功能描述：maven jar 版本详情接口

### 请求地址
```
/ext/version/detail/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| artifact     | boolean   | false       |  |
| artifactFullPath     | string   | false       |  |
| artifactId     | string   | false       |  |
| artifactName     | string   | false       |  |
| artifactVersion     | string   | false       |  |
| groupId     | string   | false       |  |
| jarName     | string   | false       |  |
| metadata     | boolean   | false       |  |
| projectId     | string   | false       |  |
| repoIdentify     | string   | false       |  |
| repoName     | string   | false       |  |
| responseName     | string   | false       |  |
| snapshot     | boolean   | false       |  |
| valid     | boolean   | false       |  |
| versionId     | string   | false       |  |
| packageKey     | string   | true       | packageKey |
| version     | string   | true       | version |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     |    | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

