package com.tns.mes.execution.domain;

import com.tns.mes.common.domain.AuditedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "qua_inspection_item")
public class QualityInspectionItem extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    private QualityInspection inspection;
    @Column(name = "line_no", nullable = false)
    private Integer lineNo;
    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;
    @Column(length = 500)
    private String specification;
    @Column(name = "min_value", precision = 18, scale = 6)
    private BigDecimal minValue;
    @Column(name = "max_value", precision = 18, scale = 6)
    private BigDecimal maxValue;
    @Column(name = "measured_value", length = 500)
    private String measuredValue;
    @Column(length = 20)
    private String unit;
    @Column(nullable = false, length = 20)
    private String result;
    @Column(length = 500)
    private String remark;

    public QualityInspection getInspection() { return inspection; }
    public void setInspection(QualityInspection value) { inspection = value; }
    public Integer getLineNo() { return lineNo; }
    public void setLineNo(Integer value) { lineNo = value; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String value) { itemCode = value; }
    public String getItemName() { return itemName; }
    public void setItemName(String value) { itemName = value; }
    public String getSpecification() { return specification; }
    public void setSpecification(String value) { specification = value; }
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal value) { minValue = value; }
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal value) { maxValue = value; }
    public String getMeasuredValue() { return measuredValue; }
    public void setMeasuredValue(String value) { measuredValue = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public String getResult() { return result; }
    public void setResult(String value) { result = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
}
