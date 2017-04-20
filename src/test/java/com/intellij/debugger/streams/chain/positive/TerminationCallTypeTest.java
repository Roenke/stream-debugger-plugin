/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.streams.chain.positive;

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.StreamChain;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vitaliy.Bibaev
 */
public class TerminationCallTypeTest extends StreamChainBuilderPositiveTestBase {
  public void testVoidType() throws Exception {
    doTest(GenericType.VOID);
  }

  public void testBooleanType() throws Exception {
    doTest(GenericType.BOOLEAN);
  }

  public void testIntType() throws Exception {
    doTest(GenericType.INT);
  }

  public void testDoubleType() throws Exception {
    doTest(GenericType.DOUBLE);
  }

  public void testLongType() throws Exception {
    doTest(GenericType.LONG);
  }

  public void testReferenceType() throws Exception {
    doTest(GenericType.OBJECT);
  }

  @NotNull
  @Override
  protected String getDirectoryName() {
    return "terminationType";
  }

  protected void doTest(@NotNull GenericType returnType) throws Exception {
    final PsiElement elementAtCaret = configureAndGetElementAtCaret();
    assertNotNull(elementAtCaret);
    final StreamChain chain = getChainBuilder().build(elementAtCaret);
    assertNotNull(chain);
    assertEquals(returnType, chain.getTerminationCall().getResultType());
  }

  @Override
  protected void checkResultChain(StreamChain chain) {
  }
}