/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.hackday;

import static java.util.Optional.ofNullable;

import java.util.Optional;

public class ConfigurationDescriptor extends ComponentDescriptor {

  private String extensionName;
  private ComponentDescriptor connection = null;

  public String getExtensionName() {
    return extensionName;
  }

  public void setExtensionName(String extensionName) {
    this.extensionName = extensionName;
  }

  public Optional<ComponentDescriptor> getConnection() {
    return ofNullable(connection);
  }

  public void setConnection(ComponentDescriptor connection) {
    this.connection = connection;
  }
}
