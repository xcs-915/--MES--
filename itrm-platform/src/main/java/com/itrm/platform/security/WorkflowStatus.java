package com.itrm.platform.security;

import com.itrm.platform.common.BaseEntity;
import javax.persistence.*;

@Entity
@Table(name = "itrm_workflow_status")
public class WorkflowStatus extends BaseEntity {
    @Column(nullable=false,unique=true,length=40) private String code;
    @Column(nullable=false,length=80) private String name;
    @Column(nullable=false,length=20) private String color = "#64748b";
    @Column(name="sort_order",nullable=false) private Integer sortOrder = 0;
    @Column(nullable=false) private Boolean terminal = false;
    @Column(name="built_in",nullable=false) private Boolean builtIn = false;
    @Column(nullable=false,length=20) private String status = "ACTIVE";
    public String getCode(){return code;} public void setCode(String v){code=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getColor(){return color;} public void setColor(String v){color=v;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    public Boolean getTerminal(){return terminal;} public void setTerminal(Boolean v){terminal=v;}
    public Boolean getBuiltIn(){return builtIn;} public void setBuiltIn(Boolean v){builtIn=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
