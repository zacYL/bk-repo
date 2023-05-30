# 根据名称类型查询仓库
功能描述：根据名称类型查询仓库

### 请求地址
```
/repo-repository/api/repo/info/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | 所属项目 |
| repoName     | string   | true       | 仓库名称 |
| type     | string   | true       | 仓库类型 |




### 返回结果

