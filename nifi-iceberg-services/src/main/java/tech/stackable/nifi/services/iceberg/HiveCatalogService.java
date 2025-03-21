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

import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Tags({"iceberg", "catalog", "service", "metastore", "hive"})
@CapabilityDescription("Catalog service that connects to a Hive metastore to keep track of Iceberg tables.")
public class HiveCatalogService extends AbstractCatalogService {

    public static final PropertyDescriptor METASTORE_URI = new PropertyDescriptor.Builder()
            .name("hive-metastore-uri")
            .displayName("Hive Metastore URI")
            .description("The URI location(s) for the Hive metastore; note that this is not the location of the Hive Server.")
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .addValidator(StandardValidators.URI_LIST_VALIDATOR)
            .required(true)
            .build();

    private static final List<PropertyDescriptor> PROPERTIES = List.of(METASTORE_URI, WAREHOUSE_LOCATION, S3_ENDPOINT_OVERRIDE, S3_PATH_STYLE_ACCESS);

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) {
        if (context.getProperty(METASTORE_URI).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.METASTORE_URI, context.getProperty(METASTORE_URI).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(WAREHOUSE_LOCATION).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.WAREHOUSE_LOCATION, context.getProperty(WAREHOUSE_LOCATION).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(S3_ENDPOINT_OVERRIDE).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.S3_ENDPOINT_OVERRIDE, context.getProperty(S3_ENDPOINT_OVERRIDE).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(S3_PATH_STYLE_ACCESS).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.S3_PATH_STYLE_ACCESS, context.getProperty(S3_PATH_STYLE_ACCESS).evaluateAttributeExpressions().getValue());
        }
    }

    @Override
    public IcebergCatalogType getCatalogType() {
        return IcebergCatalogType.HIVE;
    }
}
