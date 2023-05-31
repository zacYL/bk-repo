# 仓库清理debug接口，调用该方法后会立即执行仓库清理动作
功能描述：仓库清理debug接口，调用该方法后会立即执行仓库清理动作

### 请求地址
```
/repo-repository/api/repo/debug/clean/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |




### 返回结果

