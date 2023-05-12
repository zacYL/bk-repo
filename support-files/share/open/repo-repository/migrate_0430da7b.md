# migrate
功能描述：migrate

### 请求地址
```
/api/job/migrate/storage/{projectId}/{repoName}/{newKey}
```

### 请求方法
`POST`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | true       | projectId |
| repoName     | string   | true       | repoName |
| newKey     | string   | true       | newKey |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| failedPointId     | string   | true       | failedPointId |
| skipPage     | integer   | true       | skipPage |
| preStartTime     | string   | true       | preStartTime |



### 返回结果

