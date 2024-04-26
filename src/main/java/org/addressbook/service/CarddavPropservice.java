package org.addressbook.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.addressbook.entity.Item;
import org.jsoup.nodes.*;

import java.io.File;
import java.util.*;

@ApplicationScoped
public class CarddavPropservice {
    static final String ADDRESSBOOK_PATH = "/space/carddav/";

    @Inject
    CarddavItemService carddavItemService;


    public Node rootNodeGEn(String tag, String value) {
        var ele = new Element(tag);
        ele.appendText(value);
        return ele;
    }

    public Element deletVcfResponse(String user, String collectId, String vcfId, String status) {
        var element = new Element("response");
        element.appendChild(rootNodeGEn("href", ADDRESSBOOK_PATH + user +File.separator + collectId+ File.separator+vcfId));
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

    public List<String> checkVcf(String ifNonMatch, String ifMatch, String user, String collectId,  String vcfId, String body) {
        var itemNew = carddavItemService.readVcf(body,vcfId);
        if(Objects.isNull(itemNew)) {
            return List.of("400", "", "BadRequest");
        }
        var items = carddavItemService.getItemList(user, collectId);
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
                    var status = carddavItemService.writeVcf(user, collectId,body,vcfId);
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
                    var status = carddavItemService.writeVcf(user, collectId,body,vcfId);
                    var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                    return List.of(status, itemNew.getEtag(),response);
                }
            }else{
                if(Objects.nonNull(itemOld)) {
                    if(itemOld.getEtag().equals(ifNonMatch.replace("\"", ""))) {
                        return List.of("304", itemOld.getEtag(), "Not modfied.");
                    }else{
                        var status = carddavItemService.writeVcf(user, collectId,body,vcfId);
                        var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                        return List.of(status, itemNew.getEtag(),response);
                    }
                }else{
                    var status = carddavItemService.writeVcf(user, collectId,body,vcfId);
                    var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
                    return List.of(status, itemNew.getEtag(),response);
                }
            }
        }
        var status = carddavItemService.writeVcf(user,collectId,body,vcfId);
        var response = status.equals("201")?"{\"success\":1,\"fail\":0}":"Not allowed.";
        return List.of(status, itemNew.getEtag(),response);
    }

    public String deleteVcf(String user, String collectId, String ifMatch, String href) {
        return carddavItemService.deleteVcf(user, collectId,ifMatch, href);
    }
}
