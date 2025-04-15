# nifi-iceberg-bundle

NiFi `1.19.0` added support for the PutIceberg processor in [NIFI-10442](https://issues.apache.org/jira/browse/NIFI-10442).
Just before `2.0.0` (and after `2.0.0-M4`), it has been removed in [NIFI-13938](https://issues.apache.org/jira/browse/NIFI-13938) due to Hive 3 being end of life and the number of CVEs caused by it.
This repos is based on the [removal PR](https://github.com/apache/nifi/pull/9460) with some feature removals and additions.

> [!WARNING]
> Although we took some steps of updating dependencies and removing dependencies alongside with functionality, the situation is still relevant.
You need to decide if the added functionality is worth the added CVEs.

## Functionality

Currently the following features are supported:

* S3 object storage support
* Hive metastore as catalog implementation
* Parquet, ORC and Avro file formats

The following functionality has been added over the original implementation:

1. You don't need to rely on an `core-site.xml` somewhere in the NiFi filesystem. Instead, we made the relevant configuration options (such as S3 endpoint, path style access etc.) configurable via the NiFi UI.
2. We integrated with the `AWS Credentials Provider service`, so that it's easier to retrieve S3 credentials.

The following features were not carried over from the original implementation, we might consider adding them in the future (as the code should be there):

* HDFS support
* Kerberos support
* JDBC catalog
* Hadoop catalog

## TODOs

- [X] Add license
- [x] Spotless plugin
- [X] Think about renaming HiveCatalogService to IcebergHiveCatalogService
- [x] Think about removing override from S3 endpoint override
- [x] Add user-facing docs
- [x] Especially `nifi-iceberg-processors/src/main/resources/docs/tech.stackable.nifi.processors.iceberg.PutIceberg/additionalDetails.md`
- [ ] Re-add tests
- [ ] Clean git history
- [ ] Add branch protection :(
