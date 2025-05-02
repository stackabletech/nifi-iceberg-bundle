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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.resource.ResourceCardinality;
import org.apache.nifi.components.resource.ResourceType;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.processors.aws.credentials.provider.service.AWSCredentialsProviderService;

/** Abstract class holding common properties and methods for Catalog Service implementations. */
public abstract class AbstractCatalogService extends AbstractControllerService
    implements IcebergCatalogService {

  public static final PropertyDescriptor WAREHOUSE_LOCATION =
      new PropertyDescriptor.Builder()
          .name("warehouse-location")
          .displayName("Default Warehouse Location")
          .description(
              "Location of default database for the warehouse, e.g. \"s3a://mybucket/lakehouse\".")
          .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
          .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
          .required(true)
          .build();

  private static final PropertyDescriptor S3_ENDPOINT =
      new PropertyDescriptor.Builder()
          .name("s3-endpoint")
          .displayName("S3 endpoint")
          .description("Custom S3 endpoint in the format of an URL, e.g. \"http://minio:9000\".")
          .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
          .addValidator(StandardValidators.URL_VALIDATOR)
          .required(false)
          .build();

  private static final PropertyDescriptor S3_PATH_STYLE_ACCESS =
      new PropertyDescriptor.Builder()
          .name("s3-path-style-access")
          .displayName("S3 path style access")
          .description(
              "Enable S3 path style access by disabling the default virtual hosting behavior")
          .allowableValues("false", "true")
          .required(false)
          .build();

  public static final PropertyDescriptor S3_AWS_CREDENTIALS_PROVIDER_SERVICE =
      new PropertyDescriptor.Builder()
          .name("AWS Credentials Provider service")
          .displayName("AWS Credentials Provider Service")
          .description("The Controller Service that is used to obtain AWS credentials provider")
          .required(true)
          .identifiesControllerService(AWSCredentialsProviderService.class)
          .build();

  public static final PropertyDescriptor HADOOP_CONFIGURATION_RESOURCES =
      new PropertyDescriptor.Builder()
          .name("hadoop-config-resources")
          .displayName("Hadoop Configuration Resources")
          .description(
              "A file, or comma separated list of files, which contain the Hadoop configuration (core-site.xml, etc.). "
                  + "This is needed to access HDFS, S3 can be configured using dedicated config properties, which take precedence over settings from the xml files. "
                  + "Without this, default configuration will be used.")
          .required(false)
          .identifiesExternalResource(ResourceCardinality.MULTIPLE, ResourceType.FILE)
          .expressionLanguageSupported(ExpressionLanguageScope.ENVIRONMENT)
          .build();

  protected static final List<PropertyDescriptor> COMMON_PROPERTIES =
      List.of(
          WAREHOUSE_LOCATION,
          S3_ENDPOINT,
          S3_PATH_STYLE_ACCESS,
          S3_AWS_CREDENTIALS_PROVIDER_SERVICE,
          HADOOP_CONFIGURATION_RESOURCES);

  protected Map<IcebergCatalogProperty, Object> catalogProperties = new HashMap<>();
  protected List<String> hadoopConfigFilePaths = new ArrayList<>();

  protected void handle_common_properties(final ConfigurationContext context) {
    if (context.getProperty(WAREHOUSE_LOCATION).isSet()) {
      catalogProperties.put(
          IcebergCatalogProperty.WAREHOUSE_LOCATION,
          context.getProperty(WAREHOUSE_LOCATION).evaluateAttributeExpressions().getValue());
    }

    if (context.getProperty(S3_ENDPOINT).isSet()) {
      catalogProperties.put(
          IcebergCatalogProperty.S3_ENDPOINT,
          context.getProperty(S3_ENDPOINT).evaluateAttributeExpressions().getValue());
    }

    if (context.getProperty(S3_PATH_STYLE_ACCESS).isSet()) {
      catalogProperties.put(
          IcebergCatalogProperty.S3_PATH_STYLE_ACCESS,
          context.getProperty(S3_PATH_STYLE_ACCESS).evaluateAttributeExpressions().getValue());
    }

    if (context.getProperty(S3_AWS_CREDENTIALS_PROVIDER_SERVICE).isSet()) {
      final AWSCredentialsProviderService awsCredentialsProviderService =
          context
              .getProperty(S3_AWS_CREDENTIALS_PROVIDER_SERVICE)
              .asControllerService(AWSCredentialsProviderService.class);
      final AWSCredentialsProvider credentialsProvider =
          awsCredentialsProviderService.getCredentialsProvider();
      final AWSCredentials credentials = credentialsProvider.getCredentials();
      catalogProperties.put(
          IcebergCatalogProperty.S3_AWS_ACCESS_KEY_ID, credentials.getAWSAccessKeyId());
      catalogProperties.put(
          IcebergCatalogProperty.S3_AWS_SECRET_ACCESS_KEY, credentials.getAWSSecretKey());
    }

    if (context.getProperty(HADOOP_CONFIGURATION_RESOURCES).isSet()) {
      hadoopConfigFilePaths =
          createFilePathList(
              context
                  .getProperty(HADOOP_CONFIGURATION_RESOURCES)
                  .evaluateAttributeExpressions()
                  .getValue());
    }
  }

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

  @Override
  public List<String> getHadoopConfigFilePaths() {
    return hadoopConfigFilePaths;
  }
}
