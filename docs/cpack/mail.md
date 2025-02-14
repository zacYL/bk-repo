# Generic通用制品仓库消息通知接口

[toc]

## 文件共享邮件通知

- API: POST /generic/notify/mail
- API 名称: file_share_mail_notify
- 功能说明：
	- 中文：文件共享邮件通知
	- English：file share mail notify

- 请求体

  ``` json
  {"url": "http://test.com"}
  ```
  
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |url|string|是|无|下载链接|Download link|
  
- 响应体

``` json
{
  "code" : 0,
  "message" : null,
  "data" : true,
  "traceId" : null
}
```

- data字段说明



## 文件共享指定分享人邮件通知

- API: POST /generic/notify/mail/user

- API 名称: file_share_mail_notify_user

- 功能说明：

  - 中文：文件共享指定分享人邮件通知
  - English：file share mail notify with user

- 请求体

  ``` json
  {
    "url": "http://test.com",
    "users": ["abc","bcd"]
  }
  ```

- 请求字段说明

  | 字段  | 类型         | 是否必须 | 默认值 | 说明     | Description   |
  | ----- | ------------ | -------- | ------ | -------- | ------------- |
  | url   | string       | 是       | 无     | 下载链接 | Download link |
  | users | List<String> | 是       | 无     | 分享人   | share users   |

- 响应体

``` json
{
  "code" : 0,
  "message" : null,
  "data" : true,
  "traceId" : null
}
```

- data字段说明

