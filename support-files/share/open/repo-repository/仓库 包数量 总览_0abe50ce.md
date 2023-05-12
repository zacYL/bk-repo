# 仓库 包数量 总览
功能描述：仓库 包数量 总览

### 请求地址
```
/api/software/package/search/overview
```

### 请求方法
`GET`
### 请求参数

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| repoType     | string   | true       | repoType;枚举：[COMPOSER, DOCKER, GENERIC, GIT, HELM, MAVEN, NONE, NPM, NUGET, OCI, PYPI, RDS, RPM] |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |
| packageName     | string   | true       | packageName |



### 返回结果

