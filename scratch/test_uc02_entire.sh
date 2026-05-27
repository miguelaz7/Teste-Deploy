#!/bin/bash
# Get native UTC time
DATE_NOW=$(date -u -d "1 minute ago" +"%Y-%m-%dT%H:%M:%SZ")

echo "Ingesting batch to test UC02 pipeline (1 valid, 1 invalid, 1 duplicate)..."

curl -s -X POST -H "Content-Type: application/json" -d "{
  \"validacoes\": [
    {
      \"cardId\": \"CARD-VALID-123\",
      \"ticketId\": \"TK-VALID-123\",
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
    },
    {
      \"cardId\": \"CARD-INVALID-456\",
      \"ticketId\": \"TK-INVALID-456\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_NOW\",
      \"originStopId\": \"99999\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-VALID-123\",
      \"ticketId\": \"TK-VALID-123\",
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
    }
  ]
}" http://localhost:8080/api/validations/ingest/batch

echo ""
