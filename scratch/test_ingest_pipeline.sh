#!/bin/bash

# Clear existing events
echo "=== Clearing DB validation events, data lake, and quarantine ==="
docker exec -i tub-database mysql -u root -ppassword tub_pgu -e "DELETE FROM validation_events; DELETE FROM ngsi_ld_data_lake; DELETE FROM validation_quarantine;"

# Check current active policies
echo "=== Active Policies ==="
curl -s http://localhost:8080/api/rgpd/politicas
echo ""

# Perform Ingestion
echo "=== Ingesting Valid Event ==="
curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{"cardId":"1234567890","ticketId":"TK-987654","mediaType":"RFID_CARD","ticketTypeCode":"MENSAL","transactionType":"BOARDING","transactionDateTime":"2026-05-26T18:40:00Z","originStopId":"1","route_id":"2","trip_id":"2_305","fareForAdult":"1.50","equipmentId":"VLDR-01","transactionVehicleNum":"VH-001","result":"OK"}' \
  http://localhost:8080/api/validations/ingest

echo ""
echo "=== Checking validation_events in DB ==="
docker exec -i tub-database mysql -u root -ppassword tub_pgu -e "SELECT card_id, ticket_id, ingestion_hash FROM validation_events;"

echo "=== Checking ngsi_ld_data_lake payload_json ==="
docker exec -i tub-database mysql -u root -ppassword tub_pgu -e "SELECT payload_json FROM ngsi_ld_data_lake\G"

echo "=== Checking validation_quarantine ==="
docker exec -i tub-database mysql -u root -ppassword tub_pgu -e "SELECT reason, invalid_field, received_value, violated_rule FROM validation_quarantine;"
