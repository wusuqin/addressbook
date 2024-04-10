package org.addressbook.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Base64;

@ApplicationScoped
public class AuthService {
    public Boolean checkAuthResult(String security){
    var userPass = new String(Base64.getDecoder().decode(security.replace("Basic ", ""))).split(":");
    if(userPass.length == 2 ) {
        var abOp = abAuthRepository.findByUserAndPassword(userPass[0], userPass[1]);
        if(abOp.isPresent()) {
            var userId = abOp.get().getUserId();
            return appletService.getMemberAppletPermission("通讯录", String.valueOf(userId))?abOp.get():null;
        }else {
            return null;
        }
    }else {
        return null;
    }
}
}
