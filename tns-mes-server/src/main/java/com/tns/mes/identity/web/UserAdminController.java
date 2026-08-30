package com.tns.mes.identity.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.identity.domain.MesRole;
import com.tns.mes.identity.domain.MesUser;
import com.tns.mes.identity.domain.MesPermission;
import com.tns.mes.identity.service.UserAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/iam")
@Validated
@PreAuthorize("hasAuthority('USER_ADMIN')")
public class UserAdminController {
    private final UserAdminService service;
    public UserAdminController(UserAdminService service){this.service=service;}
    @GetMapping("/users") public ApiResponse<PageResponse<UserView>> users(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request){PageResponse<MesUser> values=service.users(page,size);return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(UserView::new).collect(Collectors.toList()),values.getTotal(),values.getPage(),values.getSize(),values.getTotalPages()),id(request));}
    @PostMapping("/users") public ApiResponse<UserView> createUser(@Valid @RequestBody UserAdminService.UserRequest body,HttpServletRequest request){return ApiResponse.ok(new UserView(service.create(body)),id(request));}
    @PutMapping("/users/{id}/status") public ApiResponse<UserView> changeStatus(@PathVariable Long id,@RequestParam String value,HttpServletRequest request){return ApiResponse.ok(new UserView(service.changeStatus(id,value)),id(request));}
    @DeleteMapping("/users/{id}") public ApiResponse<UserView> deleteUser(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new UserView(service.deleteUser(id)),id(request));}
    @PutMapping("/users/{id}/roles") public ApiResponse<UserView> assignRoles(@PathVariable Long id,@RequestBody List<String> roleCodes,HttpServletRequest request){return ApiResponse.ok(new UserView(service.assignRoles(id,roleCodes)),id(request));}
    @GetMapping("/roles") public ApiResponse<PageResponse<RoleView>> roles(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,HttpServletRequest request){PageResponse<MesRole> values=service.roles(page,size);return ApiResponse.ok(new PageResponse<>(values.getItems().stream().map(RoleView::new).collect(Collectors.toList()),values.getTotal(),values.getPage(),values.getSize(),values.getTotalPages()),id(request));}
    @PostMapping("/roles") public ApiResponse<RoleView> createRole(@Valid @RequestBody UserAdminService.RoleRequest body,HttpServletRequest request){return ApiResponse.ok(new RoleView(service.createRole(body)),id(request));}
    @PutMapping("/roles/{id}") public ApiResponse<RoleView> updateRole(@PathVariable Long id,@Valid @RequestBody UserAdminService.RoleRequest body,HttpServletRequest request){return ApiResponse.ok(new RoleView(service.updateRole(id,body)),id(request));}
    @DeleteMapping("/roles/{id}") public ApiResponse<RoleView> deleteRole(@PathVariable Long id,HttpServletRequest request){return ApiResponse.ok(new RoleView(service.deleteRole(id)),id(request));}
    @GetMapping("/permissions") public ApiResponse<List<PermissionView>> permissions(HttpServletRequest request){return ApiResponse.ok(service.permissions().stream().map(PermissionView::new).collect(Collectors.toList()),id(request));}
    private String id(HttpServletRequest request){Object value=request.getAttribute("requestId");return value==null?null:value.toString();}
    public static class UserView { private Long id; private String username; private String displayName; private String email; private String languageCode; private String status; private List<String> roles; public UserView(MesUser v){id=v.getId();username=v.getUsername();displayName=v.getDisplayName();email=v.getEmail();languageCode=v.getLanguageCode();status=v.getStatus();roles=v.getRoles().stream().map(MesRole::getCode).collect(Collectors.toList());} public Long getId(){return id;} public String getUsername(){return username;} public String getDisplayName(){return displayName;} public String getEmail(){return email;} public String getLanguageCode(){return languageCode;} public String getStatus(){return status;} public List<String> getRoles(){return roles;} }
    public static class RoleView { private Long id; private String code; private String nameZh; private String nameEn; private String nameAr; private String status; private List<String> permissions; public RoleView(MesRole v){id=v.getId();code=v.getCode();nameZh=v.getNameZh();nameEn=v.getNameEn();nameAr=v.getNameAr();status=v.getStatus();permissions=v.getPermissions().stream().map(p->p.getCode()).collect(Collectors.toList());} public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getStatus(){return status;} public List<String> getPermissions(){return permissions;} }
    public static class PermissionView { private Long id; private String code; private String nameZh; private String nameEn; private String nameAr; private String permissionType; private String groupCode; private Integer sortOrder; private String description; public PermissionView(MesPermission v){id=v.getId();code=v.getCode();nameZh=v.getNameZh();nameEn=v.getNameEn();nameAr=v.getNameAr();permissionType=v.getPermissionType();groupCode=v.getGroupCode();sortOrder=v.getSortOrder();description=v.getDescription();} public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getPermissionType(){return permissionType;} public String getGroupCode(){return groupCode;} public Integer getSortOrder(){return sortOrder;} public String getDescription(){return description;} }
}
