#!/usr/bin/env bash
set -e

echo "=== STARTING SELF-HEALING CHAOS RUNNER ==="

echo "[1/3] Writing test keys to cluster..."
for i in {1..20}; do
  curl -s -X POST http://localhost:8081/api/v1/cache \
       -H "Content-Type: application/json" \
       -d "{\"key\": \"chaos:key:$i\", \"value\": \"val-$i\"}" > /dev/null
done
echo "Keys populated."

echo "[2/3] Simulating node termination..."
./kill-random-node.sh

echo "[3/3] Triggering anti-entropy verification sweep..."
sleep 5
curl -s -X POST http://localhost:8081/api/v1/cluster/heal
echo ""
echo "=== CHAOS HARNESS RUN COMPLETED ==="
