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
package com.intellij.debugger.streams.trace.impl.handler.unified

import com.intellij.debugger.streams.trace.dsl.*
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.wrapper.CallArgument
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.impl.CallArgumentImpl
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import com.intellij.openapi.util.TextRange
import one.util.streamex.StreamEx

/**
 * @author Vitaliy.Bibaev
 */
open class DistinctByKeyHandler(callNumber: Int, call: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
  private companion object {
    val KEY_EXTRACTOR_VARIABLE_PREFIX = "keyExtractor"
    val TRANSITIONS_ARRAY_NAME = "transitionsArray"
  }

  private val myPeekHandler = PeekTraceHandler(callNumber, "distinct", call.typeBefore, call.typeAfter, dsl)
  private val myKeyExtractor: CallArgument
  private val myTypeAfter = call.typeAfter
  private val myExtractorVariable: Variable
  private val myBeforeTimes = dsl.list(dsl.types.integerType, call.name + callNumber + "BeforeTimes")
  private val myBeforeValues = dsl.list(dsl.types.anyType, call.name + callNumber + "BeforeValues")
  private val myKeys = dsl.list(dsl.types.anyType, call.name + callNumber + "Keys")
  private val myTime2ValueAfter = dsl.linkedMap(dsl.types.integerType, dsl.types.anyType, call.name + callNumber + "after")

  init {
    val arguments = call.arguments
    assert(arguments.isNotEmpty(), { "Key extractor is not specified" })
    myKeyExtractor = arguments.first()
    myExtractorVariable = dsl.variable(ClassTypeImpl(myKeyExtractor.type), KEY_EXTRACTOR_VARIABLE_PREFIX + callNumber)
  }

  override fun additionalVariablesDeclaration(): List<VariableDeclaration> {
    val extractor = dsl.declaration(myExtractorVariable, TextExpression(myKeyExtractor.text), false)
    val variables =
      mutableListOf<VariableDeclaration>(extractor, myBeforeTimes.defaultDeclaration(),
                                         myBeforeValues.defaultDeclaration(), myTime2ValueAfter.defaultDeclaration(),
                                         myKeys.defaultDeclaration())
    variables.addAll(myPeekHandler.additionalVariablesDeclaration())

    return variables
  }

  override fun transformCall(call: IntermediateStreamCall): IntermediateStreamCall {
    val newKeyExtractor = dsl.lambda("x") {
      +myExtractorVariable.call("andThen", dsl.lambda("t") {
        +myBeforeTimes.add(dsl.currentTime())
        +myBeforeValues.add(TextExpression("x"))
        +myKeys.add(TextExpression("t"))
        // TODO: avoid string literals. It'll lead to problems in kotlin (see kotlin lambda return semantic)
        +TextExpression("return t")
      }).call("apply", TextExpression("x"))
    }.toCode()
    return call.updateArguments(listOf(CallArgumentImpl(myKeyExtractor.type, newKeyExtractor)))
  }

  override fun prepareResult(): CodeBlock {
    val keys2TimesBefore = dsl.map(dsl.types.anyType, dsl.types.list(dsl.types.integerType), "keys2Times")
    val transitions = dsl.map(dsl.types.integerType, dsl.types.integerType, "transitionsMap")
    StreamEx.of(1).distinct().toList()
    return dsl.block {
      add(myPeekHandler.prepareResult())
      declare(keys2TimesBefore.defaultDeclaration())
      declare(transitions.defaultDeclaration())

      integerIteration(myKeys.size()) {
        val key = declare(variable(types.anyType, "key"), myKeys.get(loopVariable), false)
        +keys2TimesBefore.computeIfAbsent(key, lambda("k") {
          +newList(types.integerType)
        }).call("add", myBeforeTimes.get(loopVariable))
      }

      forEachLoop(variable(types.integerType, "afterTime"), myTime2ValueAfter.keys()) {
        val afterTime = loopVariable
        val valueAfter = declare(variable(types.anyType, "valueAfter"), myTime2ValueAfter.get(loopVariable), false)
        val key = declare(variable(types.anyType, "key"), nullExpression, true)
        integerIteration(myBeforeTimes.size()) {
          ifBranch(and(same(valueAfter, myBeforeValues.get(loopVariable)), not(transitions.contains(myBeforeTimes.get(loopVariable))))) {
            key.assign(myKeys.get(loopVariable))
            breakIteration()
          }
        }

        forEachLoop(variable(types.integerType, "beforeTime"), keys2TimesBefore.get(key)) {
          transitions.set(loopVariable, afterTime)
        }
      }

      add(transitions.convertToArray(this, "transitionsArray"))
    }
  }

  override fun getResultExpression(): Expression =
    dsl.newArray(dsl.types.anyType, myPeekHandler.resultExpression, TextExpression(TRANSITIONS_ARRAY_NAME))

  override fun additionalCallsBefore(): List<IntermediateStreamCall> = myPeekHandler.additionalCallsBefore()

  override fun additionalCallsAfter(): List<IntermediateStreamCall> {
    val callsAfter = ArrayList(myPeekHandler.additionalCallsAfter())
    val lambda = dsl.lambda("x") {
      +myTime2ValueAfter.set(dsl.currentTime(), TextExpression(argName))
    }

    callsAfter.add(dsl.createPeekCall(myTypeAfter, lambda.toCode()))
    return callsAfter
  }

  private fun CodeContext.integerIteration(border: Expression, init: ForLoopBody.() -> Unit) {
    forLoop(declaration(variable(types.integerType, "i"), TextExpression("0"), true),
            TextExpression("i < ${border.toCode()}"),
            TextExpression("i = i + 1"), init)
  }

  private fun IntermediateStreamCall.updateArguments(args: List<CallArgument>): IntermediateStreamCall =
    IntermediateStreamCallImpl(name, args, typeBefore, typeAfter, textRange, packageName)

  open class DistinctByCustomKey(callNumber: Int,
                                 call: IntermediateStreamCall,
                                 extractorType: String,
                                 extractorExpression: String,
                                 dsl: Dsl)
    : DistinctByKeyHandler(callNumber, call.transform(extractorType, extractorExpression), dsl) {

    companion object {
      fun IntermediateStreamCall.transform(extractorType: String, extractorExpression: String): IntermediateStreamCall {
        return IntermediateStreamCallImpl("distinct", listOf(CallArgumentImpl(extractorType, extractorExpression)), typeBefore,
                                          typeAfter, TextRange.EMPTY_RANGE, packageName)
      }
    }
  }
}

class DistinctKeysHandler(callNumber: Int, call: IntermediateStreamCall, dsl: Dsl)
  : DistinctByKeyHandler.DistinctByCustomKey(callNumber, call, "java.util.function.Function<java.util.Map.Entry, java.lang.Object>",
                                             dsl.lambda("x") {
                                               +TextExpression("x").call("getKey", TextExpression(argName))
                                             }.toCode(),
                                             dsl)

class DistinctValuesHandler(callNumber: Int, call: IntermediateStreamCall, dsl: Dsl)
  : DistinctByKeyHandler.DistinctByCustomKey(callNumber, call, "java.util.function.Function<java.util.Map.Entry, java.lang.Object>",
                                             dsl.lambda("x") {
                                               +TextExpression("x").call("getValue", TextExpression(argName))
                                             }.toCode(),
                                             dsl)