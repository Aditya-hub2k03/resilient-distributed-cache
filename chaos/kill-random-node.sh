#!/usr/bin/env bash
set -e

CONTAINERS=("dc-node-1" "dc-node-2" "dc-node-3")
RANDOM_CONTAINER=${CONTAINERS[$RANDOM % ${#CONTAINERS[@]}]}

echo "[CHAOS] Randomly selecting node to kill: $RANDOM_CONTAINER"
docker stop "$RANDOM_CONTAINER"
echo "[CHAOS] $RANDOM_CONTAINER stopped successfully."
