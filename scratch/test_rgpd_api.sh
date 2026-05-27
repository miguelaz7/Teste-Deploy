#!/bin/bash
echo "=== Creating Policy ==="
curl -s -X POST -H 'Content-Type: application/json' -d '{"campo": "ticketId", "metodo": "SUPRESSAO", "retencaoDias": -1, "aprovadoPor": "dpo@ticketub.pt", "notas": "Supressao de ticketId"}' http://localhost:8080/api/rgpd/politicas
echo ""
echo "=== Listing Active Policies ==="
curl -s http://localhost:8080/api/rgpd/politicas
echo ""
