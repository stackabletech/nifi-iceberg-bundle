#!/usr/bin/env bash
set -e

# Run from repo root directory!

NAMESPACE="${1:-nifi}"

kubectl create namespace "$NAMESPACE" || true

kubectl apply -n "$NAMESPACE" -f test/minio-tls.yaml

helm upgrade --install minio \
--namespace "$NAMESPACE" \
--version 5.4.0 \
--values test/minio-values.yaml \
--repo https://charts.min.io/ minio \
--wait

helm upgrade --install postgresql-hive-iceberg \
--namespace "$NAMESPACE" \
--version 16.1.2 \
--set auth.username=hive \
--set auth.password=hivehive \
--set auth.database=hive \
--repo https://charts.bitnami.com/bitnami postgresql \
--wait

kubectl apply -n "$NAMESPACE" -f test/s3-connection.yaml
kubectl apply -n "$NAMESPACE" -f test/hive-metastore.yaml
kubectl apply -n "$NAMESPACE" -f test/nifi.yaml
kubectl apply -n "$NAMESPACE" -f test/trino.yaml
