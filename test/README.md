Testing the NARs

1. Run `./deploy.sh` (optionally with a namespace as the first argument, deaults to `nifi`)
2. Load dbeaver or trino-cli (user `admin`, no password), connect to the Trino Coordinator (you might need to port forward), and run the following SQL:
   ```sql
   create schema iceberg.test with (location = 's3a://demo/lakehouse/test');
   create table iceberg.test.test (
     hello varchar
   );
   ```
3. Replace the NIFI_VERSION placeholder `sed -i -e 's/{{ NIFI_VERSION }}/2.4.0/g' pom.xml`.
4. Update the NiFi version in `update-nars.sh`, then run it to build and upload the NARs, then restart NiFi.
5. Open the NiFi Web UI (once it has restarted).
   If you don't have no access to the NodePort, you can port forward to the Caddy sidecar on port `5000` to get to the NiFi web UI via <http://localhost:5000>.
   If you need help adding the processors, see: <https://github.com/stackabletech/nifi-iceberg-bundle#create-generateflowfile-processor>
   - Create a `JsonTreereader` Controller Service (no config needed), and _Enable_ it.
   - Create an `AWSCredentialsProviderControllerService` Controller Service, and _Enable_ it after setting the following configuration:
     - **Access Key ID**: `admin`
     - **Secret Access Key**: `adminadmin`
   - Create an `IcebergHiveCatalogService` Controller Serice, and _Enable_ it after setting the following configuration:
     - **Hive Metastore URI**: `thrift://hive-iceberg-metastore.nifi.svc.cluster.local:9083` (replace the namespace if needed)
     - **Default Warehouse Location**: `s3a://demo/lakehouse`
     - **S3 endpoint**: `https://minio:9000`
     - **S3 path style access**: `true`
     - **AWS Credentials Provider Service**: Select the one created earlier
   - Add `GenerateFlowFile` processor with
     - **Custom Text**: `{"hello":"world from NiFi :)"}`
   - Add a `PutIceberg` processor with:
     - **Record Reader**: Select the `JsonTreeReader` created earlier
     - **Catalog Service**: Select the `IcebergHiveCatalogService` created earlier
     - **Catalog Namespace**: `test`
     - **Table Name**: `test`
     - **File Format**: `PARQUET`
   - Connect `GenerateFlowFile` -> `PutIceberg`
   - Add two `Funnels`, and connect `PutIceberg` to each for success and failure.
   - _Start_ the `PutIceberg` processor, then _Run Once_ the `GenerateFlowFile`
   - The success queue should have one entry.
     - Clear the queues if you need to retest, so that you don't need to remember where it got up to.
   - From Trino, run some queries and ensure you see a results.
     ```sql
     select * from iceberg.test.test;
     select * from iceberg.test."test$manifests";
     select * from iceberg.test."test$files";
     ```
6. Repeat from step 4 if any code changes are needed.
