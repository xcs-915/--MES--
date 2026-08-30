package com.tns.mes.identity.domain;

import com.tns.mes.common.domain.AuditedEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sys_data_dictionary")
public class DataDictionary extends AuditedEntity {
    @Column(name = "dict_type", nullable = false, length = 80) private String dictType;
    @Column(name = "dict_code", nullable = false, length = 100) private String dictCode;
    @Column(name = "label_zh", nullable = false, length = 200) private String labelZh;
    @Column(name = "label_en", length = 200) private String labelEn;
    @Column(name = "label_ar", length = 200) private String labelAr;
    @Column(name = "dict_value", nullable = false, length = 200) private String dictValue;
    @Column(nullable = false) private Integer sortOrder = 0;
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    public String getDictType(){return dictType;} public void setDictType(String v){dictType=v;}
    public String getDictCode(){return dictCode;} public void setDictCode(String v){dictCode=v;}
    public String getLabelZh(){return labelZh;} public void setLabelZh(String v){labelZh=v;}
    public String getLabelEn(){return labelEn;} public void setLabelEn(String v){labelEn=v;}
    public String getLabelAr(){return labelAr;} public void setLabelAr(String v){labelAr=v;}
    public String getDictValue(){return dictValue;} public void setDictValue(String v){dictValue=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
