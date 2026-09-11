#!/bin/bash
# Test SAP OData API connectivity from inside the pod
TOKEN=$(echo -n "MES_P:Taiking@5563" | base64)
echo "Testing SAP URL: https://my200725.s4hana.sapcloud.cn"
echo "Using Basic Auth: MES_P:***"
curl -s -o /dev/null -w "HTTP %{http_code}" \
  -H "Authorization: Basic $TOKEN" \
  -H "Accept: application/json" \
  "https://my200725.s4hana.sapcloud.cn/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product?\$top=1"
echo ""
echo "=== Try with x-csrf-token fetch ==="
curl -s -w "\nHTTP %{http_code}" \
  -H "Authorization: Basic $TOKEN" \
  -H "Accept: application/json" \
  -H "x-csrf-token: Fetch" \
  "https://my200725.s4hana.sapcloud.cn/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product?\$top=1" 2>&1 | head -5
