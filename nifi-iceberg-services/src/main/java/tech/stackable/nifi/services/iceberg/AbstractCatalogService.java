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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.controller.ControllerServiceInitializationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.aws.credentials.provider.service.AWSCredentialsProviderService;
import org.apache.nifi.reporting.InitializationException;

import java.util.*;

/**
 * Abstract class holding common properties and methods for Catalog Service implementations.
 */
public abstract class AbstractCatalogService extends AbstractControllerService implements IcebergCatalogService {
    private static final PropertyDescriptor WAREHOUSE_LOCATION = new PropertyDescriptor.Builder()
            .name("warehouse-location")
            .displayName("Default Warehouse Location")
            .description("Location of default database for the warehouse.")
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .required(true)
            .build();

    private static final PropertyDescriptor S3_ENDPOINT_OVERRIDE = new PropertyDescriptor.Builder()
            .name("s3-endpoint-override")
            .displayName("S3 endpoint override")
            .description("Custom S3 endpoint in the format of an URL")
            .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
            .addValidator(StandardValidators.URL_VALIDATOR)
            .required(false)
            .build();

    private static final PropertyDescriptor S3_PATH_STYLE_ACCESS = new PropertyDescriptor.Builder()
            .name("s3-path-style-access")
            .displayName("S3 path style access")
            .description("Enable S3 path style access by disabling the default virtual hosting behaviour")
            .allowableValues("false", "true")
            .required(false)
            .build();

    private static final PropertyDescriptor S3_AWS_CREDENTIALS_PROVIDER_SERVICE = new PropertyDescriptor.Builder()
            .name("AWS Credentials Provider service")
            .displayName("AWS Credentials Provider Service")
            .description("The Controller Service that is used to obtain AWS credentials provider")
            .required(true)
            .identifiesControllerService(AWSCredentialsProviderService.class)
            .build();

    protected static final List<PropertyDescriptor> COMMON_PROPERTIES = List.of(
            WAREHOUSE_LOCATION,
            S3_ENDPOINT_OVERRIDE,
            S3_PATH_STYLE_ACCESS,
            S3_AWS_CREDENTIALS_PROVIDER_SERVICE
    );

    protected Map<IcebergCatalogProperty, Object> catalogProperties = new HashMap<>();

    protected void handle_common_properties(final ConfigurationContext context) {
        if (context.getProperty(WAREHOUSE_LOCATION).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.WAREHOUSE_LOCATION, context.getProperty(WAREHOUSE_LOCATION).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(S3_ENDPOINT_OVERRIDE).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.S3_ENDPOINT_OVERRIDE, context.getProperty(S3_ENDPOINT_OVERRIDE).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(S3_PATH_STYLE_ACCESS).isSet()) {
            catalogProperties.put(IcebergCatalogProperty.S3_PATH_STYLE_ACCESS, context.getProperty(S3_PATH_STYLE_ACCESS).evaluateAttributeExpressions().getValue());
        }

        if (context.getProperty(S3_AWS_CREDENTIALS_PROVIDER_SERVICE).isSet()) {
            final AWSCredentialsProviderService awsCredentialsProviderService = context.getProperty(S3_AWS_CREDENTIALS_PROVIDER_SERVICE).asControllerService(AWSCredentialsProviderService.class);
            final AWSCredentialsProvider credentialsProvider = awsCredentialsProviderService.getCredentialsProvider();
            final AWSCredentials credentials = credentialsProvider.getCredentials();
            catalogProperties.put(IcebergCatalogProperty.S3_AWS_ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
            catalogProperties.put(IcebergCatalogProperty.S3_AWS_SECRET_ACCESS_KEY, credentials.getAWSSecretKey());
        }
    }

    @Override
    public Map<IcebergCatalogProperty, Object> getCatalogProperties() {
        return catalogProperties;
    }
}
