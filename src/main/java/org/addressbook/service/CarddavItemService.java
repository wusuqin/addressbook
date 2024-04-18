package org.addressbook.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.util.internal.StringUtil;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import io.quarkus.logging.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.addressbook.entity.ContactAttributeEnum;
import org.addressbook.entity.Item;
import org.apache.commons.codec.binary.Hex;
import org.service.ApplicationProperties;

@ApplicationScoped
public class CarddavItemService {
    @Inject
    ApplicationProperties properties;

    @Inject
    ObjectMapper objecMapper;

    public final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    public final ReentrantReadWriteLock.ReadLock rLock = rwLock.readLock();

    public final ReentrantReadWriteLock.WriteLock wLock = rwLock.writeLock();
    public final static Map<String, Map<String, Map<String, String>>> syncToken = new HashMap<>();


    @SneakyThrows
    public Map<String, Map<String, String>> getSyncToken(String aoId){
        if(syncToken.containsKey(aoId)) {
            return syncToken.get(aoId);
        }
        var  messageDigest = MessageDigest.getInstance("SHA-256");
        Map<String, String> fileMap = new HashMap<>();
        var items = getItemList(aoId);
        //null标识有用户正在写vcf
        if(Objects.isNull(items)) {
            return null;
        }
        for(var item:items) {
            messageDigest.update((item.getHref() + "/" +item.getEtag()).getBytes());
            fileMap.put(item.getHref(),item.getEtag());
        }
        var token = Hex.encodeHexString(messageDigest.digest());
        var file = new File(properties.addressbookLocation() + aoId + File.separator +".cache" +File.separator+"sync-token"+File.separator + token);
        if(!file.exists()) {
            try(var output = new FileOutputStream(file);
                FileChannel channel = output.getChannel();
                var ignored = channel.lock();){
                output.write(objecMapper.writeValueAsString(fileMap).getBytes());
            }
        }
        syncToken.put(aoId,Map.of(token, fileMap));
        Log.info("sync-token of user "+aoId+" create, token is " + token);
        return Map.of(token,fileMap);
    }

    public String getSyncTokenValue(String aoId) {
        String token = "0";
        for(var key:getSyncToken(aoId).keySet()) {
            if(!StringUtil.isNullOrEmpty(key)) {
                token = key;
                break;
            }
        }
        return token;
    }

    public List<Item> getItemList(String aoId) {
        Log.info("user of " + aoId + " has get the readLock");
        rLock.lock();
        try{
            List<Item> items = new ArrayList<>();
            var files = new File(properties.addressbookLocation() + aoId);
            for(var file: Objects.requireNonNull(files.listFiles())) {
                if(!file.getName().equals(".props") && file.isFile()) {
                    items.add(getItem(file));
                }
            }
            return items;
        }finally {
            rLock.unlock();
            Log.info("user of " + aoId + " release the readLock");
        }
    }

    @SneakyThrows
    public Item readVcf(String body, String vcfId) {
        if(!body.startsWith("BEGIN:VCARD")) {
            return null;
        }
        var flag = false;
        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(body.getBytes());
        Pattern pattern = Pattern.compile("(PHOTO(?:;[^:\\r\\n]*)?;ENCODING=b(?:;[^:\\r\\n]*)?:[\\s\\S]+?\\n)\\S+:");
        Matcher matcher = pattern.matcher(body);
        var itemBuilder = Item.builder().etag(Hex.encodeHexString(messageDigest.digest())).text(body).href(vcfId);
        var bodyNew = body;
        if(matcher.find()) {
            String photo = matcher.group(1);
            String[] photoAttr = photo.replace("\r\n", "\n").
                    replace("\n", "").
                    replace(" ", "").split(":");
            bodyNew = body.replace(photo, "");
        }
        String[] bodyLines = bodyNew.split("\n");
        var uidFlag = false;
        for(int i=0;i<bodyLines.length;i++) {
            var line = bodyLines[i].replace("\r", "");
            for(int j=i+1;j<bodyLines.length;j++) {
                if(!bodyLines[j].matches("[^:]+:[\\S ]*\r?")) {
                    line = line.concat(bodyLines[j].replace("\r",""));
                    i++;
                }else{
                    break;
                }
            }
            if(line.startsWith("END:VCARD")){
                flag = true;
                break;
            }else{
                var attr = line.split(":");
                switch (ContactAttributeEnum.valueFrom(attr[0])) {
                    case "fullName" -> {
                        if (attr.length == 1) {
                            itemBuilder.fullName("");
                        } else {
                            itemBuilder.fullName(attr[1]);
                        }
                    }
                    case "name" -> itemBuilder.name(attr.length > 1 ? attr[1] : "");
                    case "uid" -> {itemBuilder.uid(attr[1]);uidFlag = true;}
                    case "version" -> itemBuilder.version(attr[1]);
                    case "note" -> itemBuilder.note(attr.length > 1 ? attr[1] : "");
                    case "org" ->
                            itemBuilder.org(attr.length > 1 ? attr[1].endsWith(";") ? attr[1].substring(0, attr[1].length() - 1) : attr[1] : "");
                    case "nickName" -> itemBuilder.nickName(attr.length > 1 ? attr[1] : "");
                    case "title" -> itemBuilder.title(attr.length > 1 ? attr[1] : "");
                    case "bday" -> itemBuilder.bday(attr.length > 1 ? attr[1] : "");
                    case "rev" -> itemBuilder.rev(attr.length > 1 ? attr[1] : "");
                }
            }
        }
        if(!flag || !uidFlag) {return null;}
        return itemBuilder.build();
    }

    public Item getItem(File file) {
        try(var inputStream = new FileInputStream(file);
            FileChannel channel = inputStream.getChannel();
            var ignored = channel.lock(0, Long.MAX_VALUE,true);) {
            var bytes = inputStream.readAllBytes();
            return readVcf(new String(bytes), file.getName());
        }catch (Exception ie) {
            return new Item();
        }
    }

    public Map<String, String> getTokenVcfs(String token,String aoId) {
        var tokenFile = new File(properties.addressbookLocation() + aoId +"/.cache/sync-token/"+ token );
        if(tokenFile.exists()) {
            try(var inputStream = new FileInputStream(tokenFile);
                FileChannel channel = inputStream.getChannel();
                var ignored = channel.lock(0, Long.MAX_VALUE, true);) {
                return objecMapper.readValue(inputStream.readAllBytes(), new TypeReference<Map<String, String>>() {});
            }catch (Exception ie) {
                return Map.of();
            }
        }else{
            return Map.of();
        }
    }

    public String writeVcf(String aoId, String body, String vcfId) {
        if(wLock.tryLock()) {
            Log.info("writeVcf: user of " + aoId + " has get the writeLock");
            try{
                if(syncToken.containsKey(aoId)) {
                    syncToken.remove(aoId);
                    Log.info("sync-token of user " + aoId +" remove");
                }
                try(var output = new FileOutputStream(properties.addressbookLocation() + aoId + "/" + vcfId);
                    FileChannel fileChannel = output.getChannel();
                    FileLock lock = fileChannel.lock()){
                    output.write(body.getBytes());
                    return "201";
                }catch (Exception ie) {
                    return "405";
                }
            } finally {
                wLock.unlock();
                Log.info("writeVcf: user of " + aoId + " has release the writeLock");
            }
        }else{
            Log.info("writevf of user "+aoId+" lock failed");
            return "405";
        }
    }

    public String deleteVcfsAll(String aoId) {
        if(wLock.tryLock()) {
            Log.info("deleteAllVcfs oper: user of " + aoId + " has get the writeLock");
            try{
                syncToken.remove(aoId);
                List<File> fails = new ArrayList<>();
                var folder =new File(properties.addressbookLocation() + aoId) ;
                for(var file: Objects.requireNonNull(folder.listFiles())) {
                    if(!file.getName().equals(".props") && !file.getName().equals(".cache") ) {
                        if(!file.delete()) {
                            fails.add(file);
                        }
                    }
                }
                if(fails.isEmpty()) {
                    Log.info("DeletAllVcf oper: writers of user writes over "+aoId+" is success");
                    return "200";
                }else{
                    Log.error("DeletaAllVcfs failed, fail file is " + fails.stream().map(File::getName).toList());
                    return "405";
                }
            }finally {
                wLock.unlock();
                Log.info("deleteAllVcfs oper: user of " + aoId + " has release the writeLock");
            }
        }else{
            return "405";
        }
    }

    public String deleteVcf(String aoId, String ifMatch,String href) {
        if(wLock.tryLock()) {
            Log.info("deletevcfs: user of " + aoId + " has get the writeLock");
            try{
                File file = new File(properties.addressbookLocation() + aoId + "/" + href);
                var code = "200";
                if(file.exists()) {
                    if(syncToken.containsKey(aoId)) {
                        syncToken.remove(aoId);
                        Log.info("Delete oper: sync-token of user " + aoId +" remove");
                    }
                    var item = getItem(file);
                    if(Objects.isNull(ifMatch) || ifMatch.equals("*") || item.getEtag().equals(ifMatch.replace("\"", ""))) {
                        if (!file.delete()) {
                            code = "412";
                        }
                    }
                    return code;
                }else{
                    return "404";
                }
            }finally {
                wLock.unlock();
                Log.info("deletevcfs: user of " + aoId + " has release the writeLock");
            }
        }else{
            return "405";
        }
    }

}
