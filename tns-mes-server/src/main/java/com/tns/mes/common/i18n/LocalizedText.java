package com.tns.mes.common.i18n;

public class LocalizedText {
    private String zh;
    private String en;
    private String ar;

    public LocalizedText() { }
    public LocalizedText(String zh, String en, String ar) {
        this.zh = zh;
        this.en = en;
        this.ar = ar;
    }
    public String getZh() { return zh; }
    public void setZh(String zh) { this.zh = zh; }
    public String getEn() { return en; }
    public void setEn(String en) { this.en = en; }
    public String getAr() { return ar; }
    public void setAr(String ar) { this.ar = ar; }
}

