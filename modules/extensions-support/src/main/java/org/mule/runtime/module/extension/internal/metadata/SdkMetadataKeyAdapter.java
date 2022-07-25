/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import org.mule.runtime.api.metadata.MetadataProperty;
import org.mule.sdk.api.metadata.MetadataKey;

import java.util.Optional;
import java.util.Set;

/**
 * Adapts a {@link org.mule.sdk.api.metadata.MetadataKey} into a {@link org.mule.runtime.api.metadata.MetadataKey}
 *
 * @since 4.5.0
 */
public class SdkMetadataKeyAdapter implements MetadataKey {

  private final org.mule.runtime.api.metadata.MetadataKey delegate;

  public SdkMetadataKeyAdapter(org.mule.runtime.api.metadata.MetadataKey delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T extends MetadataProperty> Optional<T> getMetadataProperty(Class<T> propertyType) {
    return delegate.getMetadataProperty(propertyType);
  }

  @Override
  public Set<MetadataProperty> getProperties() {
    return delegate.getProperties();
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public String getDisplayName() {
    return delegate.getDisplayName();
  }

  @Override
  public Set<MetadataKey> getChilds() {
    return null;
  }

  @Override
  public String getPartName() {
    return delegate.getPartName();
  }
}
