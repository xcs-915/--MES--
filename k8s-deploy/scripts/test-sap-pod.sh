#!/bin/sh
TOKEN=$(echo -n "MES_P:Taiking@5563" | base64)
curl -s -o /dev/null -w "HTTP %{http_code}" \
  -H "Authorization: Basic $TOKEN" \
  -H "Accept: application/json" \
  "https://my200725.s4hana.sapcloud.cn/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product?\$top=1"
echo ""
