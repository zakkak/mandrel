/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.LLVMParserResult;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.ExternalLibrary;
import com.oracle.truffle.llvm.runtime.LLVMLanguage.Loader;

public final class DefaultLoader extends Loader {

    private volatile List<LLVMParserResult> cachedDefaultDependencies;
    private volatile ExternalLibrary[] cachedSulongLibraries;

    @Override
    public void loadDefaults(LLVMContext context, Path internalLibraryPath) {
        Runner.loadDefaults(context, this, context.getLanguage().getRawRunnerID(), internalLibraryPath);
    }

    @Override
    public CallTarget load(LLVMContext context, Source source, AtomicInteger id) {
        // per context, only one thread must do any parsing
        synchronized (context.getGlobalScope()) {
            return Runner.parse(context, this, id, source);
        }
    }

    List<LLVMParserResult> getCachedDefaultDependencies() {
        return cachedDefaultDependencies;
    }

    ExternalLibrary[] getCachedSulongLibraries() {
        return cachedSulongLibraries;
    }

    void setDefaultLibraries(ExternalLibrary[] defaultLibraries, List<LLVMParserResult> parserResults) {
        cachedDefaultDependencies = parserResults;
        cachedSulongLibraries = defaultLibraries;
    }
}
