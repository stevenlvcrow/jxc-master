package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.lang.reflect.Method;

final class ReflectionIdAccessor {

    private ReflectionIdAccessor() {
    }

    static Object getIdValue(Object target) {
        try {
            Method method = target.getClass().getMethod("getId");
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access id property for " + target.getClass().getName(), ex);
        }
    }
}

