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

import static org.apache.nifi.hadoop.SecurityUtil.getUgiForKerberosUser;
import static tech.stackable.nifi.processors.iceberg.IcebergUtils.findCause;
import static tech.stackable.nifi.processors.iceberg.IcebergUtils.getConfigurationFromFiles;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.nifi.annotation.behavior.RequiresInstanceClassLoading;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.ClassloaderIsolationKeyProvider;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.context.PropertyContext;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.hadoop.SecurityUtil;
import org.apache.nifi.kerberos.KerberosUserService;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.security.krb.KerberosUser;
import org.ietf.jgss.GSSException;
import tech.stackable.nifi.services.iceberg.IcebergCatalogService;

/** Base Iceberg processor class. */
@RequiresInstanceClassLoading(cloneAncestorResources = true)
public abstract class AbstractIcebergProcessor extends AbstractProcessor
    implements ClassloaderIsolationKeyProvider {

  public static final PropertyDescriptor CATALOG =
      new PropertyDescriptor.Builder()
          .name("catalog-service")
          .displayName("Catalog Service")
          .description(
              "Specifies the Controller Service to use for handling references to table’s metadata files.")
          .identifiesControllerService(IcebergCatalogService.class)
          .required(true)
          .build();

  protected static final PropertyDescriptor CATALOG_NAMESPACE =
      new PropertyDescriptor.Builder()
          .name("catalog-namespace")
          .displayName("Catalog Namespace")
          .description("The namespace of the catalog (sometimes also referred to as schema)")
          .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
          .required(true)
          .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
          .build();

  protected static final PropertyDescriptor TABLE_NAME =
      new PropertyDescriptor.Builder()
          .name("table-name")
          .displayName("Table Name")
          .description("The name of the Iceberg table to write to.")
          .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
          .required(true)
          .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
          .build();

  protected static final PropertyDescriptor KERBEROS_USER_SERVICE =
      new PropertyDescriptor.Builder()
          .name("kerberos-user-service")
          .displayName("Kerberos User Service")
          .description(
              "Specifies the Kerberos User Controller Service that should be used for authenticating with Kerberos." +
                      "This authentication can be used against Hive metastore as well as HDFS")
          .identifiesControllerService(KerberosUserService.class)
          .required(false)
          .build();

  public static final Relationship REL_FAILURE =
      new Relationship.Builder()
          .name("failure")
          .description(
              "A FlowFile is routed to this relationship if the operation failed and retrying the operation will also fail," +
                      "such as an invalid data or schema.")
          .build();

  protected static final AtomicReference<KerberosUser> kerberosUserReference =
      new AtomicReference<>();
  protected final AtomicReference<UserGroupInformation> ugiReference = new AtomicReference<>();

  @OnScheduled
  public void onScheduled(final ProcessContext context) {
    initKerberosCredentials(context);
  }

  protected synchronized void initKerberosCredentials(ProcessContext context) {
    final KerberosUserService kerberosUserService =
        context.getProperty(KERBEROS_USER_SERVICE).asControllerService(KerberosUserService.class);
    final IcebergCatalogService catalogService =
        context.getProperty(CATALOG).asControllerService(IcebergCatalogService.class);

    if (kerberosUserService != null) {
      KerberosUser kerberosUser;
      if (kerberosUserReference.get() == null) {
        kerberosUser = kerberosUserService.createKerberosUser();
      } else {
        kerberosUser = kerberosUserReference.get();
      }
      try {
        ugiReference.set(
            getUgiForKerberosUser(
                getConfigurationFromFiles(catalogService.getHadoopConfigFilePaths()),
                kerberosUser));
      } catch (IOException e) {
        throw new ProcessException("Kerberos authentication failed", e);
      }
      kerberosUserReference.set(kerberosUser);
    }
  }

  @Override
  public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {
    final FlowFile flowFile = session.get();
    if (flowFile == null) {
      return;
    }

    final KerberosUser kerberosUser = kerberosUserReference.get();
    if (kerberosUser == null) {
      doOnTrigger(context, session, flowFile);
    } else {
      try {
        getUgi()
            .doAs(
                (PrivilegedExceptionAction<Void>)
                    () -> {
                      doOnTrigger(context, session, flowFile);
                      return null;
                    });

      } catch (Exception e) {
        if (!handleAuthErrors(e, session, context)) {
          getLogger().error("Privileged action failed with kerberos user " + kerberosUser, e);
          session.transfer(session.penalize(flowFile), REL_FAILURE);
        }
      }
    }
  }

  protected abstract void doOnTrigger(
      ProcessContext context, ProcessSession session, FlowFile flowFile) throws ProcessException;

  @Override
  protected Collection<ValidationResult> customValidate(ValidationContext context) {
    final List<ValidationResult> problems = new ArrayList<>();
    final IcebergCatalogService catalogService =
        context.getProperty(CATALOG).asControllerService(IcebergCatalogService.class);
    boolean catalogServiceEnabled =
        context.getControllerServiceLookup().isControllerServiceEnabled(catalogService);

    if (catalogServiceEnabled) {
      final boolean kerberosUserServiceIsSet = context.getProperty(KERBEROS_USER_SERVICE).isSet();
      final boolean securityEnabled =
          SecurityUtil.isSecurityEnabled(
              getConfigurationFromFiles(catalogService.getHadoopConfigFilePaths()));

      if (securityEnabled && !kerberosUserServiceIsSet) {
        problems.add(
            new ValidationResult.Builder()
                .subject(KERBEROS_USER_SERVICE.getDisplayName())
                .valid(false)
                .explanation(
                    "'hadoop.security.authentication' is set to 'kerberos' in the hadoop configuration files but no KerberosUserService is configured.")
                .build());
      }

      if (!securityEnabled && kerberosUserServiceIsSet) {
        problems.add(
            new ValidationResult.Builder()
                .subject(KERBEROS_USER_SERVICE.getDisplayName())
                .valid(false)
                .explanation(
                    "KerberosUserService is configured but 'hadoop.security.authentication' is not set to 'kerberos' in the hadoop configuration files.")
                .build());
      }
    }

    return problems;
  }

  @Override
  public String getClassloaderIsolationKey(PropertyContext context) {
    try {
      final KerberosUserService kerberosUserService =
          context.getProperty(KERBEROS_USER_SERVICE).asControllerService(KerberosUserService.class);
      if (kerberosUserService != null) {
        final KerberosUser kerberosUser = kerberosUserService.createKerberosUser();
        return kerberosUser.getPrincipal();
      }
    } catch (IllegalStateException e) {
      // the Kerberos controller service is disabled, therefore this part of the isolation key
      // cannot be determined yet
    }

    return null;
  }

  private UserGroupInformation getUgi() {
    SecurityUtil.checkTGTAndRelogin(getLogger(), kerberosUserReference.get());
    return ugiReference.get();
  }

  protected boolean handleAuthErrors(Throwable t, ProcessSession session, ProcessContext context) {
    final Optional<GSSException> causeOptional =
        findCause(t, GSSException.class, gsse -> GSSException.NO_CRED == gsse.getMajor());
    if (causeOptional.isPresent()) {
      getLogger().info("No valid Kerberos credential found, retrying login", causeOptional.get());
      kerberosUserReference.get().checkTGTAndRelogin();
      session.rollback();
      context.yield();
      return true;
    }
    return false;
  }
}
