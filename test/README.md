Hive metastore URI: `thrift://hive-iceberg.nifi.svc.cluster.local:9083`
Default Warehouse Location: `s3a://demo/lakehouse`
S3 endpoint `http://minio:9000`

```sql
CREATE SCHEMA iceberg.test WITH (location = 's3a://demo/lakehouse/test');
create table iceberg.test.test (
	hello varchar
);
select * from iceberg.test.test;
select * from iceberg.test."test$manifests";
select * from iceberg.test."test$files";
```
