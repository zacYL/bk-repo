//初始管理员账号，userId:admin , password：bkrepo
db.user.insert({
    "userId" : "admin",
    "name" : "admin",
    "pwd" : "ed424c8ee8955e30b9975ed83fdfef61",
    "admin" : true,
    "locked" : false,
    "tokens" : [],
    "roles" : [],
    "asstUsers" : [],
    "group" : false,
    "createdDate" : ISODate("2021-11-04T06:08:08.197Z"),
    "lastModifiedDate" : ISODate("2021-11-04T06:08:08.197Z")
})