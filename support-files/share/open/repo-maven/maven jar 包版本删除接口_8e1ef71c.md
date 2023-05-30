# maven jar 包版本删除接口
功能描述：maven jar 包版本删除接口

### 请求地址
```
/repo-maven/ext/version/delete/{projectId}/{repoName}
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
| version     | string   | true       | version |
| packageKey     | string   | true       | packageKey |



### 返回结果

