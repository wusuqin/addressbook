package org.addressbook.service;

import io.netty.util.internal.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.addressbook.entity.Item;
import org.dom4j.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.service.ApplicationProperties;

import java.io.File;
import java.util.*;

@ApplicationScoped
public class RootPropService {
    @Inject
    CarddavItemService carddavItemService;

    @Inject
    ApplicationProperties properties;

    public void hrefResponseGen(List<Node> props, String userPrinciple, String user, String collectId, String vcfId, Integer depth, Element multistatus) {
        if(depth >=3 && Objects.isNull(vcfId)) {
            var k = user+LockService.LOCK_PREFIX+collectId;
            carddavItemService.getSyncToken(user,collectId).forEach((key, tokenMap) -> {
                tokenMap.forEach( (vcf, getag) -> {
                    hrefResponseGen(props, userPrinciple, user,collectId,vcf,3,multistatus);
                });
            });
            return;
        }
        Element response = multistatus.addElement("response");
        Element href = response.addElement("href");
        href.setText( switch(depth) {
            case 0 ->  "/carddav/";
            case 1 ->  "/carddav/" + user + "/";
            case 2 ->  "/carddav/" + user + "/" + collectId +"/";
            case 3 ->  "/carddav/" + user + "/" + collectId +"/" + vcfId;
            default -> null;
        });
        var knownPropstat  =response.addElement("propstat");
        Element knownProp = knownPropstat.addElement("prop");
        var  unPropstat = response.addElement("propstat");
        Element unProp = unPropstat.addElement("prop");
        for(var prop:props) {
            if(prop.normalName().equalsIgnoreCase("resourcetype")) {
                switch (depth) {
                    case 0 -> {var resourcetype = knownProp.addElement("resourcetype"); resourcetype.addElement("collection");}
                    case 1 -> {var resourcetype = knownProp.addElement("resourcetype");
                         resourcetype.addElement("collection");
                        resourcetype.addElement("principle");
                    }
                    case 2 -> {var resourcetype = knownProp.addElement("resourcetype");
                        resourcetype.addElement("collection"); resourcetype.addElement("cr:addressbook");
                    }
                    case 3 -> knownProp.addElement("resourcetype");
                    default -> {}
                }
            } else if (prop.normalName().equalsIgnoreCase("displayname")) {
                switch (depth) {
                    case 2 ->{var displayname =  knownProp.addElement("displayname");
                        displayname.setText(BaseService.PROPS.get(user+LockService.LOCK_PREFIX+collectId).get("displayname")); }
                    default -> unProp.addElement("displayname");
                }
            } else if (prop.normalName().equalsIgnoreCase("current-user-privilege-set")) {
                switch (depth) {
                    case 0 -> {
                        var userSet = knownProp.addElement("current-user-privilege-set");
                        privilegesGen(List.of("read"), userSet);
                    }
                    case 1,2,3-> {
                        var userSet = knownProp.addElement("current-user-privilege-set");
                        privilegesGen(List.of("read", "write", "all", "write-properties", "write-content"), userSet);
                    }
                    default -> {}
                }
            } else if (prop.normalName().equalsIgnoreCase("current-user-principal")) {
                var current = knownProp.addElement("current-user-principal");
                var h = current.addElement("href");
                h.setText("/carddav/" + userPrinciple + "/");
            } else if(prop.normalName().matches("([0-9a-zA-Z-]+:)?addressbook-home-set")) {
                switch (depth) {
                    case 0 -> {
                        var homeSet = knownProp.addElement("cr:addressbook-home-set");
                        homeSet.addElement("href").addText("/carddav/");
                    }
                    case 1-> {
                        var homeSet = knownProp.addElement("cr:addressbook-home-set");
                        homeSet.addElement("href").addText("/carddav/" + userPrinciple + "/");
                    }
                    default -> unProp.addElement("cr:addressbook-home-set");
                }
            } else if (prop.normalName().matches("([0-9a-zA-Z-]+:)?supported-address-data")) {
                var sad = knownProp.addElement("cr:supported-address-data");
                sad.addAttribute("content-type", "text/vcard");
                sad.addAttribute("version", "3.0");
            } else if (prop.normalName().matches("([0-9a-zA-Z-]+:)?addressbook-description")) {
                switch (depth) {
                    case 2 ->{var displayname =  knownProp.addElement("cr:addressbook-description");
                        displayname.setText(BaseService.PROPS.get(user+LockService.LOCK_PREFIX+collectId).get("cr:addressbook-description")); }
                    default -> unProp.addElement("cr:addressbook-description");
                }
            } else if (prop.normalName().equals("supported-report-set")) {
                var reportSet = knownProp.addElement("supported-report-set");
                supportedReportSetGen(List.of("expand-property", "principal-search-property-set", "principal-property-search",
                        "sync-collection", "cr:addressbook-multiget", "addressbook-query"), reportSet);
            } else if (prop.normalName().equals("sync-token")) {
                switch (depth) {
                    case 2 -> {String token = carddavItemService.getSyncTokenValue(user, collectId);
                        if(Objects.nonNull(token)) {
                            var syncToken = knownProp.addElement("sync-token");
                            syncToken.addText("http://ao.space/sync/"+ token);
                        }}
                    default -> unProp.addElement("sync-token");
                }
            } else if (prop.normalName().matches("([0-9a-zA-Z-]+:)?getctag")) {
                switch (depth) {
                    case 2 -> {
                        String token = carddavItemService.getSyncTokenValue(user, collectId);
                        if(Objects.nonNull(token)) {
                            var getctag = knownProp.addElement("cs:getctag");
                            getctag.addText("\"" + token +"\"");
                        }
                    }
                    default -> unProp.addElement("cs:getctag");
                }
            } else if (prop.normalName().matches("([0-9a-zA-Z-]+:)?source")) {
                switch (depth) {
                    case 2 -> knownProp.addElement("cs:source").addElement("href");
                    default -> unProp.addElement("cs:source");
                }
            } else if(prop.normalName().contains("owner")) {
                switch (depth) {
                    case 0 -> knownProp.addElement("owner");
                    default -> knownProp.addElement("owner").addElement("href").setText("/carddav/"+userPrinciple+"/");
                }
            }else{
                var un = prop.normalName();
                if(un.toLowerCase().startsWith("n0:")) {
                    un = un.replace("n0", "ical");
                } else if (un.toLowerCase().startsWith("cal:")) {
                    un = un.replaceAll("(cal)|(CAL)","c");
                }else if(un.toLowerCase().startsWith("n1:")) {
                    un = un.replaceAll("(n1)|(N1)", "cs");
                }else if(un.toLowerCase().startsWith("card:")) {
                    un = un.replaceAll("(card)|(CARD)", "cr");
                }
                unProp.addElement(un);
            }
        }
        if(!knownProp.elements().isEmpty()) {
            var status = knownPropstat.addElement("status");
            status.setText(BaseService.HTTP_OK);
        }else{
            response.remove(knownPropstat);
        }
        if(!unProp.elements().isEmpty()) {
            var status = unPropstat.addElement("status");
            status.setText(BaseService.HTTP_NOT_FOUND);
        }else{
            response.remove(unPropstat);
        }
    }

    public void getSyncValue(List<Node> props, String user, String collectId, Element multistatus){
        Map<String, Item> existVcfs = new HashMap<>();
        Map<String, String> unexistVcfs = new HashMap<>();
        var nowItems = carddavItemService.getItemList(user,collectId);
        for(var item:nowItems) {
            existVcfs.put(item.getHref(), item);
        }
        for(var prop:props) {
            if(prop.normalName().equals("sync-token")) {
                var token = carddavItemService.getSyncTokenValue(user, collectId);
                var syncToken = multistatus.addElement("sync-token");
                syncToken.setText("http://ao.space/sync/"+ token);
                //需要比较两个客户端和服务端的token差异
                var child = prop.firstChild();
                if(Objects.nonNull(child) && ((TextNode) child).getWholeText().split("/").length> 1) {
                    var oldToken = ((TextNode) child).getWholeText().split("/");
                    var hrefsOld = carddavItemService.getTokenVcfs(oldToken[oldToken.length - 1], user, collectId);
                    var oldKeys = hrefsOld.keySet().toArray();
                    for(var oldHref:oldKeys) {
                        var flag = false;
                        for(var item:nowItems) {
                            if(item.getHref().equals(oldHref)) {
                                if(item.getEtag().equals(hrefsOld.get(oldHref))) {
                                    existVcfs.remove(oldHref);
                                }
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            unexistVcfs.put((String)oldHref, hrefsOld.get(oldHref));
                        }
                    }
                }
                props.remove(prop);
                break;
            }
        }
        for(var prop:props) {
            if(prop.normalName().equals("prop")) {
                existVcfs.keySet().forEach( key -> {
                    var response = multistatus.addElement("response");
                    var href = response.addElement("href");
                    href.setText("/carddav/"+user+"/"+collectId+"/"+key);
                    var propstat =response.addElement("propstat");
                    var p = propstat.addElement("prop");
                    for(var child:prop.childNodes()) {
                        if(child.normalName().matches("([0-9a-zA-Z-]+:)?getetag")) {
                            var geteta = p.addElement("getetag");
                            geteta.setText("\""+existVcfs.get(key).getEtag() + "\"");
                        }else if(child.normalName().matches("([0-9a-zA-Z-]+:)?address-data")) {
                            var addressData = p.addElement("cr:address-data");
                            addressData.setText(existVcfs.get(key).getText());
                        }
                    }
                    var status = propstat.addElement("status");
                    status.setText(BaseService.HTTP_OK);
                });
                unexistVcfs.keySet().forEach( key -> {
                    for(var child:prop.childNodes()) {
                        if(child.normalName().matches("([0-9a-zA-Z-]+:)?getetag")) {
                            var response = multistatus.addElement("response");
                            var href = response.addElement("href");
                            href.setText("/carddav/"+user+"/"+collectId+"/"+key);
                            var status = response.addElement("status");
                            status.setText(BaseService.HTTP_NOT_FOUND);
                        }
                    }
                });
            }
        }
    }

    public void getAddressMulti(List<Node> nodes, String user, String collectId, Element multistatus) {
        List<String> hrefs = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().matches("([0-9a-zA-Z-]+:)?prop")) {
                for(var child:node.childNodes()) {
                    contents.add(child.nodeName());
                }
            }
            if(node.normalName().matches("([0-9a-zA-Z-]+:)?href")) {
                hrefs.add(((org.jsoup.nodes.Element) node).ownText());
            }
        }
        LockService.initialLockService(user, collectId);
        var rLock = LockService.lockMap.get(user + LockService.LOCK_PREFIX + collectId).readLock();
        try{
            rLock.lock();
            for(var href:hrefs) {
                var response = multistatus.addElement("response");
                var h= response.addElement("href");
                h.setText(href);
                var pathArray = href.split("/");
                File file = new File(properties.addressbookLocation() + user + File.separator + collectId+ File.separator + pathArray[pathArray.length - 1]);
                if(file.exists()) {
                    var item = carddavItemService.getItem(file);
                    var propstat = response.addElement("propstat");
                    var prop = propstat.addElement("prop");
                    for(var content:contents) {
                        if (content.matches("([0-9a-zA-Z-]+:)?getcontenttype")) {
                            var contentType = prop.addElement("getcontenttype");
                            contentType.setText("text/vcard;charset=utf-8");
                        } else if (content.matches("([0-9a-zA-Z-]+:)?getetag")) {
                            var getag = prop.addElement("getetag");
                            getag.setText("\""+ item.getEtag()+"\"");
                        } else if (content.matches("([0-9a-zA-Z-]+:)?address-data")) {
                            var addressData = prop.addElement("cr:address-data");
                            addressData.setText(item.getText());
                        }
                    }
                    var status = propstat.addElement("status");
                    status.setText(BaseService.HTTP_OK);
                }else{
                    var status = response.addElement("status");
                    status.setText(BaseService.HTTP_NOT_FOUND);
                }
            }
        }finally {
            rLock.unlock();
        }
    }


    public Element privilegesGen(List<String> privileges, Element privilegeSet){
        for(var privilege:privileges) {
            var ele = privilegeSet.addElement("privilege");
            ele.addElement(privilege);
        }
        return privilegeSet;
    }

    public Element supportedReportSetGen(List<String> reportSets,Element reportSet) {
        for(var report:reportSets) {
            var supportedReport = reportSet.addElement("supported-report");
            var re = supportedReport.addElement("report");
            re.addElement(report);
        }
        return reportSet;
    }
}
