package org.addressbook.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.service.ApplicationProperties;

import java.io.*;
import java.util.*;

@ApplicationScoped
public class BaseService {
    public static final String HTTP_OK = "HTTP/1.1 200 OK";

    public static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";

    @Inject
    ApplicationProperties properties;

    @Inject
    ObjectMapper objectMapper;

    public static final Map<String, Map<String, String>> PROPS = new HashMap<>();

    @PostConstruct
    void init() {
        getPropsFiles(new File(properties.addressbookLocation()));
    }

    public void getPropsFiles(File file) {
        for(var f:file.listFiles()) {
            if(f.isDirectory()) {
                getPropsFiles(f);
            }else if (f.getName().endsWith(".props")){
                var collectId = f.getParentFile().getName();
                var user = f.getParentFile().getParentFile().getName();
                PROPS.put(user + LockService.LOCK_PREFIX + collectId, getCollectProps(user, collectId, f));
            }
        }
    }

    public Element multistatusGen(Document document) {
        Element multistatus =document.addElement("multistatus", "DAV:");
        multistatus.addNamespace("ical", "http://apple.com/ns/ical/");
        multistatus.addNamespace("c", "urn:ietf:params:xml:ns:caldav");
        multistatus.addNamespace("cr", "urn:ietf:params:xml:ns:carddav");
        multistatus.addNamespace("cs", "http://calendarserver.org/ns/");
        return multistatus;
    }

    public Element errorGen(Document document) {
        Element error = document.addElement("error", "DAV:");
        return error;
    }

    public Map<String, String> getCollectProps(String user, String collectId, File file) {
        LockService.initialLockService(user, collectId);
        var lock = LockService.lockMap.get(user+LockService.LOCK_PREFIX+collectId);
        try (var propFile = new FileInputStream(file)) {
            lock.readLock().lock();
            Map<String, String> map = objectMapper.readValue(new String(propFile.readAllBytes()), new TypeReference<>() {});
            return map;
        } catch (IOException e) {
            return new HashMap<>();
        }finally {
            lock.readLock().unlock();
        }
    }
}
