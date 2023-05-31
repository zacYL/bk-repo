# 删除repo下的指定镜像
功能描述：删除repo下的指定镜像

### 请求地址
```
/repo-docker/ext/version/delete/{projectId}/{repoName}
```

### 请求方法
`DELETE`
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


#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |

### 返回结果

