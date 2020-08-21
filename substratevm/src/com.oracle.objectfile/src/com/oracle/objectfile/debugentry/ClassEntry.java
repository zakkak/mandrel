/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2020, Red Hat Inc. All rights reserved.
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

package com.oracle.objectfile.debugentry;

import com.oracle.objectfile.debuginfo.DebugInfoProvider.DebugFieldInfo;
import com.oracle.objectfile.debuginfo.DebugInfoProvider.DebugFrameSizeChange;
import com.oracle.objectfile.debuginfo.DebugInfoProvider.DebugTypeInfo;
import com.oracle.objectfile.debuginfo.DebugInfoProvider.DebugInstanceTypeInfo;
import com.oracle.objectfile.debuginfo.DebugInfoProvider.DebugTypeInfo.DebugTypeKind;
import org.graalvm.compiler.debug.DebugContext;

import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Track debug info associated with a Java class.
 */
public class ClassEntry extends TypeEntry {
    /**
     * Details of this class's superclass.
     */
    protected ClassEntry superClass;
    /**
     * Details of this class's interfaces.
     */
    protected LinkedList<InterfaceClassEntry> interfaces;
    /**
     * Details of fields located in this instance.
     */
    protected LinkedList<FieldEntry> fields;
    /**
     * Details of the associated file.
     */
    private FileEntry fileEntry;
    /**
     * A list recording details of all primary ranges included in this class sorted by ascending
     * address range.
     */
    private LinkedList<PrimaryEntry> primaryEntries;
    /**
     * An index identifying primary ranges which have already been encountered.
     */
    private Map<Range, PrimaryEntry> primaryIndex;
    /**
     * An index of all primary and secondary files referenced from this class's compilation unit.
     */
    private Map<FileEntry, Integer> localFilesIndex;
    /**
     * a list of the same files.
     */
    private LinkedList<FileEntry> localFiles;
    /**
     * An index of all primary and secondary dirs referenced from this class's compilation unit.
     */
    private HashMap<DirEntry, Integer> localDirsIndex;
    /**
     * A list of the same dirs.
     */
    private LinkedList<DirEntry> localDirs;
    /**
     * index of debug_info section compilation unit for this class.
     */
    private int cuIndex;
    /**
     * index of debug_info section compilation unit for deopt target methods.
     */
    private int deoptCUIndex;
    /**
     * index into debug_line section for associated compilation unit.
     */
    private int lineIndex;
    /**
     * Size of line number info prologue region for associated compilation unit.
     */
    private int linePrologueSize;
    /**
     * Total size of line number info region for associated compilation unit.
     */
    private int totalSize;

    /**
     * true iff the entry includes methods that are deopt targets.
     */
    private boolean includesDeoptTarget;

    public ClassEntry(String className, FileEntry fileEntry, int size) {
        super(className, size);
        this.interfaces = new LinkedList<>();
        this.fields = new LinkedList<>();
        this.fileEntry = fileEntry;
        this.primaryEntries = new LinkedList<>();
        this.primaryIndex = new HashMap<>();
        this.localFiles = new LinkedList<>();
        this.localFilesIndex = new HashMap<>();
        this.localDirs = new LinkedList<>();
        this.localDirsIndex = new HashMap<>();
        if (fileEntry != null) {
            localFiles.add(fileEntry);
            localFilesIndex.put(fileEntry, localFiles.size());
            DirEntry dirEntry = fileEntry.getDirEntry();
            if (dirEntry != null) {
                localDirs.add(dirEntry);
                localDirsIndex.put(dirEntry, localDirs.size());
            }
        }
        this.cuIndex = -1;
        this.deoptCUIndex = -1;
        this.lineIndex = -1;
        this.linePrologueSize = -1;
        this.totalSize = -1;
        this.includesDeoptTarget = false;
    }

    public void addPrimary(Range primary, List<DebugFrameSizeChange> frameSizeInfos, int frameSize) {
        if (primaryIndex.get(primary) == null) {
            PrimaryEntry primaryEntry = new PrimaryEntry(primary, frameSizeInfos, frameSize, this);
            primaryEntries.add(primaryEntry);
            primaryIndex.put(primary, primaryEntry);
            if (primary.isDeoptTarget()) {
                includesDeoptTarget = true;
            } else {
                /* deopt targets should all come after normal methods */
                assert includesDeoptTarget == false;
            }
        }
    }

    public void addSubRange(Range subrange, FileEntry subFileEntry) {
        Range primary = subrange.getPrimary();
        /*
         * the subrange should belong to a primary range
         */
        assert primary != null;
        PrimaryEntry primaryEntry = primaryIndex.get(primary);
        /*
         * we should already have seen the primary range
         */
        assert primaryEntry != null;
        assert primaryEntry.getClassEntry() == this;
        primaryEntry.addSubRange(subrange, subFileEntry);
        if (subFileEntry != null) {
            if (localFilesIndex.get(subFileEntry) == null) {
                localFiles.add(subFileEntry);
                localFilesIndex.put(subFileEntry, localFiles.size());
            }
            DirEntry dirEntry = subFileEntry.getDirEntry();
            if (dirEntry != null && localDirsIndex.get(dirEntry) == null) {
                localDirs.add(dirEntry);
                localDirsIndex.put(dirEntry, localDirs.size());
            }
        }
    }

    public int localDirsIdx(DirEntry dirEntry) {
        if (dirEntry != null) {
            return localDirsIndex.get(dirEntry);
        } else {
            return 0;
        }
    }

    public int localFilesIdx(@SuppressWarnings("hiding") FileEntry fileEntry) {
        return localFilesIndex.get(fileEntry);
    }

    public String getFileName() {
        if (fileEntry != null) {
            return fileEntry.getFileName();
        } else {
            return "";
        }
    }

    @SuppressWarnings("unused")
    String getFullFileName() {
        if (fileEntry != null) {
            return fileEntry.getFullName();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    String getDirName() {
        if (fileEntry != null) {
            return fileEntry.getPathName();
        } else {
            return "";
        }
    }

    public void setCUIndex(int cuIndex) {
        // Should only get set once to a non-negative value.
        assert cuIndex >= 0;
        assert this.cuIndex == -1;
        this.cuIndex = cuIndex;
    }

    public int getCUIndex() {
        // Should have been set before being read.
        assert cuIndex >= 0;
        return cuIndex;
    }

    public void setDeoptCUIndex(int deoptCUIndex) {
        // Should only get set once to a non-negative value.
        assert deoptCUIndex >= 0;
        assert this.deoptCUIndex == -1;
        this.deoptCUIndex = deoptCUIndex;
    }

    public int getDeoptCUIndex() {
        // Should have been set before being read.
        assert deoptCUIndex >= 0;
        return deoptCUIndex;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public void setLineIndex(int lineIndex) {
        this.lineIndex = lineIndex;
    }

    public void setLinePrologueSize(int linePrologueSize) {
        this.linePrologueSize = linePrologueSize;
    }

    public int getLinePrologueSize() {
        return linePrologueSize;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public FileEntry getFileEntry() {
        return fileEntry;
    }

    public LinkedList<PrimaryEntry> getPrimaryEntries() {
        return primaryEntries;
    }

    public Object primaryIndexFor(Range primaryRange) {
        return primaryIndex.get(primaryRange);
    }

    public LinkedList<DirEntry> getLocalDirs() {
        return localDirs;
    }

    public LinkedList<FileEntry> getLocalFiles() {
        return localFiles;
    }

    public boolean includesDeoptTarget() {
        return includesDeoptTarget;
    }

    public String getCachePath() {
        if (fileEntry != null) {
            Path cachePath = fileEntry.getCachePath();
            if (cachePath != null) {
                return cachePath.toString();
            }
        }
        return "";
    }

    @Override
    public DebugTypeKind typeKind() {
        return DebugTypeKind.INSTANCE;
    }

    @Override
    public boolean isClass() {
        return true;
    }

    @Override
    public void addDebugInfo(DebugInfoBase debugInfoBase, DebugTypeInfo debugTypeInfo, DebugContext debugContext) {
        assert TypeEntry.canonicalize(debugTypeInfo.typeName()).equals(typeName);
        DebugInstanceTypeInfo debugInstanceTypeInfo = (DebugInstanceTypeInfo) debugTypeInfo;
        /* Add details of super and interface classes */
        String superName = debugInstanceTypeInfo.superName();
        if (superName != null) {
            superName = superName.replace("$", ".");
        }
        debugContext.log("typename %s adding super %s\n", typeName, superName);
        if (superName != null) {
            this.superClass = debugInfoBase.lookupClassEntry(superName);
        }
        debugInstanceTypeInfo.interfaces().forEach(interfaceName -> processInterface(interfaceName, debugInfoBase, debugContext));
        /* Add details of fields and field types */
        debugInstanceTypeInfo.fieldInfoProvider().forEach(debugFieldInfo -> this.processField(debugFieldInfo, debugInfoBase, debugContext));
    }

    private void processInterface(String interfaceName, DebugInfoBase debugInfoBase, DebugContext debugContext) {
        debugContext.log("typename %s adding interface %s\n", typeName, interfaceName);
        ClassEntry entry = debugInfoBase.lookupClassEntry(interfaceName.replace("$", "."));
        assert entry instanceof InterfaceClassEntry;
        InterfaceClassEntry interfaceClassEntry = (InterfaceClassEntry) entry;
        interfaces.add(interfaceClassEntry);
        interfaceClassEntry.addImplementor(this, debugContext);
    }

    private void processField(DebugFieldInfo debugFieldInfo, DebugInfoBase debugInfoBase, DebugContext debugContext) {
        String fieldName = debugFieldInfo.name();
        String valueTypeName = TypeEntry.canonicalize(debugFieldInfo.valueType());
        int size = debugFieldInfo.size();
        int offset = debugFieldInfo.offset();
        int modifiers = debugFieldInfo.modifiers();
        debugContext.log("typename %s adding %s field %s type %s size %s at offset %d\n",
                        typeName, memberModifiers(modifiers), fieldName, valueTypeName, size, offset);
        TypeEntry valueType = debugInfoBase.lookupTypeEntry(valueTypeName);
        fields.add(new FieldEntry(fieldName, this, valueType, size, offset, modifiers));
    }

    private String memberModifiers(int modifiers) {
        StringBuilder builder = new StringBuilder();
        if (Modifier.isPublic(modifiers)) {
            builder.append("public ");
        } else if (Modifier.isProtected(modifiers)) {
            builder.append("protected ");
        } else if (Modifier.isPrivate(modifiers)) {
            builder.append("private ");
        }
        if (Modifier.isFinal(modifiers)) {
            builder.append("final ");
        }
        if (Modifier.isAbstract(modifiers)) {
            builder.append("abstract ");
        } else if (Modifier.isVolatile(modifiers)) {
            builder.append("volatile ");
        } else if (Modifier.isTransient(modifiers)) {
            builder.append("transient ");
        } else if (Modifier.isSynchronized(modifiers)) {
            builder.append("synchronized ");
        }
        if (Modifier.isNative(modifiers)) {
            builder.append("native ");
        }
        if (Modifier.isStatic(modifiers)) {
            builder.append("static");
        } else {
            builder.append("instance");
        }

        return builder.toString();
    }
}
