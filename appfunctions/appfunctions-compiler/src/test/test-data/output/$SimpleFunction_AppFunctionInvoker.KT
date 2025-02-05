package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.`internal`.AppFunctionInvoker
import androidx.appfunctions.`internal`.ConfigurableAppFunctionFactory
import javax.`annotation`.processing.Generated
import kotlin.Any
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$SimpleFunction_AppFunctionInvoker` : AppFunctionInvoker {
  override val supportedFunctionIds: Set<String> = setOf(
    "com.testdata.SimpleFunction#simpleFunction",
  )

  override suspend fun unsafeInvoke(
    appFunctionContext: AppFunctionContext,
    functionIdentifier: String,
    parameters: Map<String, Any?>,
  ): Any? {
    val result = when (functionIdentifier) {
      "com.testdata.SimpleFunction#simpleFunction" -> {
        ConfigurableAppFunctionFactory<SimpleFunction>(
          appFunctionContext.context
        )
        .createEnclosingClass(SimpleFunction::class.java)
        .simpleFunction(appFunctionContext)
      }
      else -> {
        throw AppFunctionFunctionNotFoundException("Unable to find $functionIdentifier")
      }
    }
    return result
  }
}
