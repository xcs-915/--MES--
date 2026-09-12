package com.tns.mes.integration.sap.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 客户主数据（SAP A_BusinessPartner，BusinessPartnerIsCustomer = true）。
 */
@Entity
@Table(name = "mes_customer")
public class Customer extends AuditedEntity {

    @Column(name = "bp_number", nullable = false, unique = true, length = 64)
    private String bpNumber;

    @Column(name = "bp_type", length = 16)
    private String bpType;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "search_term", length = 100)
    private String searchTerm;

    @Column(length = 10)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 200)
    private String street;

    @Column(name = "tax_number", length = 40)
    private String taxNumber;

    @Column(name = "vat_registration", length = 40)
    private String vatRegistration;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false, length = 30)
    private String source = "SAP";

    @Column(length = 500)
    private String remark;

    public String getBpNumber() { return bpNumber; }
    public void setBpNumber(String value) { bpNumber = value; }
    public String getBpType() { return bpType; }
    public void setBpType(String value) { bpType = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { fullName = value; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String value) { searchTerm = value; }
    public String getCountry() { return country; }
    public void setCountry(String value) { country = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = value; }
    public String getStreet() { return street; }
    public void setStreet(String value) { street = value; }
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String value) { taxNumber = value; }
    public String getVatRegistration() { return vatRegistration; }
    public void setVatRegistration(String value) { vatRegistration = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
