# maven jar 包删除接口
功能描述：maven jar 包删除接口

### 请求地址
```
/repo-maven/ext/package/delete/{projectId}/{repoName}
```

### 请求方法
`DELETE`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| artifactFullPath     | string   | false       |  |
| artifactName     | string   | false       |  |
| artifactVersion     | string   | false       |  |
| packageName     | string   | false       |  |
| projectId     | string   | false       |  |
| repoIdentify     | string   | false       |  |
| repoName     | string   | false       |  |
| responseName     | string   | false       |  |
| version     | string   | false       |  |
| packageKey     | string   | true       | packageKey |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |

