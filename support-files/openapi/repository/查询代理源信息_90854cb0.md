# 查询代理源信息
功能描述：查询代理源信息

### 请求地址
```
/api/proxy-channel/{projectId}/{repoName}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | 所属项目 |
| repoName     | string   | true       | 仓库名称 |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| repoType     | string   | true       | type |
| name     | string   | true       | name |



### 返回结果

