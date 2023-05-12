# 分页查询包
功能描述：分页查询包

### 请求地址
```
/api/package/page/{projectId}/{repoName}
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
| packageName     | string   | false       | 包名称, 根据该字段模糊搜索 |
| pageNumber     | integer   | false       | 当前页 |
| pageSize     | integer   | false       | 分页大小 |



### 返回结果

