#!/bin/bash
# Update SAP credentials (username + password) in K8s Secret
# SAP system switched from my200725 to my200683
# New credentials: MES_T / new password
NEW_USER='MES_T'
NEW_PASS='ooljsMX8dVfMDjjgrdhzzjGwtPyZg$JgEdgXYvWm'

# Update ConfigMap with new SAP URL and username
kubectl patch configmap tns-mes-config -n tns-mes -p '{"data":{"MES_SAP_BASE_URL":"https://my200683.s4hana.sapcloud.cn","MES_SAP_USERNAME":"MES_T"}}' 2>&1
echo "CONFIGMAP_UPDATED"

# Update Secret with new password
B64_PASS=$(echo -n "$NEW_PASS" | base64)
echo "Password base64: $B64_PASS"
kubectl patch secret tns-mes-secrets -n tns-mes -p "{\"data\":{\"sap-password\":\"$B64_PASS\"}}" 2>&1
echo "SECRET_UPDATED"

# Verify
echo -n "MES_SAP_BASE_URL: "; kubectl get configmap tns-mes-config -n tns-mes -o jsonpath='{.data.MES_SAP_BASE_URL}' 2>&1; echo ""
echo -n "MES_SAP_USERNAME: "; kubectl get configmap tns-mes-config -n tns-mes -o jsonpath='{.data.MES_SAP_USERNAME}' 2>&1; echo ""
echo -n "sap-password: "; kubectl get secret tns-mes-secrets -n tns-mes -o jsonpath='{.data.sap-password}' 2>&1; echo ""

# Restart deployment to pick up new credentials
echo "Restarting deployment..."
kubectl rollout restart deployment/tns-mes-backend -n tns-mes 2>&1
echo "DEPLOYMENT_RESTARTED"
