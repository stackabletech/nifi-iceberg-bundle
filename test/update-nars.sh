#!/usr/bin/env bash
set -e

# Run from repo root directory!

NAMESPACE="${1:-nifi}"

# nix run github:nixos/nixpkgs/nixos-unstable#maven -- -D nifi.version=2.6.0 -D cyclonedx.skip=true -D skipTests clean package
mvn -D nifi.version=2.6.0 -D cyclonedx.skip=true -D skipTests clean package

kubectl -n "$NAMESPACE" wait --for=condition=Ready pod/nifi-node-default-0

echo "Uploading nar files..."
kubectl -n "$NAMESPACE" cp -c nifi ./nifi-iceberg-services-api-nar/target/nifi-iceberg-services-api-nar-*.nar nifi-node-default-0:/stackable/userdata/nifi-processors/
kubectl -n "$NAMESPACE" cp -c nifi ./nifi-iceberg-services-nar/target/nifi-iceberg-services-nar-*.nar nifi-node-default-0:/stackable/userdata/nifi-processors/
kubectl -n "$NAMESPACE" cp -c nifi ./nifi-iceberg-processors-nar/target/nifi-iceberg-processors-nar-*.nar nifi-node-default-0:/stackable/userdata/nifi-processors/

echo "Uploaded nar files"
kubectl -n "$NAMESPACE" delete pod nifi-node-default-0
sleep 1
echo "Waiting for Pod to get ready again"
kubectl -n "$NAMESPACE" wait --for=condition=Ready pod/nifi-node-default-0 --timeout 30m
