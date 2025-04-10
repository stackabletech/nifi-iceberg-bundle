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

import com.google.common.base.Throwables;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.processor.ProcessContext;

public class IcebergUtils {
  /**
   * Collects every non-blank dynamic property from the context.
   *
   * @param context process context
   * @param flowFile FlowFile to evaluate attribute expressions
   * @return Map of dynamic properties
   */
  public static Map<String, String> getDynamicProperties(
      ProcessContext context, FlowFile flowFile) {
    return context.getProperties().entrySet().stream()
        // filter non-blank dynamic properties
        .filter(
            e ->
                e.getKey().isDynamic()
                    && StringUtils.isNotBlank(e.getValue())
                    && StringUtils.isNotBlank(
                        context
                            .getProperty(e.getKey())
                            .evaluateAttributeExpressions(flowFile)
                            .getValue()))
        // convert to Map keys and evaluated property values
        .collect(
            Collectors.toMap(
                e -> e.getKey().getName(),
                e ->
                    context
                        .getProperty(e.getKey())
                        .evaluateAttributeExpressions(flowFile)
                        .getValue()));
  }

  /**
   * Returns an optional with the first throwable in the causal chain that is assignable to the
   * provided cause type, and satisfies the provided cause predicate, {@link Optional#empty()}
   * otherwise.
   *
   * @param t The throwable to inspect for the cause.
   * @return Throwable Cause
   */
  public static <T extends Throwable> Optional<T> findCause(
      Throwable t, Class<T> expectedCauseType, Predicate<T> causePredicate) {
    return Throwables.getCausalChain(t).stream()
        .filter(expectedCauseType::isInstance)
        .map(expectedCauseType::cast)
        .filter(causePredicate)
        .findFirst();
  }
}
