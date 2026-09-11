#!/bin/bash
docker run --rm mcr.microsoft.com/mssql-tools:latest /opt/mssql-tools/bin/sqlcmd -S 10.30.10.141 -U tns_mes_user -P "Taiking@5563" -Q "SELECT TOP 5 installed_rank, version, description, success FROM tns_mes.dbo.flyway_schema_history ORDER BY installed_rank DESC"
