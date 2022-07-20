# 制品晋级
功能描述：制品晋级

### 请求地址
```
/api/stage/upgrade/{projectId}/{repoName}
```

### 请求方法
`POST`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| packageKey     | string   | true       | packageKey |
| version     | string   | true       | version |
| tag     | string   | true       | tag |


#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |

### 返回结果

