## Introduction

Addressbook  is a kind of light carddav server. It can get you to implement a simple carddav application.
It also has a set of defects list as above:
- AddressBook applications has no web home, you can use swagger-ui create users or collections instead;
- Addressbook application only supports carddav protocols， caldav protocols were not implemented；
- cardav client such as davX5 and thundbird(the haven been test) can run correctly on this application, any client based on macos were not test;

## configs 

addresbook users are put on the path /opt/addresbbok/data

run this application and with a postman, you can create a user with password by calling the above api:

``` shell
curl -X PUT http://127.0.0.1:8080/carddav/user/{user}/password/{password}
```

call MKCOL api a collection (eg: collectId) of user can be created

```shell
curl -X MKCOL http://root:root@127.0.0.1:8080/carddav/{user}/{collectid} -H "Content-Type:text/xml"
-d '<?xml version="1.0" encoding="UTF-8" ?><mkcol xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav" xmlns:CR="urn:ietf:params:xml:ns:carddav" xmlns:CS="http://calendarserver.org/ns/" xmlns:I="http://apple.com/ns/ical/" xmlns:INF="http://inf-it.com/ns/ab/"><set><prop><resourcetype><collection /><CR:addressbook /></resourcetype><displayname>title-2</displayname><CR:addressbook-description>description-2</CR:addressbook-description></prop></set></mkcol>'
```

call api above is implemented to delete a collection

```shell
curl -X DELETE http://127.0.0.1:8080/carddav/{user}/{collectid}
```

In addition, try to config your carddav server in davx5(or thundbird), nake sure that your carddav client and addressbook application are in the same local area network
