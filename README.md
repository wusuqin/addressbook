## Introduction

Addressbook  is a simple carddav server. With this application you can share your contacts over your carddav clients.
Some signicant defects are list as above:
- AddressBook applications does not have a web home page, instead you can use swagger-ui to create users or collections;
- Addressbook application does not offer caldav abilities；
- Interactins test with davX5 and thundbird(the haven been test) have done and it can run correctly, but tests are not ye notarizrd for macos;
- Addrssbook application only be test on local area network;

## configs 

Addresbook user and collection profiles are saved under the path /opt/addresbbok/data

With postman or a cli terminal, you create a user with password by running the following command.

``` shell
curl -X PUT http://127.0.0.1:8080/carddav/user/{user}/password/{password}
```

Collections could be created b running the following command.

```shell
curl -X MKCOL http://root:root@127.0.0.1:8080/carddav/{user}/{collectid} -H "Content-Type:text/xml"
-d '<?xml version="1.0" encoding="UTF-8" ?><mkcol xmlns="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav" xmlns:CR="urn:ietf:params:xml:ns:carddav" xmlns:CS="http://calendarserver.org/ns/" xmlns:I="http://apple.com/ns/ical/" xmlns:INF="http://inf-it.com/ns/ab/"><set><prop><resourcetype><collection /><CR:addressbook /></resourcetype><displayname>title-2</displayname><CR:addressbook-description>description-2</CR:addressbook-description></prop></set></mkcol>'
```

A collection will be delete by running the following command.

```shell
curl -X DELETE http://127.0.0.1:8080/carddav/{user}/{collectid}
```

If you want to interact with this application by cardav clients, , set the following configs on our davx5, 
url: http://{ip}:8080/carddav/{user}/ user:{user} password:{password}

Or set the following configs on thundbird:
url: http://{ip}:8080/carddav/{user}/{collectionId} user:{user} password:{password}
