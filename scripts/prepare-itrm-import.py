"""Prepare idempotent TRM user and legacy-requirement import payloads."""

import argparse
import json
import re
from datetime import date, datetime
from pathlib import Path

import openpyxl


def text(value):
    return "" if value is None else str(value).strip()


def iso_date(value):
    if isinstance(value, (datetime, date)):
        return value.strftime("%Y-%m-%d")
    value = text(value)
    match = re.search(r"(20\d{2})[-/.年](\d{1,2})[-/.月](\d{1,2})", value)
    return f"{match.group(1)}-{int(match.group(2)):02d}-{int(match.group(3)):02d}" if match else None


def compact_title(value, fallback):
    for line in text(value).splitlines():
        line = re.sub(r"^[\s*#、.\-\d]+", "", line).strip()
        if line:
            return line[:200]
    return fallback


def write_json(path, payload):
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2, default=str), encoding="utf-8")


def prepare_users(source, output_dir):
    sheet = openpyxl.load_workbook(source, data_only=True, read_only=True).active
    users = []
    for row in sheet.iter_rows(min_row=11, values_only=True):
        name, username, department_raw = text(row[0]), text(row[1]), text(row[4])
        if not name and not username:
            continue
        paths = [item.strip().replace("\\", "/") for item in department_raw.split(";") if item.strip()]
        if not paths:
            paths = ["泰康电子有限公司/未分配部门"]
        email = text(row[10]) or text(row[8])
        disabled = "禁用" in text(row[17]) and "未禁用" not in text(row[17])
        users.append({
            "username": username,
            "realName": name,
            "department": paths[0],
            "departmentPaths": paths,
            "email": email,
            "active": not disabled,
        })
    payload = {"defaultPassword": "taikang123.", "users": users}
    write_json(output_dir / "users.json", payload)
    return len(users)


def export_images(sheet, source_code, output_dir):
    image_dir = output_dir / "images"
    image_dir.mkdir(parents=True, exist_ok=True)
    manifest = []
    for index, image in enumerate(getattr(sheet, "_images", []), start=1):
        row = image.anchor._from.row + 1
        column = image.anchor._from.col + 1
        extension = (getattr(image, "format", None) or "png").lower()
        if extension == "jpeg":
            extension = "jpg"
        filename = f"{source_code}-{row}-{column}-{index}.{extension}"
        target = image_dir / filename
        target.write_bytes(image._data())
        manifest.append({"externalRef": f"{source_code}-{text(sheet.cell(row, 1).value)}-R{row}", "file": str(target)})
    return manifest


def prepare_mes_requirements(source, output_dir):
    workbook = openpyxl.load_workbook(source, data_only=True)
    sheet = workbook.active
    status_map = {
        "已完成": "COMPLETED",
        "待处理": "PENDING_RECEIVE",
        "持续跟进": "IN_DEVELOPMENT",
        "处理中": "IN_DEVELOPMENT",
        "无效需求": "REJECTED",
        "不支持": "REJECTED",
        "暂无需求": "REJECTED",
    }
    category_map = {"BUG": "BUG", "优化": "FUNC_OPT", "需求": "NEW_FUNC", "开发": "NEW_FUNC", "泰康处理": "OTHER"}
    owner_map = {"徐昌师": "TK04729", "史浩军": "TK26022601", "龚嘉俊": "TKWZ25120101", "蒋晓晓": "TK04042", "王欣怡": "TK26052503", "刘洲": "TK26052002"}
    rows = []
    for row_number in range(2, sheet.max_row + 1):
        seq = text(sheet.cell(row_number, 1).value)
        problem = text(sheet.cell(row_number, 3).value)
        if not seq and not problem:
            continue
        factory = text(sheet.cell(row_number, 2).value)
        department = text(sheet.cell(row_number, 7).value) or "历史台账未填写部门"
        proposer = text(sheet.cell(row_number, 8).value) or "历史台账未填写"
        source_status = text(sheet.cell(row_number, 13).value)
        source_type = text(sheet.cell(row_number, 12).value)
        remark = text(sheet.cell(row_number, 14).value)
        solution = text(sheet.cell(row_number, 15).value)
        mes_consultant = text(sheet.cell(row_number, 16).value)
        taikang_consultant = text(sheet.cell(row_number, 17).value)
        rows.append({
            "externalRef": f"MES-{seq}-R{row_number}",
            "title": compact_title(problem, f"MES历史需求 {seq}"),
            "category": category_map.get(source_type, "OTHER"),
            "belongSystem": "MES",
            "urgency": "HIGH" if source_type == "BUG" and source_status not in ("已完成", "无效需求") else "MEDIUM",
            "sourceDept": department,
            "proposer": proposer,
            "currentProblem": problem,
            "description": f"来源工厂：{factory}" if factory else "",
            "expectedEffect": solution,
            "acceptanceCriteria": "按历史实际上线结果验收" if source_status == "已完成" else solution,
            "remark": "；".join(item for item in [remark, f"工厂：{factory}" if factory else "", f"MES顾问：{mes_consultant}" if mes_consultant else "", f"泰康顾问：{taikang_consultant}" if taikang_consultant else ""] if item),
            "legacyStatus": source_status,
            "status": status_map.get(source_status, "PENDING_RECEIVE"),
            "reviewOwner": owner_map.get(mes_consultant, ""),
            "devOwner": owner_map.get(taikang_consultant, ""),
            "expectedCompletionDate": iso_date(sheet.cell(row_number, 10).value),
            "plannedEndDate": iso_date(sheet.cell(row_number, 10).value),
            "onlineDate": iso_date(sheet.cell(row_number, 11).value),
            "createdAt": iso_date(sheet.cell(row_number, 9).value),
        })
    payload = {"source": "MES历史问题台账", "requirements": rows}
    write_json(output_dir / "requirements-mes.json", payload)
    return len(rows), export_images(sheet, "MES", output_dir)


def prepare_sap_requirements(source, output_dir):
    workbook = openpyxl.load_workbook(source, data_only=True)
    sheet = workbook.active
    rows = []
    for row_number in range(2, sheet.max_row + 1):
        seq = text(sheet.cell(row_number, 1).value)
        requirement = text(sheet.cell(row_number, 6).value)
        if not seq and not requirement:
            continue
        support_result = text(sheet.cell(row_number, 5).value)
        user_result = text(sheet.cell(row_number, 7).value)
        combined_result = f"{support_result}\n{user_result}"
        rejected = any(word in combined_result for word in ("不支持", "关闭", "无法实现", "不能实现", "不考虑"))
        completed = any(word in user_result for word in ("已经实现", "功能已经实现", "在OA实现"))
        status = "REJECTED" if rejected else ("COMPLETED" if completed else "PENDING_RECEIVE")
        system = "OA" if "OA" in requirement or "OA" in user_result else "SAP"
        category = "REPORT" if "报表" in requirement else ("BUG" if "bug" in requirement.lower() else "FUNC_OPT")
        proposer = text(sheet.cell(row_number, 3).value) or "历史台账未填写"
        confirmer = text(sheet.cell(row_number, 4).value)
        logic = text(sheet.cell(row_number, 10).value)
        note = text(sheet.cell(row_number, 11).value)
        rows.append({
            "externalRef": f"SAP-{seq}-R{row_number}",
            "title": compact_title(requirement, f"SAP财务历史需求 {seq}"),
            "category": category,
            "belongSystem": system,
            "urgency": "MEDIUM",
            "sourceDept": text(sheet.cell(row_number, 2).value) or "财务部",
            "proposer": proposer,
            "currentProblem": requirement,
            "description": logic,
            "expectedEffect": user_result or logic,
            "acceptanceCriteria": user_result,
            "remark": "；".join(item for item in [f"待确认人：{confirmer}" if confirmer else "", f"BOS确认：{support_result}" if support_result else "", note] if item),
            "legacyStatus": support_result or user_result,
            "status": status,
        })
    payload = {"source": "SAP财务需求台账", "requirements": rows}
    write_json(output_dir / "requirements-sap.json", payload)
    return len(rows), export_images(sheet, "SAP", output_dir)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--contacts", required=True, type=Path)
    parser.add_argument("--mes", required=True, type=Path)
    parser.add_argument("--sap", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    users = prepare_users(args.contacts, args.output)
    mes_count, mes_images = prepare_mes_requirements(args.mes, args.output)
    sap_count, sap_images = prepare_sap_requirements(args.sap, args.output)
    write_json(args.output / "images.json", mes_images + sap_images)
    print(json.dumps({"users": users, "mesRequirements": mes_count, "sapRequirements": sap_count, "images": len(mes_images) + len(sap_images)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
