package com.tns.mes.basic.web;

import com.tns.mes.basic.domain.MasterData;
import com.tns.mes.basic.domain.MasterDataType;
import com.tns.mes.basic.service.MasterDataService;
import com.tns.mes.common.api.ApiResponse;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.i18n.LocalizedText;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/master-data")
@Validated
public class MasterDataController {
    private final MasterDataService service;

    public MasterDataController(MasterDataService service) { this.service = service; }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('BASIC_DATA_READ')")
    public ApiResponse<List<TypeView>> types(HttpServletRequest request) {
        List<TypeView> result = Arrays.stream(MasterDataType.values()).map(t -> new TypeView(t.name(), t.getCode(),
                typeNames(t))).collect(Collectors.toList());
        return ApiResponse.ok(result, requestId(request));
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('BASIC_DATA_READ')")
    public ApiResponse<PageResponse<MasterData>> page(@PathVariable String type,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       HttpServletRequest request) {
        return ApiResponse.ok(service.page(type, keyword, page, size), requestId(request));
    }

    @GetMapping("/{type}/{id}")
    @PreAuthorize("hasAuthority('BASIC_DATA_READ')")
    public ApiResponse<MasterData> get(@PathVariable String type, @PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(service.get(type, id), requestId(request));
    }

    @PostMapping("/{type}")
    @PreAuthorize("hasAuthority('BASIC_DATA_WRITE')")
    public ApiResponse<MasterData> create(@PathVariable String type,
                                           @Valid @RequestBody MasterDataService.MasterDataRequest body,
                                           HttpServletRequest request) {
        return ApiResponse.ok(service.create(type, body), requestId(request));
    }

    @PutMapping("/{type}/{id}")
    @PreAuthorize("hasAuthority('BASIC_DATA_WRITE')")
    public ApiResponse<MasterData> update(@PathVariable String type, @PathVariable Long id,
                                          @Valid @RequestBody MasterDataService.MasterDataRequest body,
                                          HttpServletRequest request) {
        return ApiResponse.ok(service.update(type, id, body), requestId(request));
    }

    @DeleteMapping("/{type}/{id}")
    @PreAuthorize("hasAuthority('BASIC_DATA_WRITE')")
    public ApiResponse<Void> delete(@PathVariable String type, @PathVariable Long id, HttpServletRequest request) {
        service.delete(type, id);
        return ApiResponse.ok(null, requestId(request));
    }

    private LocalizedText typeNames(MasterDataType type) {
        switch (type) {
            case ENTERPRISE: return new LocalizedText("企业", "Enterprise", "المؤسسة");
            case FACTORY: return new LocalizedText("工厂", "Factory", "المصنع");
            case WORKSHOP: return new LocalizedText("车间", "Workshop", "الورشة");
            case DEPARTMENT: return new LocalizedText("部门", "Department", "القسم");
            case WAREHOUSE: return new LocalizedText("仓库", "Warehouse", "المستودع");
            case WORK_CENTER: return new LocalizedText("工作中心", "Work center", "مركز العمل");
            case PRODUCTION_LINE: return new LocalizedText("产线", "Production line", "خط الإنتاج");
            case WORKSTATION: return new LocalizedText("工位", "Workstation", "محطة العمل");
            case PERSON: return new LocalizedText("人员", "Person", "الشخص");
            case POSITION: return new LocalizedText("岗位", "Position", "الوظيفة");
            case CUSTOMER: return new LocalizedText("客户", "Customer", "العميل");
            case SUPPLIER: return new LocalizedText("供应商", "Supplier", "المورد");
            case MANUFACTURER: return new LocalizedText("制造商", "Manufacturer", "الشركة المصنعة");
            default: return new LocalizedText(type.name(), type.name(), type.name());
        }
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute("requestId");
        return value == null ? null : value.toString();
    }

    public static class TypeView {
        private String key;
        private String code;
        private LocalizedText name;
        public TypeView(String key, String code, LocalizedText name) { this.key = key; this.code = code; this.name = name; }
        public String getKey() { return key; }
        public String getCode() { return code; }
        public LocalizedText getName() { return name; }
    }
}

