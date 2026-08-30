package com.tns.mes.identity;

import com.tns.mes.identity.domain.MesPermission;
import com.tns.mes.identity.domain.MesRole;
import com.tns.mes.identity.domain.MesUser;
import com.tns.mes.identity.repo.MesPermissionRepository;
import com.tns.mes.identity.repo.MesRoleRepository;
import com.tns.mes.identity.repo.MesUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.HashSet;

@Configuration
public class DataInitializer {
    @Value("${mes.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;
    @Value("${mes.bootstrap.admin-password:}")
    private String bootstrapAdminPassword;

    @Bean
    public CommandLineRunner seedIdentity(MesPermissionRepository permissions, MesRoleRepository roles,
                                          MesUserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!bootstrapEnabled || bootstrapAdminPassword == null || bootstrapAdminPassword.length() < 8) {
                return;
            }
            PermissionDefinition[] definitions = definitions();
            HashSet<MesPermission> all = new HashSet<>();
            for (PermissionDefinition definition : definitions) {
                MesPermission permission = permissions.findByCode(definition.code).orElseGet(MesPermission::new);
                permission.setCode(definition.code);
                permission.setNameZh(definition.nameZh);
                permission.setNameEn(definition.nameEn);
                permission.setNameAr(definition.nameAr);
                permission.setPermissionType(definition.type);
                permission.setGroupCode(definition.group);
                permission.setSortOrder(definition.sortOrder);
                permission = permissions.save(permission);
                all.add(permission);
            }
            MesRole adminRole = roles.findByCode("MES_ADMIN").orElseGet(() -> {
                MesRole role = new MesRole();
                role.setCode("MES_ADMIN"); role.setNameZh("MES管理员"); role.setNameEn("MES Administrator"); role.setNameAr("مسؤول النظام");
                return role;
            });
            adminRole.setPermissions(all);
            adminRole = roles.save(adminRole);
            if (!users.findByUsername("admin").isPresent()) {
                MesUser admin = new MesUser();
                admin.setUsername("admin"); admin.setPasswordHash(encoder.encode(bootstrapAdminPassword));
                admin.setDisplayName("系统管理员"); admin.setLanguageCode("zh-CN"); admin.setRoles(new HashSet<>(Arrays.asList(adminRole)));
                users.save(admin);
            }
        };
    }

    private PermissionDefinition[] definitions() {
        return new PermissionDefinition[]{
                p("BASIC_DATA_READ","基础资料查看","View master data","عرض البيانات الأساسية","API","MASTER_DATA",10),
                p("BASIC_DATA_WRITE","基础资料维护","Maintain master data","إدارة البيانات الأساسية","API","MASTER_DATA",20),
                p("ENGINEERING_READ","工程资料查看","View engineering data","عرض البيانات الهندسية","API","ENGINEERING",30),
                p("ENGINEERING_WRITE","工程资料维护","Maintain engineering data","إدارة البيانات الهندسية","API","ENGINEERING",40),
                p("WORK_ORDER_READ","工单查看","View work orders","عرض أوامر العمل","API","PRODUCTION",50),
                p("WORK_ORDER_WRITE","工单执行","Execute work orders","تنفيذ أوامر العمل","API","PRODUCTION",60),
                p("INTEGRATION_READ","集成查看","View integrations","عرض التكامل","API","INTEGRATION",70),
                p("INTEGRATION_WRITE","集成维护","Maintain integrations","إدارة التكامل","API","INTEGRATION",80),
                p("QUALITY_READ","质量数据查看","View quality data","عرض بيانات الجودة","API","QUALITY",85),
                p("QUALITY_WRITE","质量数据维护","Maintain quality data","إدارة بيانات الجودة","API","QUALITY",88),
                p("USER_ADMIN","用户角色管理","Manage users and roles","إدارة المستخدمين والأدوار","API","SECURITY",90),
                p("PAGE_OVERVIEW","访问运营总览","Access overview","الوصول إلى النظرة العامة","PAGE","OVERVIEW",100),
                p("PAGE_MASTER_DATA","访问基础资料","Access master data","الوصول إلى البيانات الأساسية","PAGE","MASTER_DATA",110),
                p("PAGE_IAM","访问权限管理","Access security administration","الوصول إلى إدارة الصلاحيات","PAGE","SECURITY",120),
                p("PAGE_PRODUCT","访问产品主数据","Access products","الوصول إلى المنتجات","PAGE","ENGINEERING",130),
                p("PAGE_BOM","访问BOM","Access BOM","الوصول إلى قوائم المواد","PAGE","ENGINEERING",140),
                p("PAGE_ROUTE","访问工艺路线","Access routes","الوصول إلى المسارات","PAGE","ENGINEERING",150),
                p("PAGE_QUALITY","访问检验规则","Access quality rules","الوصول إلى قواعد الفحص","PAGE","QUALITY",160),
                p("PAGE_BATCH","访问批次管理","Access batch management","الوصول إلى إدارة الدفعات","PAGE","QUALITY",165),
                p("PAGE_WORK_ORDER","访问生产工单","Access work orders","الوصول إلى أوامر العمل","PAGE","PRODUCTION",170),
                p("PAGE_INTEGRATION","访问接口同步","Access integration sync","الوصول إلى مزامنة التكامل","PAGE","INTEGRATION",180),
                p("PAGE_SYNC_JOB","访问定时任务","Access scheduled jobs","الوصول إلى المهام المجدولة","PAGE","INTEGRATION",190),
                p("MASTER_DATA_CREATE","新增基础资料","Create master data","إضافة بيانات أساسية","ACTION","MASTER_DATA",210),
                p("MASTER_DATA_EDIT","编辑基础资料","Edit master data","تعديل البيانات الأساسية","ACTION","MASTER_DATA",220),
                p("MASTER_DATA_DISABLE","停用基础资料","Disable master data","تعطيل البيانات الأساسية","ACTION","MASTER_DATA",230),
                p("USER_CREATE","新增用户","Create user","إضافة مستخدم","ACTION","SECURITY",240),
                p("USER_STATUS","变更用户状态","Change user status","تغيير حالة المستخدم","ACTION","SECURITY",250),
                p("ROLE_CREATE","新增角色","Create role","إضافة دور","ACTION","SECURITY",260),
                p("ROLE_EDIT","配置角色权限","Configure role permissions","تكوين صلاحيات الدور","ACTION","SECURITY",270),
                p("PRODUCT_SYNC","同步产品","Sync product","مزامنة المنتج","ACTION","ENGINEERING",280),
                p("WORK_ORDER_SYNC","同步工单","Sync work order","مزامنة أمر العمل","ACTION","PRODUCTION",290),
                p("WORK_ORDER_RELEASE","下达工单","Release work order","إصدار أمر العمل","ACTION","PRODUCTION",300),
                p("WORK_ORDER_START","开工","Start work order","بدء أمر العمل","ACTION","PRODUCTION",310),
                p("WORK_ORDER_COMPLETE","完工","Complete work order","إكمال أمر العمل","ACTION","PRODUCTION",320),
                p("SYNC_JOB_RUN","立即执行任务","Run sync job","تشغيل مهمة المزامنة","ACTION","INTEGRATION",330),
                p("SYNC_JOB_TOGGLE","启停定时任务","Enable or disable job","تشغيل أو إيقاف المهمة","ACTION","INTEGRATION",340),
                p("SYNC_JOB_EDIT","编辑任务计划","Edit job schedule","تعديل جدولة المهمة","ACTION","INTEGRATION",350)
        };
    }

    private PermissionDefinition p(String code, String zh, String en, String ar, String type, String group, int sort) {
        return new PermissionDefinition(code, zh, en, ar, type, group, sort);
    }

    private static class PermissionDefinition {
        private final String code, nameZh, nameEn, nameAr, type, group;
        private final int sortOrder;
        private PermissionDefinition(String code, String nameZh, String nameEn, String nameAr, String type, String group, int sortOrder) {
            this.code=code; this.nameZh=nameZh; this.nameEn=nameEn; this.nameAr=nameAr; this.type=type; this.group=group; this.sortOrder=sortOrder;
        }
    }
}
