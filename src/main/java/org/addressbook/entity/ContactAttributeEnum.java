package org.addressbook.entity;

import lombok.Getter;

public enum ContactAttributeEnum {
    FULLNAME("^FN\\S*", "fullName"),
    NAME("^N$","name"),
    NOTE("^NOTE$", "note"),
    ORG("^ORG\\S*", "org"),
    UID("UID", "uid"),
    TITLE("TITLE", "title"),
    NICKNAME("NICKNAME", "nickName"),
    BDAY("BDAY", "bday"),
    REV("REV", "rev"),
    TEL("TEL\\S*", "telephones"),
    TEL_NUM("item\\d+\\.TEL", "telephones"),
    ADR("ADR\\S*", "addresses"),
    ADR_NUM("item\\d+\\.ADR\\S*", "addresses"),
    URLS("URL", "urls"),
    LABLES("LABEL\\S*", "labels"),
    EMAILS("EMAIL\\S*", "emails"),
    ABS("item\\d+\\.X-(?i)ABLABEL", "abs"),
    ABDATES("item\\d+\\.X-(?i)ABDATE", "abvalues"),
    ABRELOATORS("item\\d+\\.X-(?i)ABRELATEDNAMES", "abvalues"),
    IMAGE("PHOTO\\S+", "image"),
    VERSION("VERSION", "version"),
    SUBLOCALITY("item\\d+.X-APPLE-SUBLOCALITY", "subCity"),
    IMPP("IMPP\\S*", "impp"),
    ;

    @Getter
    private final String attrMatch;

    @Getter
    private final String conAttr;

    ContactAttributeEnum(String attrMatch, String conAttr) {
        this.attrMatch = attrMatch;
        this.conAttr = conAttr;
    }

    public static String valueFrom(String attributeName) {
        for(ContactAttributeEnum e:ContactAttributeEnum.values()){
            if (attributeName.matches(e.getAttrMatch())) {
                return e.getConAttr();
            }
        }
        return "";
    }

}
