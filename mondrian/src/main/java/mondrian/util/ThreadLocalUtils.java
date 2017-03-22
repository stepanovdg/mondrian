/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package mondrian.util;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * User: Dzmitry Stsiapanau
 * Date: 03/22/2017
 * Time: 16:28
 */

public class ThreadLocalUtils {


    public static void restoreOldThreadLocals(Reference<?>[] copyOfThreadLocals, Reference<?>[] copyOfInheritableThreadLocals) {
        restoreOldThreadLocal(copyOfThreadLocals);
        restoreOldInheritableThreadLocal(copyOfInheritableThreadLocals);
    }

    public static void restoreOldThreadLocal(Reference<?>[] copyOfThreadLocals) {
        new ThreadLocal<Object>().get();
        restore(threadLocalsField, copyOfThreadLocals);
    }

    public static void restoreOldInheritableThreadLocal(Reference<?>[] copyOfInheritableThreadLocals) {
        new InheritableThreadLocal<Object>().get();
        restore(inheritableThreadLocalsField, copyOfInheritableThreadLocals);
    }

    private static void restore(Field field, Object value) {
        try {
            Thread thread = Thread.currentThread();
            if (value == null) {
                field.set(thread, null);
            } else {
                tableField.set(field.get(thread), value);
                Object[] array = (Object[]) value;
                int size = 0;
                for (int i = 0; i < array.length; i++) {
                    if (array[i] != null) {
                        size++;
                    }
                }
                sizeField.set(field.get(thread), size);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access denied", e);
        }
    }

    /* Reflection fields */

    private static final Field threadLocalsField;

    private static final Field inheritableThreadLocalsField;
    private static final Class<?> threadLocalMapClass;
    private static final Field tableField;
    private static final Field sizeField;
    private static final Class<?> threadLocalMapEntryClass;

    static {
        try {
            threadLocalsField = field(Thread.class, "threadLocals");
            inheritableThreadLocalsField = field(Thread.class, "inheritableThreadLocals");
            threadLocalMapClass = inner(ThreadLocal.class, "ThreadLocalMap");
            tableField = field(threadLocalMapClass, "table");
            sizeField = field(threadLocalMapClass, "size");
            threadLocalMapEntryClass = inner(threadLocalMapClass, "Entry");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                    "Could not locate threadLocals field in Thread.  " +
                            "Will not be able to clear thread locals: " + e);
        }
    }

    private static Class<?> inner(Class<?> clazz, String name) {
        for (Class<?> c : clazz.getDeclaredClasses()) {
            if (c.getSimpleName().equals(name)) {
                return c;
            }
        }
        throw new IllegalStateException(
                "Could not find inner class " + name + " in " + clazz);
    }

    private static Field field(Class<?> c, String name)
            throws NoSuchFieldException {
        Field field = c.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static Reference<?>[] copyInheritableThreadLocal() {
        return copy(inheritableThreadLocalsField);
    }

    public static Reference<?>[] copyThreadLocal() {
        return copy(threadLocalsField);
    }

    private static Reference<?>[] copy(Field field) {
        try {
            Thread thread = Thread.currentThread();
            Object threadLocals = field.get(thread);
            if (threadLocals == null) return null;
            Reference<?>[] table =
                    (Reference<?>[]) tableField.get(threadLocals);
            return Arrays.copyOf(table, table.length);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Access denied", e);
        }
    }
}

