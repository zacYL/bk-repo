# 批量删除节点
功能描述：批量删除节点

### 请求地址
```
/api/node/batch/{projectId}/{repoName}
```

### 请求方法
`DELETE`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| null     | array<null>   |  |

### 返回结果

