package org.addressbook.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    String etag;
    String uid;
    String href;
    String name;
    String text;
    String fullName;
    String version;
    String note;
    String org;
    String rev;
    String nickName;
    String bday;
    String title;
    List<String> urls;
    Map<String, String> impps;
}
