/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.baselineprofile.gradle.utils

import org.gradle.api.logging.Logger

internal class BaselineProfilePluginLogger(private val logger: Logger) {

    private var warnings = Warnings()

    fun setWarnings(warnings: Warnings) {
        this.warnings = warnings
    }

    fun debug(message: String) = logger.debug(message)

    fun info(message: String) = logger.info(message)

    fun warn(
        property: Warnings.() -> (Boolean),
        propertyName: String?,
        message: String
    ) {
        if (property(warnings)) {
            logger.warn(message)
            if (propertyName != null) {
                logger.warn(
                    """
                
                This warning can be disabled setting the following property:
                baselineProfile {
                    warnings {
                        $propertyName = false
                    }
                }
            """.trimIndent()
                )
            }
        }
    }

    fun error(message: String) = logger.error(message)
}