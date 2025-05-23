/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software

* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package tech.stackable.nifi.processors.iceberg;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static tech.stackable.nifi.services.iceberg.AbstractCatalogService.HADOOP_CONFIGURATION_RESOURCES;
import static tech.stackable.nifi.services.iceberg.AbstractCatalogService.WAREHOUSE_LOCATION;
import static tech.stackable.nifi.services.iceberg.IcebergHiveCatalogService.METASTORE_URI;

import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.serialization.record.MockRecordParser;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.stackable.nifi.services.iceberg.IcebergHiveCatalogService;

public class TestPutIcebergCustomValidation {

  private static final String RECORD_READER_NAME = "record-reader";
  private static final String KERBEROS_USER_SERVICE_NAME = "kerberos-user-service";
  private static final String CATALOG_SERVICE_NAME = "catalog-service";

  private static final String CATALOG_NAMESPACE = "catalogNamespace";
  private static final String TABLE_NAME = "tableName";

  private TestRunner runner;

  @BeforeEach
  public void setUp() {
    PutIceberg processor = new PutIceberg();
    runner = TestRunners.newTestRunner(processor);
  }

  private void initRecordReader() throws InitializationException {
    MockRecordParser readerFactory = new MockRecordParser();

    runner.addControllerService(RECORD_READER_NAME, readerFactory);
    runner.enableControllerService(readerFactory);

    runner.setProperty(PutIceberg.RECORD_READER, RECORD_READER_NAME);
  }

  private void initCatalogService(String configFilePaths) throws InitializationException {
    final IcebergHiveCatalogService catalogService = new IcebergHiveCatalogService();
    runner.addControllerService(CATALOG_SERVICE_NAME, catalogService);
    runner.setProperty(catalogService, METASTORE_URI, "test-metastore");
    runner.setProperty(catalogService, WAREHOUSE_LOCATION, "test-warehouse");
    runner.setProperty(catalogService, HADOOP_CONFIGURATION_RESOURCES, configFilePaths);

    runner.enableControllerService(catalogService);

    runner.setProperty(PutIceberg.CATALOG, CATALOG_SERVICE_NAME);
  }

  private void initKerberosUserService() throws InitializationException {
    KerberosUserService kerberosUserService = mock(KerberosUserService.class);
    when(kerberosUserService.getIdentifier()).thenReturn(KERBEROS_USER_SERVICE_NAME);

    runner.addControllerService(KERBEROS_USER_SERVICE_NAME, kerberosUserService);
    runner.enableControllerService(kerberosUserService);

    runner.setProperty(PutIceberg.KERBEROS_USER_SERVICE, KERBEROS_USER_SERVICE_NAME);
  }

  @Test
  public void testCustomValidateWithKerberosSecurityConfigAndWithoutKerberosUserService()
      throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/secured-core-site.xml");

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);
    runner.assertNotValid();
  }

  @Test
  public void testCustomValidateWithKerberosSecurityConfigAndKerberosUserService()
      throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/secured-core-site.xml");

    initKerberosUserService();

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);
    runner.assertValid();
  }

  @Test
  public void testCustomValidateWithoutKerberosSecurityConfigAndKerberosUserService()
      throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/unsecured-core-site.xml");

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);
    runner.assertValid();
  }

  @Test
  public void testCustomValidateWithoutKerberosSecurityConfigAndWithKerberosUserService()
      throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/unsecured-core-site.xml");

    initKerberosUserService();

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);
    runner.assertNotValid();
  }

  @Test
  public void testInvalidSnapshotSummaryDynamicProperty() throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/unsecured-core-site.xml");

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);

    runner.setProperty("invalid.dynamic.property", "test value");
    runner.assertNotValid();
  }

  @Test
  public void testValidSnapshotSummaryDynamicProperty() throws InitializationException {
    initRecordReader();
    initCatalogService("src/test/resources/unsecured-core-site.xml");

    runner.setProperty(PutIceberg.CATALOG_NAMESPACE, CATALOG_NAMESPACE);
    runner.setProperty(PutIceberg.TABLE_NAME, TABLE_NAME);

    runner.setProperty("snapshot-property.valid-property", "test value");
    runner.assertValid();
  }
}
