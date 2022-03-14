/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.runtime.nodes.vars;

import com.oracle.truffle.api.ContextThreadLocal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.IDGenerater.BitcodeID;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.LLVMLanguage.LLVMThreadLocalValue;
import com.oracle.truffle.llvm.runtime.memory.LLVMAllocateNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

public class AggregateTLGlobalInPlaceNode extends RootNode {

    @Child private AggregateLiteralInPlaceNode inPlaceNode;
    private final ContextThreadLocal<LLVMThreadLocalValue> contextThreadLocal;
    @Child private LLVMAllocateNode allocTLSection;
    private final BitcodeID bitcodeID;

    public AggregateTLGlobalInPlaceNode(LLVMLanguage llvmLanguage, AggregateLiteralInPlaceNode inPlaceNode, LLVMAllocateNode allocTLSection, BitcodeID bitcodeID) {
        super(llvmLanguage);
        this.contextThreadLocal = llvmLanguage.contextThreadLocal;
        this.inPlaceNode = inPlaceNode;
        this.allocTLSection = allocTLSection;
        this.bitcodeID = bitcodeID;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        assert frame.getArguments().length > 0;
        assert frame.getArguments()[0] instanceof Thread;
        Thread thread = (Thread) frame.getArguments()[0];
        LLVMPointer tlgBase = allocOrNull(allocTLSection);
        contextThreadLocal.get().addSection(tlgBase, bitcodeID);
        inPlaceNode.execute(frame, thread);
        return null;
    }

    private static LLVMPointer allocOrNull(LLVMAllocateNode allocNode) {
        if (allocNode != null) {
            return allocNode.executeWithTarget();
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return "TLS" + '/' + bitcodeID.getId();
    }
}
