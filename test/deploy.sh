#!/usr/bin/env bash
set -e

# Run from repo root directory!

kubectl create namespace nifi || true

helm install minio \
--namespace nifi \
--version 5.3.0 \
--values test/minio-values.yaml \
--repo https://charts.min.io/ minio \
--wait

helm install postgresql-hive-iceberg \
--namespace nifi \
--version 16.1.2 \
--set auth.username=hive \
--set auth.password=hivehive \
--set auth.database=hive \
--repo https://charts.bitnami.com/bitnami postgresql \
--wait

kubectl apply -f test/s3-connection.yaml
kubectl apply -f test/hive-metastore.yaml
kubectl apply -f test/nifi.yaml
kubectl apply -f test/trino.yaml
