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
package com.intellij.debugger.streams.trace.impl.handler.type

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.TypeConversionUtil
import one.util.streamex.StreamEx
import org.jetbrains.annotations.Contract

/**
 * @author Vitaliy.Bibaev
 */
object GenericTypeUtil {
  private val OPTIONAL_TYPES = StreamEx
    .of(GenericType.OPTIONAL, GenericType.OPTIONAL_INT, GenericType.OPTIONAL_LONG,
        GenericType.OPTIONAL_DOUBLE)
    .toSet()

  fun fromStreamPsiType(streamPsiType: PsiType): GenericType {
    if (InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM)) return GenericType.INT
    if (InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM)) return GenericType.LONG
    if (InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM))
      return GenericType
        .DOUBLE
    return if (PsiType.VOID == streamPsiType) GenericType.VOID else GenericType.OBJECT

  }

  fun fromPsiClass(psiClass: PsiClass): GenericType {
    if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM)) return GenericType.INT
    if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM)) return GenericType.LONG
    return if (InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM)) GenericType.DOUBLE
    else GenericType.OBJECT

  }

  fun fromPsiType(type: PsiType): GenericType {
    if (PsiType.VOID == type) return GenericType.VOID
    if (PsiType.INT == type) return GenericType.INT
    if (PsiType.DOUBLE == type) return GenericType.DOUBLE
    if (PsiType.LONG == type) return GenericType.LONG
    return if (PsiType.BOOLEAN == type) GenericType.BOOLEAN else ClassTypeImpl(TypeConversionUtil.erasure(type).canonicalText)
  }

  @Contract(pure = true)
  private fun isOptional(type: GenericType): Boolean {
    return OPTIONAL_TYPES.contains(type)
  }

  fun unwrapOptional(type: GenericType): GenericType {
    assert(isOptional(type))

    return when (type) {
      GenericType.OPTIONAL_INT -> GenericType.INT
      GenericType.OPTIONAL_LONG -> GenericType.LONG
      GenericType.OPTIONAL_DOUBLE -> GenericType.DOUBLE
      else -> GenericType.OBJECT
    }
  }
}