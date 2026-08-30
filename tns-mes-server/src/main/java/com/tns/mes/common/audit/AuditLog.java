package com.tns.mes.common.audit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name="sys_audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="request_id",nullable=false,length=64) private String requestId;
    @Column(name="user_name",length=64) private String userName;
    @Column(nullable=false,length=30) private String action;
    @Column(nullable=false,length=100) private String resource;
    @Column(name="resource_id",length=64) private String resourceId;
    @Column(name="http_method",length=10) private String httpMethod;
    @Column(name="request_path",length=500) private String requestPath;
    @Column(nullable=false,length=20) private String result;
    @Column(length=4000) private String detail;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    public Long getId(){return id;} public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=v;} public String getUserName(){return userName;} public void setUserName(String v){userName=v;} public String getAction(){return action;} public void setAction(String v){action=v;} public String getResource(){return resource;} public void setResource(String v){resource=v;} public String getResourceId(){return resourceId;} public void setResourceId(String v){resourceId=v;} public String getHttpMethod(){return httpMethod;} public void setHttpMethod(String v){httpMethod=v;} public String getRequestPath(){return requestPath;} public void setRequestPath(String v){requestPath=v;} public String getResult(){return result;} public void setResult(String v){result=v;} public String getDetail(){return detail;} public void setDetail(String v){detail=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}

