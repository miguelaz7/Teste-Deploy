#!/bin/bash
DATE_1H_AGO=$(date -u -d "1 hour ago" +"%Y-%m-%dT%H:%M:%SZ")
DATE_2H_AGO=$(date -u -d "2 hours ago" +"%Y-%m-%dT%H:%M:%SZ")
DATE_3H_AGO=$(date -u -d "3 hours ago" +"%Y-%m-%dT%H:%M:%SZ")
DATE_4H_AGO=$(date -u -d "4 hours ago" +"%Y-%m-%dT%H:%M:%SZ")

echo "Ingesting history data directly..."

curl -s -X POST -H "Content-Type: application/json" -d "{
  \"validacoes\": [
    {
      \"cardId\": \"CARD-HIST-1\",
      \"ticketId\": \"TK-HIST-1\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_1H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-2\",
      \"ticketId\": \"TK-HIST-2\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"AVULSO\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_2H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-3\",
      \"ticketId\": \"TK-HIST-3\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"AVULSO\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_2H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-4\",
      \"ticketId\": \"TK-HIST-4\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_3H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-5\",
      \"ticketId\": \"TK-HIST-5\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_3H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-6\",
      \"ticketId\": \"TK-HIST-6\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_3H_AGO\",
      \"originStopId\": \"10\",
      \"route_id\": \"13\",
      \"trip_id\": \"13_15\",
      \"fareForAdult\": \"1.50\",
      \"equipmentId\": \"VLDR-01\",
      \"transactionVehicleNum\": \"VH-001\",
      \"result\": \"OK\"
    },
    {
      \"cardId\": \"CARD-HIST-7\",
      \"ticketId\": \"TK-HIST-7\",
      \"mediaType\": \"RFID_CARD\",
      \"ticketTypeCode\": \"MENSAL\",
      \"transactionType\": \"BOARDING\",
      \"transactionDateTime\": \"$DATE_4H_AGO\",
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
