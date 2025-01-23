# 获取文件引用信息
功能描述：获取文件引用信息

### 请求地址
```
/api/references/{sha256}
```

### 请求方法
`GET`
### 请求参数
#### 路径参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| sha256     | string   | true       | sha256 |

#### 查询参数

| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| projectId     | string   | false       | projectId |
| repoName     | string   | false       | repoName |
| credentialsKey     | string   | false       | credentialsKey |



### 返回结果
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| code     | integer , format:int32  | 返回码 |
| data     | 文件引用信息   | 数据 |
| message     | string   | 错误信息 |
| traceId     | string   | 链路追踪id |
#### 文件引用信息
| 字段 | 类型 | 描述 |
| -------- | -------- | -------- |
| count     | integer , format:int64  | 当前记录文件被引用次数 |
| credentialsKey     | string   | 文件实际存储所在存储 |
| sha256     | string   | 所引用文件的sha256 |

