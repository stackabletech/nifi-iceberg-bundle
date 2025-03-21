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
package tech.stackable.nifi.services.iceberg;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class holding common properties and methods for Catalog Service implementations.
 */
public abstract class AbstractCatalogService extends AbstractControllerService implements IcebergCatalogService {
    public static final PropertyDescriptor WAREHOUSE_LOCATION = new PropertyDescriptor.Builder()
            .name("warehouse-location")
            .displayName("Default Warehouse Location")
            .description("Location of default database for the warehouse.")
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .required(true)
            .build();

    public static final PropertyDescriptor S3_ENDPOINT_OVERRIDE = new PropertyDescriptor.Builder()
            .name("s3-endpoint-override")
            .displayName("S3 endpoint override")
            .description("Custom S3 endpoint in the format of an URL")
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .required(false)
            .build();

    public static final PropertyDescriptor S3_PATH_STYLE_ACCESS = new PropertyDescriptor.Builder()
            .name("s3-path-style-access")
            .displayName("S3 path style access")
            .description("Enable S3 path style access by disabling the default virtual hosting behaviour")
            .allowableValues("false", "true")
            .required(false)
            .build();

    protected Map<IcebergCatalogProperty, Object> catalogProperties = new HashMap<>();

    protected List<String> createFilePathList(String configFilePaths) {
        List<String> filePathList = new ArrayList<>();
        if (configFilePaths != null && !configFilePaths.trim().isEmpty()) {
            for (final String configFile : configFilePaths.split(",")) {
                filePathList.add(configFile.trim());
            }
        }
        return filePathList;
    }

    @Override
    public Map<IcebergCatalogProperty, Object> getCatalogProperties() {
        return catalogProperties;
    }
}
