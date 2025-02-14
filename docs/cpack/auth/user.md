

# Auth接口

[toc]

## 批量创建用户

- API: POST /auth/api/user/batch
- API 名称: batch_create_user
- 功能说明：
  - 中文：批量创建用户
  - English：batch create user

- input body:

``` json
[
    {
        "userId":"BK-CICD",
        "name":"BK-CICD",
        "email": "onnt1997@outlook.com",
        "phone": "11113451234"
    },
    {
        "userId":"BK-CICD1",
        "name":"BK-CICD1",
        "email": "onnt1997@outlook.com",
        "phone": "11113451234"
    }
]

```


- input 字段说明

| 字段   | 类型   | 是否必须 | 默认值 | 说明     | Description  |
| ------ | ------ | -------- | ------ | -------- | ------------ |
| name   | string | 是       | 无     | 用户名   | the  name    |
| userId | string | 是       | 无     | 用户id   | the user id  |
| email  | string | 否       | 无     | 邮箱     | email        |
| phone  | string | 否       | 无     | 联系电话 | phone number |

- output:

```
{
    "code": 0,
    "message": null,
    "data": {
        "success": 2,
        "failed": 0,
        "failedUsers": []
    },
    "traceId": ""
}
```

- output 字段说明

| 字段             | 类型           | 说明                                    | Description               |
| ---------------- | -------------- | --------------------------------------- | ------------------------- |
| code             | bool           | 错误编码。 0表示success，>0表示失败错误 | 0:success, other: failure |
| message          | result message | 错误消息                                | the failure message       |
| data.success     | int            | 成功数量                                | Success count             |
| data.failed      | int            | 失败数量                                | Failed count              |
| data.failedUsers | list<String>   | 失败userId 集合                         | Failed userId list        |
| traceId          | string         | 请求跟踪id                              | the trace id              |





## 全部用户

- API: GET /auth/api/user/list

- API 名称: user_list

- 功能说明：**改接口只有管理员有权限调用**
  
  - 中文：用户列表
  - English：user list
  
- 请求体

  ```json
  此接口请求体为空
  ```
  
- 请求字段说明

  
  
- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "userId":"anc",
        "name":"abc"
      },
      {
        "userId":"anc",
        "name":"abc"
      }
    ],
    "traceId": null
  }
  ```

- data 字段说明

  | 字段   | 类型   | 说明   | Description |
  | ------ | ------ | ------ | ----------- |
  | name   | string | 用户名 | User name   |
  | userId | string | 用户ID | User id     |



## 项目下用户列表

- API: GET /auth/api/user/list/{projectId}
- API 名称: project_user_list
- 功能说明：
  - 中文：项目下用户列表
  - English：project user list
- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明 | Description  |
  | --------- | ------ | -------- | ------ | ---- | ------------ |
  | projectId | string | 是       | 无     |      | proejct name |

- 响应体

  ``` json
  {
    "code": 0,
    "message": null,
    "data": [
      {
        "userId":"anc",
        "name":"abc"
      },
      {
        "userId":"anc",
        "name":"abc"
      }
    ],
    "traceId": null
  }
  ```

- data 字段说明

  | 字段|类型|说明|Description|
  |---|---|---|---|
  |name|string|用户名|User name |
  |userId|string|用户ID|User id|

## 用户是否指定项目下管理员

- API: GET /auth/api/user/admin/{projectId}

- API 名称: is_project_admin

- 功能说明：

  - 中文：用户是否指定项目下管理员
  - English：is project admin

- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明 | Description  |
  | --------- | ------ | -------- | ------ | ---- | ------------ |
  | projectId | string | 是       | 无     |      | proejct name |

- 响应体

  ``` json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```

- data 字段说明

  | 字段 | 类型    | 说明                                 | Description |
  | ---- | ------- | ------------------------------------ | ----------- |
  | data | boolean | true:项目管理员，false: 非项目管理员 | User name   |

## 获取公钥

- API：GET /auth/api/user/rsa

- API 名称: get_rsa_public_key

- 功能说明：

  - 中文：获取rsa公钥
  - English：get rsa public key

- 请求体
  此接口请求体为空

- 请求字段说明
  请求字段为空

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDnn7+FI7eu5iOhFv6hGz1MtUnh6gIXsCGrgjEFAnvehzYjiEK4gGic/5TillkatPm3sNnTh4KXDMLagJHSbOvfth5Ml9vJYIXeNz3pXuA2EFfr6JDaftvLYLXNGJOS++JohwJjZ3x7Y1dswYAuUga2YTElSRNZWwPmGzrDqp7WzwIDAQAB",
      "traceId": ""
  }
  ```

- data字段说明

  | 字段 | 类型   | 说明 | Description |
  | ---- | ------ | ---- | ----------- |
  | data | String | 公钥 | publicKey   |



## 重设指定用户密码

* API: POST /auth/api/user/reset/{userId}?newPwd={newPwd}

* API 名称: reset_user_password

* 功能说明：

  - 中文：重设指定用户密码
  - English：reset user password

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段   | 类型   | 是否必须 | 默认值 | 说明                         | Description |
  | ------ | ------ | -------- | ------ | ---------------------------- | ----------- |
  | userId | string | 是       | 无     | 需要重设密码 userId          | userId      |
  | newPwd | string | 否       | 无     | 重置密码, 为空重设为默认密码 | newPwd      |

* 响应体

  ``` json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
- data 字段说明

  | 字段 | 类型    | 说明                                   | Description |
  | ---- | ------- | -------------------------------------- | ----------- |
  | data | boolean | true:重置密码成功，false: 重置密码失败 | data        |


