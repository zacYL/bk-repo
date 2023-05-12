# 更新账号
功能描述：更新账号

### 请求地址
```
/api/account/update
```

### 请求方法
`PUT`
### 请求参数



#### 请求体参数
| 字段 | 类型 | 必填 | 描述 |
| -------- | -------- | -------- | -------- |
| appId     | string   | 系统Id |
| authorizationGrantTypes     | array<string>   | 授权方式 |
| avatarUrl     | string   | 应用图标地址 |
| description     | string   | 简要描述 |
| homepageUrl     | string   | 应用主页 |
| locked     | boolean   | 是否锁定 |
| redirectUri     | string   | 应用回调地址 |
| scope     | array<string>   | 权限范围 |

### 返回结果

