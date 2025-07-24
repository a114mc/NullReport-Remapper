/*
    NullReport-Remapper, srg mappings generator.
    Copyright (C) 2025  a114mc

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.omegalabs.mapper;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*; // 用于更方便地访问指令

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class NullReportRemapper {



    public static String SUSPICIOUS_PACKAGE_REGEX = "[^./]*(?:/[^./]*){3,}$";
    // 使用 Set<String> 来存储生成的 SRG 映射，避免重复
    public Set<String> generateMappings(InputStream classInputStream) throws IOException {
        Set<String> mappings = new LinkedHashSet<>();
        ClassReader classReader = new ClassReader(classInputStream);
        classReader.accept(new ClassScanVisitor(mappings), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return mappings;
    }

    public Set<String> generateMappingsFromJar(InputStream jarInputStream) throws IOException {
        Set<String> mappings = new LinkedHashSet<>();
        try (JarInputStream jis = new JarInputStream(jarInputStream)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    byte[] classBytes = jis.readAllBytes();
                    ClassReader classReader = new ClassReader(classBytes);
                    classReader.accept(new ClassScanVisitor(mappings), ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                jis.closeEntry();
            }
        }
        return mappings;
    }

    private static class ClassScanVisitor extends ClassVisitor {
        private String currentClassNameInternal; // 内部表示，例如 "lermitage/intellij/extra/icons/services/FacetsFinderService"
        private final Set<String> mappings;

        public ClassScanVisitor(Set<String> mappings) {
            super(Opcodes.ASM9);
            this.mappings = mappings;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.currentClassNameInternal = name; // 获取混淆后的类名（内部形式）
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            // 检查方法是否为 synthetic
            if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
                System.out.println("Synthetic method detected -> " + currentClassNameInternal + ":" + name +"#"+ descriptor);
                return new SyntheticMethodVisitor(Opcodes.ASM9, currentClassNameInternal, mappings);
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    private static class SyntheticMethodVisitor extends MethodVisitor {
        private final String obfuscatedClassNameInternal; // obfuscated name
        private final Set<String> mappings;

        public SyntheticMethodVisitor(int api, String obfuscatedClassNameInternal, Set<String> mappings) {
            super(api);
            this.obfuscatedClassNameInternal = obfuscatedClassNameInternal;
            this.mappings = mappings;
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof String ldcString) {
                if (ldcString.matches(SUSPICIOUS_PACKAGE_REGEX)) {
                    // Might be incorrect, use with caution

                    // SRG formatted text:
                    // CL: <obfuscated/pack/Name> <original/pack/ClassName>
                    String srgEntry = String.format("CL: %s %s",
                            obfuscatedClassNameInternal,
                            ldcString
                    );

                    if (!mappings.contains(srgEntry)) { // 避免重复添加
                        mappings.add(srgEntry);
                        System.out.println("  Sussy ldc string found -> '" + ldcString + "'");
                    }
                }
            }
            super.visitLdcInsn(value);
        }
    }
}
