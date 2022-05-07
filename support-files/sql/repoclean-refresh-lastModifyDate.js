//初始管理员账号，userId:admin , password：bkrepo

db.getCollection('package_version').find({}).forEach(
    function(item){
        db.getCollection('package_version').update({'_id':item._id},{$set:{'lastModifiedDate':new ISODate()}})
    }
);
