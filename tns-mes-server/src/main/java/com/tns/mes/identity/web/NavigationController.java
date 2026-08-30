package com.tns.mes.identity.web;

import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.identity.domain.MenuItem;
import com.tns.mes.identity.repo.MenuItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Read-only navigation endpoint available to every authenticated user. */
@RestController
@RequestMapping("/api/v1/navigation")
public class NavigationController {
    private final MenuItemRepository menus;
    public NavigationController(MenuItemRepository menus){this.menus=menus;}
    @GetMapping
    public ApiResponse<List<NavigationView>> list(Authentication authentication, HttpServletRequest request) {
        List<String> authorities = authentication == null ? java.util.Collections.emptyList() : authentication.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
        List<NavigationView> result = menus.findAll().stream().filter(m -> "ACTIVE".equals(m.getStatus()))
                .filter(m -> m.getPermissionCode() == null || m.getPermissionCode().trim().isEmpty() || authorities.contains("USER_ADMIN") || authorities.contains(m.getPermissionCode()))
                .sorted(Comparator.comparing(MenuItem::getSortOrder).thenComparing(MenuItem::getCode)).map(NavigationView::new).collect(Collectors.toList());
        Object id=request.getAttribute("requestId"); return ApiResponse.ok(result,id == null ? null : id.toString());
    }
    public static class NavigationView { public Long id; public String code,nameZh,nameEn,nameAr,parentCode,path,icon,permissionCode; public Integer sortOrder; NavigationView(MenuItem m){id=m.getId();code=m.getCode();nameZh=m.getNameZh();nameEn=m.getNameEn();nameAr=m.getNameAr();parentCode=m.getParentCode();path=m.getPath();icon=m.getIcon();permissionCode=m.getPermissionCode();sortOrder=m.getSortOrder();} public Long getId(){return id;} public String getCode(){return code;} public String getNameZh(){return nameZh;} public String getNameEn(){return nameEn;} public String getNameAr(){return nameAr;} public String getParentCode(){return parentCode;} public String getPath(){return path;} public String getIcon(){return icon;} public String getPermissionCode(){return permissionCode;} public Integer getSortOrder(){return sortOrder;} }
}
