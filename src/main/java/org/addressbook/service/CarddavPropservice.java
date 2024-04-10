package org.addressbook.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.addressbook.entity.Item;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.service.ApplicationProperties;

import java.io.File;
import java.util.*;
import io.netty.util.internal.StringUtil;

@ApplicationScoped
public class CarddavPropservice {
    public static Map<String, Element> closeNodes= new HashMap<String, Element>();

    static final String ADDRESSBOOK_PATH = "/space/carddav/";

    static final String HTTP_OK = "HTTP/1.1 200 OK";

    static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";

    @Inject
    CarddavItemService carddavItemService;

    @Inject
    ApplicationProperties properties;

    @PostConstruct
    void init() {
        var nodes = "<resourcetype />\n" +
                "    <displayname />\n" + "<group-membership />\n"+
                "    <C:calendar-description />\n" +
                "    <ICAL:calendar-color />\n" +
                "    <cs:source />\n"+
                "    <cs:getctag />\n"+
                "    <sync-token />\n"+
                "    <getetag />\n"+
                "    <C:supported-calendar-component-set />\n"+
                "    <CR:supported-address-data />\n" +
                "    <principal-search-property-set />\n" +
                "    <expand-property />\n" +
                "    <href />" +
                "    <c:calendar-timezone />\n" +
                "    <principal-property-search />\n"+
                "    <sync-collection />\n"+
                "    <CR:addressbook-multiget />\n" +
                "    <CR:addressbook-query />"+
                "    <CR:max-resource-size />\n" +
                "    <CR:address-data-type content-type=\"text/vcard\" version=\"3.0\"/>" +
                "    <CR:addressbook-description />\n" +"<CR:addressbook />\n" +
                "    <CR:addressbook-home-set />\n" + "<CAL:calendar-user-address-set />"+
                "    <current-user-principal />\n" + "<collection />\n" + "<principal />\n" +"<href />\n" + "<status />\n" +
                " <read />\n" + "<write />\n" + "<all />\n" + "<write-content />\n" + "<write-properties />\n" +
                "";
        var doc = Jsoup.parse(nodes);
        for(var node: doc.getAllElements()) {
            if(!closeNodes.containsKey(node.normalName())) {
                closeNodes.put(node.tagName(), node);
            }
        }
    }

    public Document documentGen(List<Node> nodes) {
        Document document = new Document("DAV", null);
        document.appendChild(new Comment("?xml version='1.0' encoding='UTF-8' ?"));
        document.outputSettings().prettyPrint(false);
        Attributes attributes = new Attributes();
        attributes.add("xmlns", "DAV:");
        attributes.add("xmlns:cr", "urn:ietf:params:xml:ns:carddav");
        attributes.add("xmlns:c","urn:ietf:params:xml:ns:caldav");
        attributes.add("xmlns:cs","http://calendarserver.org/ns/");
        attributes.add("xmlns:ical","http://apple.com/ns/ical/");
        var multistatus = new Element(org.jsoup.parser.Tag.valueOf("multistatus"), null, attributes);
        for(var node:nodes) {
            multistatus.appendChild(node);
        }
        document.appendChild(multistatus);
        return document;
    }

    public List<Element> getRootProp(List<Node> props, String aoId, int depth, String vcfId){
        var knowProp = new Element("prop");
        var unknownProp = new Element("prop");
        List<Element> responses = new ArrayList<>();
        for (var prop : props) {
            if (prop.normalName().equalsIgnoreCase("resourcetype")) {
                if (depth <= 1) {
                    var resource = new Element("resourcetype");
                    resource.appendChild(closeNodes.get("collection").clone());
                    if (depth == 0) {
                        resource.appendChild(closeNodes.get("principal").clone());
                    } else if (depth == 1) {
                        resource.appendChild(closeNodes.get("cr:addressbook").clone());
                    }
                    knowProp.appendChild(resource);
                } else {
                    knowProp.appendChild(closeNodes.get("resourcetype").clone());
                }
            } else if (prop.normalName().equalsIgnoreCase("current-user-principal")) {
                knowProp.appendChild(childNodeGenerate("current-user-principal", Map.of("href", ADDRESSBOOK_PATH)));
            } else if (prop.normalName().equalsIgnoreCase("displayname")) {
                knowProp.appendChild(rootNodeGEn("displayname", "addressbook_" + aoId));
            } else if (prop.normalName().contains("addressbook-description")) {
                if (depth == 0) {
                    unknownProp.appendChild(closeNodes.get("cr:addressbook-description").clone());
                } else {
                    knowProp.appendChild(rootNodeGEn("cr:addressbook-description", "eulixos"));
                }
            } else if (prop.normalName().contains("addressbook-home-set")) {
                if (depth == 0) {
                    knowProp.appendChild(childNodeGenerate("cr:addressbook-home-set", Map.of("href", ADDRESSBOOK_PATH)));
                } else {
                    unknownProp.appendChild(closeNodes.get("cr:addressbook-home-set").clone());
                }
            } else if (prop.normalName().contains("calendar-user-address-set")) {
                unknownProp.appendChild(closeNodes.get("cal:calendar-user-address-set").clone());
            }else if(prop.normalName().contains("calendar-timezone")) {
                unknownProp.appendChild(closeNodes.get("c:calendar-timezone").clone());
            }else if(prop.normalName().contains("supported-address-data")) {
                var supportedAddressData = new Element("cr:supported-address-data");
                supportedAddressData.appendChild(closeNodes.get("cr:address-data-type").clone());
                knowProp.appendChild(supportedAddressData);
                //unknownProp.appendChild(closeNodes.get("cr:supported-address-data").clone());
            }else if(prop.normalName().contains("current-user-privilege-set")) {
                knowProp.appendChild(getCurrentUserPriviligeSet(List.of("read","write","all","write-content","write-properties")));
            }else if(prop.normalName().contains("owner")) {
                knowProp.appendChild(childNodeGenerate("owner", Map.of("href", ADDRESSBOOK_PATH+aoId+"/")));
            }else if(prop.normalName().contains("group-membership")) {
                unknownProp.appendChild(closeNodes.get("group-membership").clone());
            }else if(prop.normalName().contains("calendar-color")) {
                unknownProp.appendChild(closeNodes.get("ical:calendar-color").clone());
            }else if(prop.normalName().contains("calendar-description")) {
                unknownProp.appendChild(closeNodes.get("c:calendar-description").clone());
            }else if(prop.normalName().equalsIgnoreCase("n1:source")) {
                unknownProp.appendChild(closeNodes.get("cs:source").clone());
            }else if(prop.normalName().contains("supported-calendar-component-set")) {
                unknownProp.appendChild(closeNodes.get("c:supported-calendar-component-set").clone());
            }else if(prop.normalName().contains("max-resource-size")) {
                unknownProp.appendChild(closeNodes.get("cr:max-resource-size").clone());
            }else if(prop.normalName().equalsIgnoreCase("supported-report-set")) {
                if(depth == 1) {
                    knowProp.appendChild(getSupportedReportSet());
                }
            } else if (prop.normalName().contains("getctag") ) {
                if(depth == 0) {
                    unknownProp.appendChild(closeNodes.get("cs:getctag").clone());
                }else if(depth == 1){
                    String token = carddavItemService.getSyncTokenValue(aoId);
                    if(Objects.nonNull(token)) {
                        knowProp.appendChild(rootNodeGEn("cs:getctag", "\""+token+"\""));
                    }
                }else{
                    unknownProp.appendChild(closeNodes.get("cs:getctag").clone());
                }
            } else if (prop.normalName().equals("getetag") ) {
                if(depth == 0) {
                    unknownProp.appendChild(closeNodes.get("getetag").clone());
                }else if(depth == 1) {
                    String token = carddavItemService.getSyncTokenValue(aoId);
                    knowProp.appendChild(rootNodeGEn("getetag", "\""+token+"\""));
                } else{
                    String token = "0";
                    for(var key:carddavItemService.getSyncToken(aoId).keySet()) {
                        if(!StringUtil.isNullOrEmpty(key)) {
                            token = key;
                            break;
                        }
                    }
                    var tokenMap  = carddavItemService.getSyncToken(aoId).get(token);
                    if(Objects.nonNull(vcfId)) {
                        knowProp.appendChild(rootNodeGEn("getetag", "\""+tokenMap.get(vcfId) + "\""));
                    }else{
                        for(var vcf:tokenMap.keySet()) {
                            responses.addAll(getRootProp(props, aoId, depth, vcf));
                        }
                    }
                }
            } else if (prop.normalName().equals("sync-token")) {
                if(depth == 1) {
                    String token = carddavItemService.getSyncTokenValue(aoId);
                    if(Objects.nonNull(token)) {
                        knowProp.appendChild(rootNodeGEn("sync-token", "http://ao.space/sync/"+ token));
                    }
                }else{
                    unknownProp.appendChild(closeNodes.get("sync-token").clone());
                }
            }
        }
        List<Node> list1 = new ArrayList<>();
        if(depth == 0) {
            list1.add(rootNodeGEn("href",ADDRESSBOOK_PATH ));
        }else if(depth == 1){
            list1.add(rootNodeGEn("href",ADDRESSBOOK_PATH + aoId+"/"));
        }else if(depth > 1 && Objects.nonNull(vcfId)) {
            list1.add(rootNodeGEn("href",ADDRESSBOOK_PATH + aoId+"/" + vcfId));
        }
        if(knowProp.childNodeSize() > 0) {
            list1.add(getKnownProp(knowProp));
        }
        if(unknownProp.childNodeSize() > 0) {
            list1.add(getUnknownProp(unknownProp));
        }
        if(list1.size() > 0 && !(Objects.isNull(vcfId) && depth > 1)) {
            responses.add(childNodeGen("response", list1));
        }
        return responses;
    }

    public Element getKnownProp(Element knownProp) {
        if(knownProp.childNodeSize() > 0) {
            var kPropStat = new Element("propstat");
            kPropStat.appendChild(knownProp);
            kPropStat.appendChild(rootNodeGEn("status", HTTP_OK));
            return kPropStat;
        }else {
            return null;
        }
    }

    public Element getUnknownProp(Element unknownProp) {
        if(unknownProp.childNodeSize() > 0) {
            var ukPropStat = new Element("proppstat");
            ukPropStat.appendChild(unknownProp);
            ukPropStat.appendChild(rootNodeGEn("status", HTTP_NOT_FOUND));
            return ukPropStat;
        }else {
            return null;
        }
    }
    public Element childNodeGen(String tag, List<Node> nodes) {
        var element = new Element(tag);
        for(var node:nodes) {
            element.appendChild(node);
        }
        return element;
    }

    public Element childNodeGenerate(String tag, Map<String, String> childTags) {
        var element = new Element(tag);
        for(var key:childTags.keySet()){
            element.appendChild(rootNodeGEn(key, childTags.get(key)));
        }
        return element;
    }

    public Node rootNodeGEn(String tag, String value) {
        var ele = new Element(tag);
        ele.appendText(value);
        return ele;
    }

    public Element getCurrentUserPriviligeSet(List<String> elements) {
        var currentUserPrivilegeSet = new Element("current-user-privilege-set");
        for(var value:elements) {
            var ele = new Element("privilege");
            ele.appendChild(closeNodes.get(value).clone());
            currentUserPrivilegeSet.appendChild(ele);
        }
        return currentUserPrivilegeSet;
    }

    public Element getSupportedReportSet() {
        var supportedReportSet = new Element("supported-report-set");
        List<String> props = List.of("expand-property", "principal-search-property-set",
                "principal-property-search", "sync-collection","cr:addressbook-multiget","cr:addressbook-query");
        for(var prop:props) {
            var supportedReport = new Element("supported-report");
            var report = new Element("report");
            report.appendChild(closeNodes.get(prop).clone());
            supportedReport.appendChild(report);
            supportedReportSet.appendChild(supportedReport);
        }
        return supportedReportSet;
    }

    public List<Node> getSyncValue(List<Node> props, String aoId) {
        List<Node> elements = new ArrayList<>();
        Map<String, Item> existVcfs = new HashMap<>();
        Map<String, String> unexistVcfs = new HashMap<>();
        var nowItems = carddavItemService.getItemList(aoId);
        for(var item:nowItems) {
            existVcfs.put(item.getHref(), item);
        }
        for(var prop:props) {
            if(prop.normalName().equals("sync-token")) {
                var tokenMap = carddavItemService.getSyncToken(aoId);
                for(var key:tokenMap.keySet()) {
                    if(!StringUtil.isNullOrEmpty(key)) {
                        elements.add(rootNodeGEn("sync-token", "http://ao.space/sync/" + key));
                        break;
                    }
                }
                //需要比较两个客户端和服务端的token差异
                var child = prop.firstChild();
                if(Objects.nonNull(child) && ((TextNode) child).getWholeText().split("/").length> 1) {
                    var oldToken = ((TextNode) child).getWholeText().split("/");
                    var hrefsOld = carddavItemService.getTokenVcfs(oldToken[oldToken.length - 1], aoId);
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
            }
        }
        for(var prop:props) {
            if(prop.normalName().equals("prop")) {
                existVcfs.keySet().forEach( key -> {
                    var reponse = new Element("response");
                    reponse.appendChild(rootNodeGEn("href", ADDRESSBOOK_PATH+aoId+"/"+key));
                    var propstat = new Element("propstat");
                    var p = new Element("prop");
                    for(var child:prop.childNodes()) {
                        if(child.normalName().matches("([0-9a-zA-Z-]+:)?getetag")) {
                            p.appendChild(rootNodeGEn("getetag", "\""+existVcfs.get(key).getEtag() + "\""));
                        }else if(child.normalName().matches("([0-9a-zA-Z-]+:)?address-data")) {
                            p.appendChild(rootNodeGEn("cr:address-data", existVcfs.get(key).getText()));
                        }
                    }
                    propstat.appendChild(p);
                    propstat.appendChild(rootNodeGEn("status", HTTP_OK));
                    reponse.appendChild(propstat);
                    elements.add(reponse);
                });
                unexistVcfs.keySet().forEach( key -> {
                    for(var child:prop.childNodes()) {
                        if(child.normalName().matches("([0-9a-zA-Z-]+:)?getetag")) {
                            var reponse = new Element("response");
                            reponse.appendChild(rootNodeGEn("href", ADDRESSBOOK_PATH+aoId+"/"+key));
                            reponse.appendChild(rootNodeGEn("status", HTTP_NOT_FOUND));
                            elements.add(reponse);
                        }
                    }
                });
            }
        }
        return elements;
    }

    public List<Node> getAddressMulti(List<Node> nodes, String aoId) {
        List<Node> elements = new ArrayList<>();
        List<String> hrefs = new ArrayList<>();
        List<String> contents = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().matches("([0-9a-zA-Z-]+:)?prop")) {
                for(var child:node.childNodes()) {
                    contents.add(child.nodeName());
                }
            }
            if(node.normalName().matches("([0-9a-zA-Z-]+:)?href")) {
                hrefs.add(((Element) node).ownText());
            }
        }
        for(var href:hrefs) {
            var response = new Element("response");
            response.appendChild(rootNodeGEn("href", href));
            var pathArray = href.split("/");
            File file = new File(properties.addressbookLocation() + aoId + "/" + pathArray[pathArray.length - 1]);
            if(file.exists()) {
                var item = carddavItemService.getItem(file);
                var propstat = new Element("propstat");
                var prop = new Element("prop");
                for(var content:contents) {
                    if (content.matches("([0-9a-zA-Z-]+:)?getcontenttype")) {
                        prop.appendChild(rootNodeGEn("getcontenttype", "text/vcard;charset=utf-8"));
                    } else if (content.matches("([0-9a-zA-Z-]+:)?getetag")) {
                        prop.appendChild(rootNodeGEn("getetag", "\""+item.getEtag()+"\""));
                    } else if (content.matches("([0-9a-zA-Z-]+:)?address-data")) {
                        prop.appendChild(rootNodeGEn("cr:address-data",item.getText()));
                    }
                }
                propstat.appendChild(prop);
                propstat.appendChild(rootNodeGEn("status", HTTP_OK));
                response.appendChild(propstat);
            }else{
                response.appendChild(rootNodeGEn("status", HTTP_NOT_FOUND));
            }
            elements.add(response);
        }
        return elements;
    }

    public Element deletVcfResponse(String aoId, String vcfId, String status) {
        var element = new Element("response");
        element.appendChild(rootNodeGEn("href", ADDRESSBOOK_PATH + aoId + "/"+vcfId));
        element.appendChild(rootNodeGEn("status", status));
        return element;
    }

    public List<Node> getPropFindProps(List<Node> nodes, String name) {
        List<Node> deepNodes = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().equalsIgnoreCase(name) ) {
                for(var child:node.childNodes()) {
                    if(child.normalName().equals("prop")) {
                        for(var ch:child.childNodes()) {
                            deepNodes.add(ch);
                        }
                        return deepNodes;
                    }
                }
            } else {
                deepNodes = getPropFindProps(node.childNodes(), name);
                if(deepNodes.size() > 0) {
                    break;
                }
            }
        }
        return deepNodes;
    }

    public List<Node> getReportProps(List<Node> nodes, String name) {
        List<Node> deepNodes = new ArrayList<>();
        for(var node:nodes) {
            if(node.normalName().equalsIgnoreCase(name)) {
                for(var ch:node.childNodes()) {
                    deepNodes.add(ch);
                }
                return deepNodes;
            } else {
                deepNodes = getReportProps(node.childNodes(), name);
                if(deepNodes.size() > 0) {
                    break;
                }
            }
        }
        return deepNodes;
    }

    public List<String> checkVcf(String ifNonMatch, String ifMatch, String aoId, String vcfId, String body) {
        var itemNew = carddavItemService.readVcf(body,vcfId);
        if(Objects.isNull(itemNew)) {
            return List.of("400", "", "BadRequest");
        }
        var items = carddavItemService.getItemList(aoId);
        Item itemOld = null;
        for(var item:Objects.requireNonNull(items)) {
            if(item.getUid().equals(itemNew.getUid()) && !item.getHref().equals(itemNew.getHref())) {
                return List.of("409", "", "<?xml version='1.0' encoding='utf-8'?>\n" +
                        "<error xmlns=\"DAV:\" xmlns:CR=\"urn:ietf:params:xml:ns:carddav\">\n" +
                        "    <CR:no-uid-conflict />\n" +
                        "</error>");
            } else if (item.getHref().equals(itemNew.getHref())) {
                itemOld = item;
                System.out.println(itemOld.getEtag());
            }
        }
        if(Objects.nonNull(ifMatch)) {
            if(!Objects.nonNull(itemOld)) {
                return List.of("412", "", "Precondition failed.");
            } else {
                if(!itemOld.getEtag().equals(ifMatch.replace("\"", ""))) {
                    return List.of("412",itemOld.getEtag(),"Precondition failed.");
                }else{
                    //写vcf
                    var status = carddavItemService.writeVcf(aoId,body,vcfId);
                    var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                    return List.of(status, itemNew.getEtag(),response);
                }
            }
        }
        if(Objects.nonNull(ifNonMatch)) {
            if(ifNonMatch.equals("*")) {
                if(Objects.nonNull(itemOld)) {
                    return List.of("412",itemOld.getEtag());
                }else{
                    var status = carddavItemService.writeVcf(aoId,body,vcfId);
                    var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                    return List.of(status, itemNew.getEtag(),response);
                }
            }else{
                if(Objects.nonNull(itemOld)) {
                    if(itemOld.getEtag().equals(ifNonMatch.replace("\"", ""))) {
                        return List.of("304", itemOld.getEtag(), "Not modfied.");
                    }else{
                        var status = carddavItemService.writeVcf(aoId,body,vcfId);
                        var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                        return List.of(status, itemNew.getEtag(),response);
                    }
                }else{
                    var status = carddavItemService.writeVcf(aoId,body,vcfId);
                    var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                    return List.of(status, itemNew.getEtag(),response);
                }
            }
        }
        var status = carddavItemService.writeVcf(aoId,body,vcfId);
        var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
        return List.of(status, itemNew.getEtag(),response);
    }

    public String deleteVcf(String aoId, String ifMatch, String href) {
        return carddavItemService.deleteVcf(aoId,ifMatch, href);
    }
}
