# 软件源仓库列表
功能描述：软件源仓库列表

### 请求地址
```
/api/software/repo/page/{pageNumber}/{pageSize}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| pageNumber     | integer   | true       | 当前页 |
| pageSize     | integer   | true       | 分页大小 |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| userId     | string   | false       | userId |
| projectId     | string   | true       | 项目id |
| name     | string   | true       | 仓库名 |
| type     | string   | true       | 仓库类型 |



### 返回结果

