# Generic通用制品仓库消息通知接口

[toc]

## 文件共享邮件通知

- API: POST /generic/notify/mail
- API 名称: file_share_mail_notify
- 功能说明：
	- 中文：文件共享邮件通知
	- English：file share mail notify

- 请求体

  ``` txt
  {"url":{url}}
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

