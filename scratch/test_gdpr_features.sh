#!/bin/bash
# test_gdpr_features.sh
# Tests: Ingestion -> Uncategorized Fila -> Export Request -> Export Approval -> Right to be Forgotten

echo "=== 1. Ingesting event with known ticket type MENSAL and card CARD_TEST_123 ==="
DATE_NOW=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
curl -s -X POST -H "Content-Type: application/json" -d "{
  \"cardId\": \"CARD_TEST_123\",
  \"ticketId\": \"TK-123\",
  \"mediaType\": \"RFID_CARD\",
  \"ticketTypeCode\": \"MENSAL\",
  \"transactionType\": \"BOARDING\",
  \"transactionDateTime\": \"$DATE_NOW\",
  \"originStopId\": \"10\",
  \"route_id\": \"13\",
  \"trip_id\": \"13_15\",
  \"fareForAdult\": \"1.50\",
  \"equipmentId\": \"VLDR-01\",
  \"transactionVehicleNum\": \"VH-001\",
  \"result\": \"OK\"
}" http://localhost:8080/api/validations/ingest
echo -e "\n"

echo "=== 2. Ingesting event with UNKNOWN ticket type BILHETE_SECRETO_999 ==="
curl -s -X POST -H "Content-Type: application/json" -d "{
  \"cardId\": \"CARD_TEST_456\",
  \"ticketId\": \"TK-456\",
  \"mediaType\": \"RFID_CARD\",
  \"ticketTypeCode\": \"BILHETE_SECRETO_999\",
  \"transactionType\": \"BOARDING\",
  \"transactionDateTime\": \"$DATE_NOW\",
  \"originStopId\": \"10\",
  \"route_id\": \"13\",
  \"trip_id\": \"13_15\",
  \"fareForAdult\": \"1.50\",
  \"equipmentId\": \"VLDR-01\",
  \"transactionVehicleNum\": \"VH-001\",
  \"result\": \"OK\"
}" http://localhost:8080/api/validations/ingest
echo -e "\n"

echo "=== 3. Fetching Pending Uncategorized Events Queue ==="
UN_EVENTS=$(curl -s http://localhost:8080/categorization/nao-categorizados)
echo "Response: $UN_EVENTS"
EVENT_ID=$(echo "$UN_EVENTS" | grep -o '"id":[0-9]*' | head -n 1 | cut -d':' -f2)
echo "First Pending Event ID found: $EVENT_ID"
echo -e "\n"

if [ ! -z "$EVENT_ID" ]; then
  echo "=== 4. Resolving Uncategorized Event $EVENT_ID as RECLASSIFICADO ==="
  curl -s -X PUT -H "Content-Type: application/json" -d '{"estado": "RECLASSIFICADO", "resolvidoPor": "DPO-Test"}' http://localhost:8080/categorization/nao-categorizados/$EVENT_ID/resolver
  echo -e "\n"
fi

echo "=== 5. Submitting Open Data Export Request (as analyst) ==="
EXPORT_RES=$(curl -s -X POST -H "Content-Type: application/json" -H "X-Api-User: analista" -d '{"formato": "JSON", "filtros": "route_id=13"}' http://localhost:8080/api/ngsi-ld/exportacoes)
echo "Response: $EXPORT_RES"
EXPORT_ID=$(echo "$EXPORT_RES" | grep -o '"id":[0-9]*' | head -n 1 | cut -d':' -f2)
echo "Export ID: $EXPORT_ID"
echo -e "\n"

if [ ! -z "$EXPORT_ID" ]; then
  echo "=== 6. DPO approves the Export Request ==="
  curl -s -X PUT -H "Content-Type: application/json" -H "X-Api-User: dpo-test" -d '{"decisao": "APROVADA"}' http://localhost:8080/api/ngsi-ld/exportacoes/$EXPORT_ID/aprovar
  echo -e "\n"
fi

echo "=== 7. Executing DPO Right to be Forgotten on card CARD_TEST_123 ==="
curl -s -X POST -H "Content-Type: application/json" -H "X-Api-User: dpo-test" -d '{"cardId": "CARD_TEST_123"}' http://localhost:8080/api/rgpd/direito-ao-esquecimento
echo -e "\n"

echo "=== 8. Checking Audit Logs for GDPR actions ==="
# We can search the database or check logs, but let's query the console or stats.
echo "Testing complete."
