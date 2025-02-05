/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.appfunctions.metadata

import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.Companion.TYPE_STRING
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionMetadataTest {

    @Test
    fun appFunctionMetadata_equalsAndHashCode() {
        val schema =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val parameters =
            AppFunctionObjectTypeMetadata(
                properties = emptyMap(),
                required = emptyList(),
                isNullable = false
            )
        val response = AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false)

        val metadata1 =
            AppFunctionMetadata(
                id = " id",
                isEnabledByDefault = true,
                schema = schema,
                parameters = parameters,
                response = response,
            )
        val metadata2 =
            AppFunctionMetadata(
                id = " id",
                isEnabledByDefault = true,
                schema = schema,
                parameters = parameters,
                response = response,
            )
        val metadata3 =
            AppFunctionMetadata(
                id = " id",
                isEnabledByDefault = false,
                schema = schema,
                parameters = parameters,
                response = response,
            )

        assertThat(metadata1).isEqualTo(metadata2)
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode())
        assertThat(metadata1).isNotEqualTo(metadata3)
        assertThat(metadata1.hashCode()).isNotEqualTo(metadata3.hashCode())
    }

    @Test
    fun appFunctionMetadata_toAppFunctionMetadataDocument_returnsCorrectDocument() {
        val id = "fakeFunctionIdentifier"
        val isEnabledByDefault = true
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val primitiveTypeInt = AppFunctionPrimitiveTypeMetadata(TYPE_INT, true)
        val primitiveTypeLong = AppFunctionPrimitiveTypeMetadata(TYPE_LONG, true)
        val properties =
            mapOf(
                "prop1" to primitiveTypeInt,
                "prop2" to primitiveTypeLong,
            )
        val isNullable = false
        val requiredProperties = listOf("prop1", "prop2")
        val parameters = AppFunctionObjectTypeMetadata(properties, requiredProperties, isNullable)
        val response = AppFunctionPrimitiveTypeMetadata(type = TYPE_STRING, isNullable = false)
        val primitiveType1 = AppFunctionPrimitiveTypeMetadata(TYPE_INT, false)
        val primitiveType2 = AppFunctionPrimitiveTypeMetadata(TYPE_STRING, true)
        val components = AppFunctionComponentsMetadata(listOf(primitiveType1, primitiveType2))
        val appFunctionMetadata =
            AppFunctionMetadata(
                id = id,
                isEnabledByDefault = isEnabledByDefault,
                schema = schemaMetadata,
                parameters = parameters,
                response = response,
                components = components
            )
        val expectedAppFunctionMetadataDocument =
            AppFunctionMetadataDocument(
                id = id,
                isEnabledByDefault = isEnabledByDefault,
                schema = schemaMetadata.toAppFunctionSchemaMetadataDocument(),
                parameters = parameters.toAppFunctionDataTypeMetadataDocument(),
                response = response.toAppFunctionDataTypeMetadataDocument(),
                components = components.toAppFunctionComponentsMetadataDocument()
            )

        val actualAppFunctionMetadataDocument = appFunctionMetadata.toAppFunctionMetadataDocument()

        assertThat(actualAppFunctionMetadataDocument).isEqualTo(expectedAppFunctionMetadataDocument)
    }
}
