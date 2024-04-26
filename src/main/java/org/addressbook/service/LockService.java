package org.addressbook.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ApplicationScoped
public class LockService {
    public static final Map<String, ReentrantReadWriteLock> lockMap = new ConcurrentHashMap<>();

    public static final String LOCK_PREFIX = "_";

    public static void initialLockService(String user, String collectId){
        if(!lockMap.containsKey(user + LOCK_PREFIX + collectId)) {
            lockMap.put(user + LOCK_PREFIX +collectId, new ReentrantReadWriteLock());
        }
    }
}
