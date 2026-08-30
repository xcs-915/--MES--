package com.tns.mes.identity.service;

import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.audit.Auditable;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.identity.domain.MesPermission;
import com.tns.mes.identity.domain.MesRole;
import com.tns.mes.identity.domain.MesUser;
import com.tns.mes.identity.repo.MesPermissionRepository;
import com.tns.mes.identity.repo.MesRoleRepository;
import com.tns.mes.identity.repo.MesUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
public class UserAdminService {
    private final MesUserRepository users;
    private final MesRoleRepository roles;
    private final MesPermissionRepository permissions;
    private final PasswordEncoder encoder;

    public UserAdminService(MesUserRepository users, MesRoleRepository roles, MesPermissionRepository permissions, PasswordEncoder encoder) {
        this.users = users; this.roles = roles; this.permissions = permissions; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<MesUser> users(int page, int size) {
        return PageResponse.from(users.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("username"))));
    }

    @Transactional
    @Auditable(action = "CREATE", resource = "IAM_USER")
    public MesUser create(UserRequest request) {
        if (users.findByUsername(request.getUsername()).isPresent()) throw new BizException(4091, "error.duplicate");
        MesUser user = new MesUser(); user.setUsername(request.getUsername().trim()); user.setPasswordHash(encoder.encode(request.getPassword())); user.setDisplayName(request.getDisplayName().trim()); user.setEmail(request.getEmail()); user.setLanguageCode(normalizeLanguage(request.getLanguageCode())); user.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus()); user.setRoles(resolveRoles(request.getRoleCodes())); return users.save(user);
    }

    @Transactional
    @Auditable(action = "UPDATE_STATUS", resource = "IAM_USER")
    public MesUser changeStatus(Long id, String status) { MesUser user=users.findById(id).orElseThrow(()->new BizException(4041,"error.not-found")); if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) throw new BizException(4003,"error.validation"); user.setStatus(status.toUpperCase()); return users.save(user); }

    @Transactional
    @Auditable(action = "DELETE", resource = "IAM_USER")
    public MesUser deleteUser(Long id) { return changeStatus(id, "INACTIVE"); }

    @Transactional
    @Auditable(action = "ASSIGN_ROLE", resource = "IAM_USER")
    public MesUser assignRoles(Long id, List<String> roleCodes) { MesUser user=users.findById(id).orElseThrow(()->new BizException(4041,"error.not-found")); user.setRoles(resolveRoles(roleCodes)); return users.save(user); }

    @Transactional(readOnly = true)
    public PageResponse<MesRole> roles(int page, int size) { return PageResponse.from(roles.findAll(PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),200),Sort.by("code")))); }

    @Transactional
    @Auditable(action = "CREATE", resource = "IAM_ROLE")
    public MesRole createRole(RoleRequest request) { if (roles.findByCode(request.getCode()).isPresent()) throw new BizException(4091,"error.duplicate"); MesRole role=new MesRole(); role.setCode(request.getCode().trim()); role.setNameZh(request.getNameZh().trim()); role.setNameEn(request.getNameEn()); role.setNameAr(request.getNameAr()); role.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus()); role.setPermissions(resolvePermissions(request.getPermissionCodes())); return roles.save(role); }

    @Transactional
    @Auditable(action = "UPDATE", resource = "IAM_ROLE")
    public MesRole updateRole(Long id, RoleRequest request) {
        MesRole role = roles.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        role.setNameZh(request.getNameZh().trim());
        role.setNameEn(request.getNameEn());
        role.setNameAr(request.getNameAr());
        role.setPermissions(resolvePermissions(request.getPermissionCodes()));
        return roles.save(role);
    }

    @Transactional
    @Auditable(action = "DELETE", resource = "IAM_ROLE")
    public MesRole deleteRole(Long id) {
        MesRole role = roles.findById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        role.setStatus("INACTIVE");
        return roles.save(role);
    }

    @Transactional(readOnly = true)
    public List<MesPermission> permissions() {
        return permissions.findAll().stream()
                .sorted(Comparator.comparing(MesPermission::getSortOrder).thenComparing(MesPermission::getCode))
                .collect(Collectors.toList());
    }

    private Set<MesRole> resolveRoles(List<String> codes) { Set<MesRole> result=new HashSet<>(); if(codes!=null) for(String code:codes) result.add(roles.findByCode(code).orElseThrow(()->new BizException(4043,"error.not-found"))); return result; }
    private Set<MesPermission> resolvePermissions(List<String> codes) { Set<MesPermission> result=new HashSet<>(); if(codes!=null) for(String code:codes) result.add(permissions.findByCode(code).orElseThrow(()->new BizException(4043,"error.not-found"))); return result; }
    private String normalizeLanguage(String language) { if(language==null||language.trim().isEmpty())return "zh-CN"; String value=language.trim(); if(value.toLowerCase().startsWith("zh"))return "zh-CN"; if(value.toLowerCase().startsWith("en"))return "en"; if(value.toLowerCase().startsWith("ar"))return "ar-TN"; throw new BizException(4003,"error.validation"); }

    public static class UserRequest { @javax.validation.constraints.NotBlank private String username; @javax.validation.constraints.NotBlank @javax.validation.constraints.Size(min=8,max=100) private String password; @javax.validation.constraints.NotBlank private String displayName; private String email; private String languageCode; private String status; private List<String> roleCodes; public String getUsername(){return username;} public void setUsername(String v){username=v;} public String getPassword(){return password;} public void setPassword(String v){password=v;} public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getLanguageCode(){return languageCode;} public void setLanguageCode(String v){languageCode=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public List<String> getRoleCodes(){return roleCodes;} public void setRoleCodes(List<String> v){roleCodes=v;} }
    public static class RoleRequest { @javax.validation.constraints.NotBlank private String code; @javax.validation.constraints.NotBlank private String nameZh; private String nameEn; private String nameAr; private String status; private List<String> permissionCodes; public String getCode(){return code;} public void setCode(String v){code=v;} public String getNameZh(){return nameZh;} public void setNameZh(String v){nameZh=v;} public String getNameEn(){return nameEn;} public void setNameEn(String v){nameEn=v;} public String getNameAr(){return nameAr;} public void setNameAr(String v){nameAr=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public List<String> getPermissionCodes(){return permissionCodes;} public void setPermissionCodes(List<String> v){permissionCodes=v;} }
}
