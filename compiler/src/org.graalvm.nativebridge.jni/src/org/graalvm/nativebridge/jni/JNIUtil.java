/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.nativebridge.jni;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.nativebridge.jni.JNI.JArray;
import org.graalvm.nativebridge.jni.JNI.JByteArray;
import org.graalvm.nativebridge.jni.JNI.JClass;
import org.graalvm.nativebridge.jni.JNI.JFieldID;
import org.graalvm.nativebridge.jni.JNI.JLongArray;
import org.graalvm.nativebridge.jni.JNI.JMethodID;
import org.graalvm.nativebridge.jni.JNI.JNIEnv;
import org.graalvm.nativebridge.jni.JNI.JObject;
import org.graalvm.nativebridge.jni.JNI.JObjectArray;
import org.graalvm.nativebridge.jni.JNI.JString;
import org.graalvm.nativebridge.jni.JNI.JThrowable;
import org.graalvm.nativebridge.jni.JNI.JValue;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.nativeimage.c.type.CShortPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import static org.graalvm.nativeimage.c.type.CTypeConversion.toCString;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.word.WordFactory;
import static org.graalvm.word.WordFactory.nullPointer;

/**
 * Helpers for calling org.graalvm.nativebridge.jni.JNI functions.
 */

public final class JNIUtil {

    private static final String CLASS_SERVICES = "jdk/vm/ci/services/Services";

    private static final String[] METHOD_GET_JVMCI_CLASS_LOADER = {
                    "getJVMCIClassLoader",
                    "()Ljava/lang/ClassLoader;"
    };
    private static final String[] METHOD_GET_PLATFORM_CLASS_LOADER = {
                    "getPlatformClassLoader",
                    "()Ljava/lang/ClassLoader;"
    };
    private static final String[] METHOD_LOAD_CLASS = {
                    "loadClass",
                    "(Ljava/lang/String;)Ljava/lang/Class;"
    };

    // Checkstyle: stop
    public static boolean IsSameObject(JNIEnv env, JObject ref1, JObject ref2) {
        traceJNI("IsSameObject");
        return env.getFunctions().getIsSameObject().call(env, ref1, ref2);
    }

    public static void DeleteLocalRef(JNIEnv env, JObject ref) {
        traceJNI("DeleteLocalRef");
        env.getFunctions().getDeleteLocalRef().call(env, ref);
    }

    public static int PushLocalFrame(JNIEnv env, int capacity) {
        traceJNI("PushLocalFrame");
        return env.getFunctions().getPushLocalFrame().call(env, capacity);
    }

    public static JObject PopLocalFrame(JNIEnv env, JObject result) {
        traceJNI("PopLocalFrame");
        return env.getFunctions().getPopLocalFrame().call(env, result);
    }

    public static JClass DefineClass(JNIEnv env, CCharPointer name, JObject loader, CCharPointer buf, int bufLen) {
        return env.getFunctions().getDefineClass().call(env, name, loader, buf, bufLen);
    }

    public static JClass FindClass(JNIEnv env, CCharPointer name) {
        traceJNI("FindClass");
        return env.getFunctions().getFindClass().call(env, name);
    }

    public static JClass GetObjectClass(JNIEnv env, JObject object) {
        traceJNI("GetObjectClass");
        return env.getFunctions().getGetObjectClass().call(env, object);
    }

    public static JMethodID GetStaticMethodID(JNIEnv env, JClass clazz, CCharPointer name, CCharPointer sig) {
        traceJNI("GetStaticMethodID");
        return env.getFunctions().getGetStaticMethodID().call(env, clazz, name, sig);
    }

    public static JMethodID GetMethodID(JNIEnv env, JClass clazz, CCharPointer name, CCharPointer sig) {
        traceJNI("GetMethodID");
        return env.getFunctions().getGetMethodID().call(env, clazz, name, sig);
    }

    public static JFieldID GetStaticFieldID(JNIEnv env, JClass clazz, CCharPointer name, CCharPointer sig) {
        traceJNI("GetStaticFieldID");
        return env.getFunctions().getGetStaticFieldID().call(env, clazz, name, sig);
    }

    public static JFieldID GetFieldID(JNIEnv env, JClass clazz, CCharPointer name, CCharPointer signature) {
        traceJNI("GetFieldID");
        return env.getFunctions().getGetFieldID().call(env, clazz, name, signature);
    }

    public static JObject GetStaticObjectField(JNIEnv env, JClass clazz, JFieldID fieldID) {
        traceJNI("GetFieldID");
        return env.getFunctions().getGetStaticObjectField().call(env, clazz, fieldID);
    }

    public static int GetIntField(JNIEnv env, JObject object, JFieldID fieldID) {
        traceJNI("GetIntField");
        return env.getFunctions().getGetIntField().call(env, object, fieldID);
    }

    public static JObjectArray NewObjectArray(JNIEnv env, int len, JClass componentClass, JObject initialElement) {
        traceJNI("NewObjectArray");
        return env.getFunctions().getNewObjectArray().call(env, len, componentClass, initialElement);
    }

    public static JByteArray NewByteArray(JNIEnv env, int len) {
        traceJNI("NewByteArray");
        return env.getFunctions().getNewByteArray().call(env, len);
    }

    public static JLongArray NewLongArray(JNIEnv env, int len) {
        traceJNI("NewLongArray");
        return env.getFunctions().getNewLongArray().call(env, len);
    }

    public static int GetArrayLength(JNIEnv env, JArray array) {
        traceJNI("GetArrayLength");
        return env.getFunctions().getGetArrayLength().call(env, array);
    }

    public static void SetObjectArrayElement(JNIEnv env, JObjectArray array, int index, JObject value) {
        traceJNI("SetObjectArrayElement");
        env.getFunctions().getSetObjectArrayElement().call(env, array, index, value);
    }

    public static JObject GetObjectArrayElement(JNIEnv env, JObjectArray array, int index) {
        traceJNI("GetObjectArrayElement");
        return env.getFunctions().getGetObjectArrayElement().call(env, array, index);
    }

    public static CLongPointer GetLongArrayElements(JNIEnv env, JLongArray array, JValue isCopy) {
        traceJNI("GetLongArrayElements");
        return env.getFunctions().getGetLongArrayElements().call(env, array, isCopy);
    }

    public static void ReleaseLongArrayElements(JNIEnv env, JLongArray array, CLongPointer elems, int mode) {
        traceJNI("ReleaseLongArrayElements");
        env.getFunctions().getReleaseLongArrayElements().call(env, array, elems, mode);
    }

    public static CCharPointer GetByteArrayElements(JNIEnv env, JByteArray array, JValue isCopy) {
        traceJNI("GetByteArrayElements");
        return env.getFunctions().getGetByteArrayElements().call(env, array, isCopy);
    }

    public static void ReleaseByteArrayElements(JNIEnv env, JByteArray array, CCharPointer elems, int mode) {
        traceJNI("ReleaseByteArrayElements");
        env.getFunctions().getReleaseByteArrayElements().call(env, array, elems, mode);
    }

    public static void Throw(JNIEnv env, JThrowable throwable) {
        traceJNI("Throw");
        env.getFunctions().getThrow().call(env, throwable);
    }

    public static boolean ExceptionCheck(JNIEnv env) {
        traceJNI("ExceptionCheck");
        return env.getFunctions().getExceptionCheck().call(env);
    }

    public static void ExceptionClear(JNIEnv env) {
        traceJNI("ExceptionClear");
        env.getFunctions().getExceptionClear().call(env);
    }

    public static void ExceptionDescribe(JNIEnv env) {
        traceJNI("ExceptionDescribe");
        env.getFunctions().getExceptionDescribe().call(env);
    }

    public static JThrowable ExceptionOccurred(JNIEnv env) {
        traceJNI("ExceptionOccurred");
        return env.getFunctions().getExceptionOccurred().call(env);
    }

    /**
     * Creates a new global reference.
     *
     * @param env the JNIEnv
     * @param ref JObject to create org.graalvm.nativebridge.jni.JNI global reference for
     * @param type type of the object, used only for tracing to distinguish global references
     * @return org.graalvm.nativebridge.jni.JNI global reference for given {@link JObject}
     */
    @SuppressWarnings("unchecked")
    public static <T extends JObject> T NewGlobalRef(JNIEnv env, T ref, String type) {
        traceJNI("NewGlobalRef");
        T res = (T) env.getFunctions().getNewGlobalRef().call(env, ref);
        if (tracingAt(3)) {
            trace(3, "New global reference for 0x%x of type %s -> 0x%x", ref.rawValue(), type, res.rawValue());
        }
        return res;
    }

    public static void DeleteGlobalRef(JNIEnv env, JObject ref) {
        traceJNI("DeleteGlobalRef");
        if (tracingAt(3)) {
            trace(3, "Delete global reference 0x%x", ref.rawValue());
        }
        env.getFunctions().getDeleteGlobalRef().call(env, ref);
    }

    public static VoidPointer GetDirectBufferAddress(JNIEnv env, JObject buf) {
        traceJNI("GetDirectBufferAddress");
        return env.getFunctions().getGetDirectBufferAddress().call(env, buf);
    }

    public static boolean IsInstanceOf(JNIEnv env, JObject obj, JClass clazz) {
        traceJNI("IsInstanceOf");
        return env.getFunctions().getIsInstanceOf().call(env, obj, clazz);
    }

    // Checkstyle: resume

    private static void traceJNI(String function) {
        trace(2, "%s->JNI: %s", getFeatureName(), function);
    }

    private JNIUtil() {
    }

    /**
     * Decodes a string in the HotSpot heap to a local {@link String}.
     */
    public static String createString(JNIEnv env, JString hsString) {
        if (hsString.isNull()) {
            return null;
        }
        int len = env.getFunctions().getGetStringLength().call(env, hsString);
        CShortPointer unicode = env.getFunctions().getGetStringChars().call(env, hsString, WordFactory.nullPointer());
        try {
            char[] data = new char[len];
            for (int i = 0; i < len; i++) {
                data[i] = (char) unicode.read(i);
            }
            return new String(data);
        } finally {
            env.getFunctions().getReleaseStringChars().call(env, hsString, unicode);
        }
    }

    /**
     * Creates a String in the HotSpot heap from {@code string}.
     */
    public static JString createHSString(JNIEnv env, String string) {
        if (string == null) {
            return WordFactory.nullPointer();
        }
        int len = string.length();
        CShortPointer buffer = UnmanagedMemory.malloc(len << 1);
        try {
            for (int i = 0; i < len; i++) {
                buffer.write(i, (short) string.charAt(i));
            }
            return env.getFunctions().getNewString().call(env, buffer, len);
        } finally {
            UnmanagedMemory.free(buffer);
        }
    }

    /**
     * Converts a fully qualified Java class name from Java source format (e.g.
     * {@code "java.lang.getString"}) to internal format (e.g. {@code "Ljava/lang/getString;"}.
     */
    public static String getInternalName(String fqn) {
        return "L" + getBinaryName(fqn) + ";";
    }

    /**
     * Converts a fully qualified Java class name from Java source format (e.g.
     * {@code "java.lang.getString"}) to binary format (e.g. {@code "java/lang/getString"}.
     */
    public static String getBinaryName(String fqn) {
        return fqn.replace('.', '/');
    }

    /**
     * Creates a JVM method signature as specified in the Sections 4.3.3 of the JVM Specification.
     */
    public static String encodeMethodSignature(Class<?> returnType, Class<?>... parameterTypes) {
        StringBuilder builder = new StringBuilder("(");
        for (Class<?> type : parameterTypes) {
            encodeType(type, builder);
        }
        builder.append(")");
        encodeType(returnType, builder);
        return builder.toString();
    }

    private static void encodeType(Class<?> type, StringBuilder buf) {
        String desc;
        if (type == boolean.class) {
            desc = "Z";
        } else if (type == byte.class) {
            desc = "B";
        } else if (type == char.class) {
            desc = "C";
        } else if (type == short.class) {
            desc = "S";
        } else if (type == int.class) {
            desc = "I";
        } else if (type == long.class) {
            desc = "J";
        } else if (type == float.class) {
            desc = "F";
        } else if (type == double.class) {
            desc = "D";
        } else if (type == void.class) {
            desc = "V";
        } else if (type.isArray()) {
            buf.append('[');
            encodeType(type.getComponentType(), buf);
            return;
        } else {
            desc = "L" + type.getName().replace('.', '/') + ";";
        }
        buf.append(desc);
    }

    /**
     * Returns a {@link JClass} for given binary name.
     */
    public static JClass findClass(JNIEnv env, String binaryName) {
        try (CTypeConversion.CCharPointerHolder name = CTypeConversion.toCString(binaryName)) {
            return JNIUtil.FindClass(env, name.get());
        }
    }

    /**
     * Finds a class in HotSpot heap using a given {@code ClassLoader}.
     *
     * @param env the {@code JNIEnv}
     * @param binaryName the class binary name
     */
    public static JClass findClass(JNIEnv env, JObject classLoader, String binaryName) {
        if (classLoader.isNull()) {
            throw new IllegalArgumentException("ClassLoader must be non null.");
        }
        trace(1, "%s->HS: findClass", getFeatureName());
        JMethodID findClassId = findMethod(env, JNIUtil.GetObjectClass(env, classLoader), false, false, METHOD_LOAD_CLASS[0], METHOD_LOAD_CLASS[1]);
        JValue params = StackValue.get(1, JValue.class);
        params.addressOf(0).setJObject(JNIUtil.createHSString(env, binaryName.replace('/', '.')));
        return (JClass) env.getFunctions().getCallObjectMethodA().call(env, classLoader, findClassId, params);
    }

    /**
     * Finds a class in HotSpot heap using org.graalvm.nativebridge.jni.JNI.
     *
     * @param env the {@code JNIEnv}
     * @param classLoader the class loader to find class in or {@link WordFactory#nullPointer() NULL
     *            pointer}.
     * @param binaryName the class binary name
     * @param required if {@code true} the {@link JNIExceptionWrapper} is thrown when the class is
     *            not found. If {@code false} the {@code NULL pointer} is returned when the class is
     *            not found.
     */
    public static JClass findClass(JNIEnv env, JObject classLoader, String binaryName, boolean required) {
        Class<? extends Throwable> allowedException = null;
        try {
            if (classLoader.isNonNull()) {
                allowedException = required ? null : ClassNotFoundException.class;
                return findClass(env, classLoader, binaryName);
            } else {
                allowedException = required ? null : NoClassDefFoundError.class;
                return findClass(env, binaryName);
            }
        } finally {
            if (allowedException != null) {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env, allowedException);
            } else {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env);
            }
        }
    }

    /**
     * Returns a ClassLoader used to load the compiler classes.
     */
    public static JObject getJVMCIClassLoader(JNIEnv env) {
        if (JavaVersionUtil.JAVA_SPEC <= 8) {
            JClass clazz;
            try (CTypeConversion.CCharPointerHolder className = CTypeConversion.toCString(CLASS_SERVICES)) {
                clazz = JNIUtil.FindClass(env, className.get());
            }
            if (clazz.isNull()) {
                throw new InternalError("No such class " + CLASS_SERVICES);
            }
            JMethodID getClassLoaderId = findMethod(env, clazz, true, true, METHOD_GET_JVMCI_CLASS_LOADER[0], METHOD_GET_JVMCI_CLASS_LOADER[1]);
            if (getClassLoaderId.isNull()) {
                throw new InternalError(String.format("Cannot find method %s in class %s.", METHOD_GET_JVMCI_CLASS_LOADER[0], CLASS_SERVICES));
            }
            return env.getFunctions().getCallStaticObjectMethodA().call(env, clazz, getClassLoaderId, nullPointer());
        } else {
            JClass clazz;
            try (CTypeConversion.CCharPointerHolder className = CTypeConversion.toCString(JNIUtil.getBinaryName(ClassLoader.class.getName()))) {
                clazz = JNIUtil.FindClass(env, className.get());
            }
            if (clazz.isNull()) {
                throw new InternalError("No such class " + ClassLoader.class.getName());
            }
            JMethodID getClassLoaderId = findMethod(env, clazz, true, true, METHOD_GET_PLATFORM_CLASS_LOADER[0], METHOD_GET_PLATFORM_CLASS_LOADER[1]);
            if (getClassLoaderId.isNull()) {
                throw new InternalError(String.format("Cannot find method %s in class %s.", METHOD_GET_PLATFORM_CLASS_LOADER[0], ClassLoader.class.getName()));
            }
            return env.getFunctions().getCallStaticObjectMethodA().call(env, clazz, getClassLoaderId, nullPointer());
        }
    }

    public static JMethodID findMethod(JNIEnv env, JClass clazz, boolean staticMethod, String methodName, String methodSignature) {
        return findMethod(env, clazz, staticMethod, false, methodName, methodSignature);
    }

    private static JMethodID findMethod(JNIEnv env, JClass clazz, boolean staticMethod, boolean optional,
                    String methodName, String methodSignature) {
        JMethodID result;
        try (CTypeConversion.CCharPointerHolder name = toCString(methodName); CTypeConversion.CCharPointerHolder sig = toCString(methodSignature)) {
            result = staticMethod ? GetStaticMethodID(env, clazz, name.get(), sig.get()) : GetMethodID(env, clazz, name.get(), sig.get());
            if (optional) {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env, NoSuchMethodError.class);
            } else {
                JNIExceptionWrapper.wrapAndThrowPendingJNIException(env);
            }
            return result;
        }
    }

    public static JFieldID findField(JNIEnv env, JClass clazz, boolean staticField, String fieldName, String fieldSignature) {
        JFieldID result;
        try (CTypeConversion.CCharPointerHolder name = toCString(fieldName); CTypeConversion.CCharPointerHolder sig = toCString(fieldSignature)) {
            result = staticField ? GetStaticFieldID(env, clazz, name.get(), sig.get()) : GetFieldID(env, clazz, name.get(), sig.get());
            JNIExceptionWrapper.wrapAndThrowPendingJNIException(env);
            return result;
        }
    }

    /*----------------- TRACING ------------------*/

    public static boolean tracingAt(int level) {
        return NativeBridgeSupport.getInstance().isTracingEnabled(level);
    }

    /**
     * Emits a trace line composed of {@code format} and {@code args} if the tracing level equal to
     * or greater than {@code level}.
     */
    public static void trace(int level, String format, Object... args) {
        NativeBridgeSupport.getInstance().trace(level, format, args);
    }

    public static void trace(int level, Throwable throwable) {
        NativeBridgeSupport.getInstance().trace(level, throwable);
    }

    static String getFeatureName() {
        return NativeBridgeSupport.getInstance().getFeatureName();
    }
}
