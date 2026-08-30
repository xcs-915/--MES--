package com.tns.mes.common.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuditLoggingAspect {
    private final AuditLogRepository repository;
    private final javax.servlet.http.HttpServletRequest request;
    public AuditLoggingAspect(AuditLogRepository repository, HttpServletRequest request){this.repository=repository;this.request=request;}
    @AfterReturning(pointcut="@annotation(auditable)", returning="result")
    public void after(JoinPoint joinPoint, Auditable auditable, Object result){
        try { AuditLog log=new AuditLog(); log.setRequestId(MDC.get("requestId")==null?"unknown":MDC.get("requestId")); Authentication auth=SecurityContextHolder.getContext().getAuthentication(); log.setUserName(auth==null?"anonymous":auth.getName()); log.setAction(auditable.action()); log.setResource(auditable.resource()); log.setResourceId(findId(joinPoint.getArgs(), result)); log.setHttpMethod(request.getMethod()); log.setRequestPath(request.getRequestURI()); log.setResult("SUCCESS"); repository.save(log); } catch (RuntimeException ignored) { }
    }
    private String findId(Object[] args,Object result){for(Object arg:args)if(arg instanceof Long)return String.valueOf(arg);if(result!=null){try{java.lang.reflect.Method m=result.getClass().getMethod("getId");Object id=m.invoke(result);return id==null?null:String.valueOf(id);}catch(Exception ignored){}}return null;}
}

