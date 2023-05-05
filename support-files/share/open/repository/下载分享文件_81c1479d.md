# 下载分享文件
功能描述：下载分享文件

### 请求地址
```
/api/share/{projectId}/{repoName}/**
```

### 请求方法
`GET`
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
| token     | string   | true       | token |
| userId     | string   | true       | userId |



### 返回结果

| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |

