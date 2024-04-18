package org.addressbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.internal.StringUtil;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.service.ApplicationProperties;

import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class AuthService {
    @Inject
    ApplicationProperties properties;

    @Inject
    ObjectMapper objectMapper;

    private final static Map<String, String> secrets = new HashMap<>();

    @PostConstruct
    void init() {
        var secretFile = new File(properties.addressbookLocation() + "./secrets");
        if(secretFile.exists()) {
            try (var inputStream = new FileInputStream(secretFile)) {
                var bytes = inputStream.readAllBytes();
                var mapper = objectMapper.readTree(bytes, 0 ,bytes.length);
                var keys = mapper.fieldNames();
                while( keys.hasNext()) {
                    var key = keys.next();
                    var value = mapper.get(key).textValue();
                    secrets.put(key, value);
                }
            }catch (Exception ie) {
                Log.error("read secretFile failed");
            }
        }
    }
    public boolean checkAuthResult(String security){
        if(StringUtil.isNullOrEmpty(security)) {
            return false;
        }
        var userPass = new String(Base64.getDecoder().decode(security.replace("Basic ", ""))).split(":");
        if(userPass.length <2) {
            return false;
        }else if(secrets.containsKey(userPass[0]) && secrets.get(userPass[0]).equals(userPass[1])) {
            return true;
        }else {
            return false;
        }
    }
}
