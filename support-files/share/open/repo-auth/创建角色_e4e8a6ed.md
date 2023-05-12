# 创建角色
功能描述：创建角色

### 请求地址
```
/api/role/create
```

### 请求方法
`POST`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| admin     | boolean   | 管理员 |
| description     | string   | 描述信息 |
| name     | string   | 角色名称 |
| projectId     | string   | 项目ID |
| repoName     | string   | 仓库名称 |
| roleId     | string   | 角色id |
| type     | string   | 角色类型 |

### 返回结果

