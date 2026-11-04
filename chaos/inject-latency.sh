#!/usr/bin/env bash
set -e

CONTAINER=${1:-dc-node-2}
LATENCY_MS=${2:-250ms}

echo "[CHAOS] Injecting $LATENCY_MS packet delay into $CONTAINER via tc..."
docker exec --privileged "$CONTAINER" tc qdisc add dev eth0 root netem delay "$LATENCY_MS" 2>/dev/null || \
docker exec --privileged "$CONTAINER" tc qdisc change dev eth0 root netem delay "$LATENCY_MS"
echo "[CHAOS] Injected latency $LATENCY_MS into $CONTAINER."
