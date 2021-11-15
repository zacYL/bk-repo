# Auth接口

[toc]

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

