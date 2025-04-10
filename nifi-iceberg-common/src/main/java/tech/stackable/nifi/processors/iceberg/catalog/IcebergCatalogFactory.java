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
package tech.stackable.nifi.processors.iceberg.catalog;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.hive.HiveCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.stackable.nifi.services.iceberg.IcebergCatalogProperty;
import tech.stackable.nifi.services.iceberg.IcebergCatalogService;

import java.util.HashMap;
import java.util.Map;

import static tech.stackable.nifi.services.iceberg.IcebergCatalogProperty.*;

public class IcebergCatalogFactory {
//    private static final Logger LOGGER = LoggerFactory.getLogger(IcebergCatalogFactory.class);
    private final IcebergCatalogService catalogService;

    public IcebergCatalogFactory(IcebergCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    public Catalog create() {
        return switch (catalogService.getCatalogType()) {
            case HIVE -> initHiveCatalog(catalogService);
        };
    }

    private Catalog initHiveCatalog(IcebergCatalogService catalogService) {
        HiveCatalog catalog = new HiveCatalog();

        final Map<IcebergCatalogProperty, Object> catalogProperties = catalogService.getCatalogProperties();
        final Map<String, String> properties = new HashMap<>();

        if (catalogProperties.containsKey(METASTORE_URI)) {
            properties.put(CatalogProperties.URI, (String) catalogProperties.get(METASTORE_URI));
        }

        if (catalogProperties.containsKey(WAREHOUSE_LOCATION)) {
            properties.put(CatalogProperties.WAREHOUSE_LOCATION, (String) catalogProperties.get(WAREHOUSE_LOCATION));
        }

        final Configuration hadoopConf = new Configuration();
        if (catalogProperties.containsKey(S3_ENDPOINT_OVERRIDE)) {
            hadoopConf.set("fs.s3a.endpoint", (String) catalogProperties.get(S3_ENDPOINT_OVERRIDE));
        }
        if (catalogProperties.containsKey(S3_PATH_STYLE_ACCESS)) {
            hadoopConf.set("fs.s3a.path.style.access", (String) catalogProperties.get(S3_PATH_STYLE_ACCESS));
        }
        if (catalogProperties.containsKey(S3_AWS_ACCESS_KEY_ID)) {
            hadoopConf.set("fs.s3a.access.key", (String) catalogProperties.get(S3_AWS_ACCESS_KEY_ID));
        }
        if (catalogProperties.containsKey(S3_AWS_SECRET_ACCESS_KEY)) {
            hadoopConf.set("fs.s3a.secret.key", (String) catalogProperties.get(S3_AWS_SECRET_ACCESS_KEY));
        }
        catalog.setConf(hadoopConf);

//        LOGGER.info("Create Iceberg Hive catalog using the properties: {} and the following Hadoop properties:", properties);
//        for (Map.Entry<String, String> entry : hadoopConf) {
//            LOGGER.info("Key: {}, Value: {}", entry.getKey(), entry.getValue());
//        }

        catalog.initialize("hive-catalog", properties);
        return catalog;
    }
}
