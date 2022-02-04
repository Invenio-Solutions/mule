/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import java.util.HashMap;
import java.util.Map;

public class ExtensionExecutionRequest {

  private String configRef;
  private ConfigurationDescriptor configurationDescriptor;
  private String operation;
  private Map<String, Object> parameters = new HashMap<>();

  public String getConfigRef() {
    return configRef;
  }

  public void setConfigRef(String configRef) {
    this.configRef = configRef;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public ConfigurationDescriptor getConfigurationDescriptor() {
    return configurationDescriptor;
  }

  public void setConfigurationDescriptor(ConfigurationDescriptor configurationDescriptor) {
    this.configurationDescriptor = configurationDescriptor;
  }
}
