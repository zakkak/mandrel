/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.graal.isolated;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import jdk.vm.ci.meta.ResolvedJavaField;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import org.graalvm.compiler.serviceprovider.UnencodedSpeculationReason;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;

import com.oracle.svm.core.c.function.CEntryPointOptions;
import com.oracle.svm.core.deopt.SubstrateSpeculationLog.SubstrateSpeculation;
import com.oracle.svm.core.util.VMError;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.SpeculationLog;

public final class IsolatedSpeculationLog extends IsolatedObjectProxy<SpeculationLog> implements SpeculationLog {
    public IsolatedSpeculationLog(ClientHandle<SpeculationLog> logHandle) {
        super(logHandle);
    }

    @Override
    public void collectFailedSpeculations() {
        collectFailedSpeculations0(IsolatedCompileContext.get().getClient(), handle);
    }

    static class SpeculationReasonInvocationHandler implements InvocationHandler {
        private final IsolatedSpeculationReasonEncoding receiver;

        SpeculationReasonInvocationHandler(IsolatedSpeculationReasonEncoding receiver) {
            this.receiver = receiver;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return IsolatedSpeculationReasonEncoding.class.getDeclaredMethod(method.getName(), method.getParameterTypes()).invoke(receiver, args);
        }
    }

    static class SpeculationReasonEncodingSupplier implements Supplier<Object> {
        private static final Class<?> PROXY_CLASS;
        private static final Constructor<?> CONSTRUCTOR;
        private static final Class<?> CLASS;
        private static final String CLASS_NAME = "jdk.vm.ci.meta.SpeculationLog$SpeculationReasonEncoding";

        static {
            Class<?> speculationReasonEncodingClass = null;
            try {
                speculationReasonEncodingClass = Class.forName(CLASS_NAME);
            } catch (ClassNotFoundException e) {
                VMError.shouldNotReachHere(CLASS_NAME +
                                " not found in JDK. UnencodedSpeculationReason should have been used as SpeculationReason");
            }
            CLASS = speculationReasonEncodingClass;
            assert CLASS != null;
            PROXY_CLASS = Proxy.newProxyInstance(CLASS.getClassLoader(), new Class<?>[]{CLASS},
                            new SpeculationReasonInvocationHandler(new IsolatedSpeculationReasonEncoding())).getClass();
            Constructor<?> declaredConstructor = null;
            try {
                declaredConstructor = PROXY_CLASS.getDeclaredConstructor(CLASS);
            } catch (NoSuchMethodException e) {
                VMError.shouldNotReachHere("Failed to get constructor for " + PROXY_CLASS);
            }
            CONSTRUCTOR = declaredConstructor;
        }

        @Override
        public Object get() {
            try {
                return CONSTRUCTOR.newInstance(new SpeculationReasonInvocationHandler(new IsolatedSpeculationReasonEncoding()));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                VMError.shouldNotReachHere("Failed to instantiate " + CLASS_NAME + "through proxy", e);
            }
            return null;
        }
    }

    private byte[] encodeAsByteArray(SpeculationReason reason) {
        byte[] bytes = null;
        if (reason instanceof UnencodedSpeculationReason) {
            IsolatedSpeculationReasonEncoding encoding = encode((UnencodedSpeculationReason) reason);
            bytes = encoding.getByteArray();
        } else {
            try {
                final Method encode = Class.forName("jdk.vm.ci.meta.EncodedSpeculationReason")
                                .getDeclaredMethod("encode", Supplier.class);
                IsolatedSpeculationReasonEncoding encoding = (IsolatedSpeculationReasonEncoding) encode.invoke(reason, new SpeculationReasonEncodingSupplier());
                bytes = encoding.getByteArray();
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                VMError.shouldNotReachHere("Failed to invoke \"jdk.vm.ci.meta.EncodedSpeculationReason#encode\" through reflection", e);
            }
        }
        return bytes;
    }

    /** Adapted from {@code jdk.vm.ci.meta.EncodedSpeculationReason#encode}. */
    public IsolatedSpeculationReasonEncoding encode(UnencodedSpeculationReason reason) {
        IsolatedSpeculationReasonEncoding encoding = new IsolatedSpeculationReasonEncoding();
        try {
            int groupId = UnencodedSpeculationReason.class.getDeclaredField("groupId").getInt(reason);
            Object[] context = (Object[]) UnencodedSpeculationReason.class.getDeclaredField("context").get(reason);
            encoding.addInt(groupId);
            for (Object o : context) {
                if (o == null) {
                    encoding.addInt(0);
                } else {
                    addNonNullObject(encoding, o);
                }
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            VMError.shouldNotReachHere("Failed to encode speculation reason", e);
        }
        return encoding;
    }

    /** Copied from {@code jdk.vm.ci.meta.EncodedSpeculationReason#addNonNullObject}. */
    static void addNonNullObject(IsolatedSpeculationReasonEncoding encoding, Object o) {
        Class<?> c = o.getClass();
        if (c == String.class) {
            encoding.addString((String) o);
        } else if (c == Byte.class) {
            encoding.addByte((Byte) o);
        } else if (c == Short.class) {
            encoding.addShort((Short) o);
        } else if (c == Character.class) {
            encoding.addShort((Character) o);
        } else if (c == Integer.class) {
            encoding.addInt((Integer) o);
        } else if (c == Long.class) {
            encoding.addLong((Long) o);
        } else if (c == Float.class) {
            encoding.addInt(Float.floatToRawIntBits((Float) o));
        } else if (c == Double.class) {
            encoding.addLong(Double.doubleToRawLongBits((Double) o));
        } else if (o instanceof Enum) {
            encoding.addInt(((Enum<?>) o).ordinal());
        } else if (o instanceof ResolvedJavaMethod) {
            encoding.addMethod((ResolvedJavaMethod) o);
        } else if (o instanceof ResolvedJavaType) {
            encoding.addType((ResolvedJavaType) o);
        } else if (o instanceof ResolvedJavaField) {
            encoding.addField((ResolvedJavaField) o);
        } else {
            throw new IllegalArgumentException("Unsupported type for encoding: " + c.getName());
        }
    }

    @Override
    public boolean maySpeculate(SpeculationReason reason) {
        byte[] bytes = encodeAsByteArray(reason);
        try (PinnedObject pinnedBytes = PinnedObject.create(bytes)) {
            return maySpeculate0(IsolatedCompileContext.get().getClient(), handle, pinnedBytes.addressOfArrayElement(0), bytes.length);
        }
    }

    @Override
    public Speculation speculate(SpeculationReason reason) {
        byte[] bytes = encodeAsByteArray(reason);
        ClientHandle<SpeculationReason> reasonHandle;
        try (PinnedObject pinnedBytes = PinnedObject.create(bytes)) {
            reasonHandle = speculate0(IsolatedCompileContext.get().getClient(), handle, pinnedBytes.addressOfArrayElement(0), bytes.length);
        }
        return new SubstrateSpeculation(new IsolatedSpeculationReason(reasonHandle));
    }

    @Override
    public boolean hasSpeculations() {
        return hasSpeculations0(IsolatedCompileContext.get().getClient(), handle);
    }

    @Override
    public Speculation lookupSpeculation(JavaConstant constant) {
        throw VMError.shouldNotReachHere("not required");
    }

    @CEntryPoint
    @CEntryPointOptions(include = CEntryPointOptions.NotIncludedAutomatically.class, publishAs = CEntryPointOptions.Publish.NotPublished)
    private static void collectFailedSpeculations0(@SuppressWarnings("unused") ClientIsolateThread client, ClientHandle<SpeculationLog> logHandle) {
        IsolatedCompileClient.get().unhand(logHandle).collectFailedSpeculations();
    }

    @CEntryPoint
    @CEntryPointOptions(include = CEntryPointOptions.NotIncludedAutomatically.class, publishAs = CEntryPointOptions.Publish.NotPublished)
    private static boolean hasSpeculations0(@SuppressWarnings("unused") ClientIsolateThread client, ClientHandle<SpeculationLog> logHandle) {
        return IsolatedCompileClient.get().unhand(logHandle).hasSpeculations();
    }

    @CEntryPoint
    @CEntryPointOptions(include = CEntryPointOptions.NotIncludedAutomatically.class, publishAs = CEntryPointOptions.Publish.NotPublished)
    private static boolean maySpeculate0(@SuppressWarnings("unused") ClientIsolateThread client, ClientHandle<SpeculationLog> logHandle, PointerBase arrayData, int length) {
        byte[] bytes = new byte[length];
        ByteBuffer.wrap(bytes).put(CTypeConversion.asByteBuffer(arrayData, length));
        SpeculationLog log = IsolatedCompileClient.get().unhand(logHandle);
        return log.maySpeculate(new EncodedSpeculationReason(bytes));
    }

    @CEntryPoint
    @CEntryPointOptions(include = CEntryPointOptions.NotIncludedAutomatically.class, publishAs = CEntryPointOptions.Publish.NotPublished)
    private static ClientHandle<SpeculationReason> speculate0(@SuppressWarnings("unused") ClientIsolateThread client, ClientHandle<SpeculationLog> logHandle, PointerBase arrayData, int length) {
        byte[] bytes = new byte[length];
        ByteBuffer.wrap(bytes).put(CTypeConversion.asByteBuffer(arrayData, length));
        SpeculationLog log = IsolatedCompileClient.get().unhand(logHandle);
        EncodedSpeculationReason encodedReason = new EncodedSpeculationReason(bytes);
        Speculation speculation = log.speculate(encodedReason);
        /*
         * We can't pass the Speculation instance to the compilation isolate in a useful way, but we
         * only need a handle to the SpeculationReason so we can create Speculation objects that are
         * equal to that one in both the compiler isolate and in the client isolate.
         */
        SpeculationReason reason = speculation.getReason();
        assert speculation.equals(new SubstrateSpeculation(reason));
        return IsolatedCompileClient.get().hand(reason);
    }
}
