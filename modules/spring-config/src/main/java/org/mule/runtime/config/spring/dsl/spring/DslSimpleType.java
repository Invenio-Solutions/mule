/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static com.google.common.collect.ImmutableSet.of;

import java.util.Set;

/**
 * Simple type values for DSL processing.
 *
 * @since 4.0
 */
public class DslSimpleType
{

    private final static Set<Class<?>> simpleTypes = of(Byte.class, byte.class,
                                                        Short.class, short.class,
                                                        Integer.class, int.class,
                                                        Long.class, long.class,
                                                        Character.class, char.class,
                                                        Float.class, float.class,
                                                        Double.class, double.class,
                                                        String.class);

    /**
     * @param type the value type.
     * @return true if the type is a primitive type, a primitive type wrapper class, string or an enum.
     */
    public static boolean isSimpleType(Class<?> type)
    {
        return simpleTypes.contains(type) || type.isEnum();
    }

}
