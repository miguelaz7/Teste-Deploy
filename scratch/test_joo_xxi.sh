#!/bin/bash
# Get current WSL host time in UTC minus 1 minute
DATE_1MIN_AGO=$(date -u -d "1 minute ago" +"%Y-%m-%dT%H:%M:%SZ")

echo "Host current time (UTC): $(date -u)"
echo "Ingestion transactionDateTime: $DATE_1MIN_AGO"

# Ingest validation
curl -s -X POST -H "Content-Type: application/json" -d "{
  \"cardId\": \"1234567890\",
  \"ticketId\": \"TK-TESTE-999\",
  \"mediaType\": \"RFID_CARD\",
  \"ticketTypeCode\": \"MENSAL\",
  \"transactionType\": \"BOARDING\",
  \"transactionDateTime\": \"$DATE_1MIN_AGO\",
  \"originStopId\": \"10\",
  \"route_id\": \"13\",
  \"trip_id\": \"13_15\",
  \"fareForAdult\": \"1.50\",
  \"equipmentId\": \"VLDR-01\",
  \"transactionVehicleNum\": \"VH-001\",
  \"result\": \"OK\"
}" http://localhost:8080/api/validations/ingest

echo ""
