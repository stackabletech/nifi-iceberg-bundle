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
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;

import java.util.ArrayList;
import java.util.Collections;
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

    private static final List<PropertyDescriptor> PROPERTIES;
    static {
        List<PropertyDescriptor> properties = new ArrayList<>(COMMON_PROPERTIES);
        properties.add(METASTORE_URI);
        PROPERTIES = Collections.unmodifiableList(properties);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return PROPERTIES;
    }

    @OnEnabled
    public void onEnabled(final ConfigurationContext context) {
        handle_common_properties(context);

        if (context.getProperty(METASTORE_URI).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.METASTORE_URI, context.getProperty(METASTORE_URI).evaluateAttributeExpressions().getValue());
        }
    }

    @Override
    public IcebergCatalogType getCatalogType() {
        return IcebergCatalogType.HIVE;
    }
}
