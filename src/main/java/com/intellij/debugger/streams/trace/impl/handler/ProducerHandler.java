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
package com.intellij.debugger.streams.trace.impl.handler;

import com.intellij.debugger.streams.trace.CallTransformer;
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.wrapper.ProducerStreamCall;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class ProducerHandler extends HandlerBase.Producer implements CallTransformer<ProducerStreamCall> {
  private final PeekTracerHandler myPeekTracerHandler;

  public ProducerHandler(@NotNull GenericType afterType) {
    myPeekTracerHandler = new PeekTracerHandler(0, "producer", GenericType.OBJECT, afterType);
  }

  @NotNull
  @Override
  protected List<Variable> getVariables() {
    return myPeekTracerHandler.getVariables();
  }

  @NotNull
  @Override
  public List<IntermediateStreamCall> additionalCallsAfter() {
    return myPeekTracerHandler.additionalCallsAfter();
  }

  @NotNull
  @Override
  public String prepareResult() {
    return myPeekTracerHandler.prepareResult();
  }

  @NotNull
  @Override
  public String getResultExpression() {
    return myPeekTracerHandler.getResultExpression();
  }
}
