#!/bin/bash
docker run --rm mcr.microsoft.com/mssql-tools:latest /opt/mssql-tools/bin/sqlcmd -S 10.30.10.141 -U tns_mes_user -P "Taiking@5563" -Q "DELETE FROM tns_mes.dbo.flyway_schema_history WHERE version = N'24' AND success = 0; SELECT @@ROWCOUNT AS DeletedRows"
