package org.addressbook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.service.ApplicationProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ApplicationScoped
public class CarddavCollectService {
    @Inject
    ApplicationProperties properties;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    BaseService baseService;

    public List<Node> getMkcolNodes(List<Node> nodes) {
        List<Node> deepNodes = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().equalsIgnoreCase("mkcol")) {
                deepNodes.addAll(node.childNodes());
                break;
            }else if(!node.childNodes().isEmpty()){
                deepNodes = getMkcolNodes(node.childNodes());
                if(!deepNodes.isEmpty()) {
                    break;
                }
            }
        }
        return deepNodes;
    }

    public List<Node> getProps(List<Node> nodes) {
        List<Node> deepNodes = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().equals("prop")) {
                deepNodes.addAll(node.childNodes());
                break;
            } else if (!node.childNodes().isEmpty()) {
                deepNodes = getProps(node.childNodes());
                if(!deepNodes.isEmpty()){
                    break;
                }
            }
        }
        return deepNodes;
    }

    public boolean mkColCollectCreate(String userPrinciple, String collectId, List<Node> props) {
        var file = new File(properties.addressbookLocation() + userPrinciple + File.separator + collectId);
        if(!file.mkdirs())  {
            return false;
        }
        var cacheFile = new File(file.getAbsolutePath()+ File.separator + "/.cache/sync-token");
        if(!cacheFile.mkdirs()) {
            Log.infof("syncToken mkdirs");
        }
        var propFile = new File(file.getAbsolutePath() + File.separator + ".props");
        var propMap = new HashMap<String, String>();
        for(var node:props) {
            if(node.normalName().equals("resourcetype")) {
                for(var child: node.childNodes()) {
                    if(child.normalName().matches("([0-9a-zA-Z-]+:)?addressbook")) {
                        propMap.put("tag", "VADDRESSBOOK");
                    }
                }
            } else if (node.normalName().matches("([0-9a-zA-Z-]+:)?displayname")) {
                var p = (TextNode)node.firstChild();
                assert p != null;
                propMap.put(node.normalName(),p.text());
            } else if(node.normalName().matches("([0-9a-zA-Z-]+:)?addressbook-description")) {
                var p = (TextNode)node.firstChild();
                assert p != null;
                propMap.put(node.normalName(), p.text());
            }
        }
        try(var outputstream = new FileOutputStream(propFile)) {
            outputstream.write(objectMapper.writeValueAsString(propMap).getBytes());
            BaseService.PROPS.put(userPrinciple+LockService.LOCK_PREFIX+collectId, propMap);
            return true;
        }catch (Exception ie) {
            return false;
        }
    }

    public boolean deleteCollect(String user, String collectId) {
        var key = user + LockService.LOCK_PREFIX + collectId;
        var collectFile  = new File(properties.addressbookLocation() + user+ File.separator + collectId + File.separator);
        deleteFiles(collectFile);
        BaseService.PROPS.remove(key);
        return true;
    }

    public void deleteFiles(File file) {
        if(file.isDirectory() && file.listFiles().length > 0) {
            var childs = file.listFiles();
            for(var child:childs) {
                if(child.isDirectory() && child.listFiles().length > 0) {
                    deleteFiles(child);
                }else{
                    child.delete();
                }
            }
        }
        file.delete();
    }

    public Document deleteResponseGen(String collect) {
        Document document = DocumentHelper.createDocument();
        var multistatus = baseService.multistatusGen(document);
        Element response = multistatus.addElement("response");
        response.addElement("href").setText(collect);
        response.addElement("status").setText(BaseService.HTTP_OK);
        return document;
    }
}
