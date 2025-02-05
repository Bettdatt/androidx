/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.graphics.Color;

import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.Duration;
import java.time.Instant;

@RunWith(RobolectricTestRunner.class)
public final class DynamicDataValueTest {
    @Test
    public void boolDynamicDataValue() {
        DynamicDataValue<DynamicBool> boolDynamicDataValue = DynamicDataValue.fromBool(true);

        assertThat(boolDynamicDataValue.hasBoolValue()).isTrue();
        assertThat(boolDynamicDataValue.getBoolValue()).isTrue();
        assertThat(boolDynamicDataValue.toDynamicDataValueProto().getBoolVal().getValue()).isTrue();

        assertThat(boolDynamicDataValue.hasColorValue()).isFalse();
        assertThrows(IllegalStateException.class, boolDynamicDataValue::getColorValue);
        assertThat(boolDynamicDataValue.hasValueOfType(Color.class)).isFalse();
        assertThat(boolDynamicDataValue.hasValueOfType(boolean.class)).isTrue();
        assertThat(boolDynamicDataValue.hasValueOfType(Boolean.class)).isTrue();
        assertThat(boolDynamicDataValue.hasValueOfType(DynamicBool.class)).isTrue();
    }

    @Test
    public void colorDynamicDataValue() {
        int c = 0xff00ff00;
        DynamicDataValue<DynamicColor> colorDynamicDataValue = DynamicDataValue.fromColor(c);

        assertThat(colorDynamicDataValue.hasColorValue()).isTrue();
        assertThat(colorDynamicDataValue.getColorValue()).isEqualTo(c);
        assertThat(colorDynamicDataValue.toDynamicDataValueProto().getColorVal().getArgb())
                .isEqualTo(c);

        assertThat(colorDynamicDataValue.hasFloatValue()).isFalse();
        assertThrows(IllegalStateException.class, colorDynamicDataValue::getFloatValue);
        assertThat(colorDynamicDataValue.hasValueOfType(Float.class)).isFalse();
        assertThat(colorDynamicDataValue.hasValueOfType(Color.class)).isTrue();
        assertThat(colorDynamicDataValue.hasValueOfType(DynamicColor.class)).isTrue();
    }

    @Test
    public void floatDynamicDataValue() {
        float f = 42.42f;
        DynamicDataValue<DynamicFloat> floatDynamicDataValue = DynamicDataValue.fromFloat(f);

        assertThat(floatDynamicDataValue.hasFloatValue()).isTrue();
        assertThat(floatDynamicDataValue.getFloatValue()).isEqualTo(f);
        assertThat(floatDynamicDataValue.toDynamicDataValueProto().getFloatVal().getValue())
                .isWithin(0.0001f)
                .of(f);

        assertThat(floatDynamicDataValue.hasIntValue()).isFalse();
        assertThrows(IllegalStateException.class, floatDynamicDataValue::getIntValue);
        assertThat(floatDynamicDataValue.hasValueOfType(int.class)).isFalse();
        assertThat(floatDynamicDataValue.hasValueOfType(Float.class)).isTrue();
        assertThat(floatDynamicDataValue.hasValueOfType(float.class)).isTrue();
        assertThat(floatDynamicDataValue.hasValueOfType(DynamicFloat.class)).isTrue();
    }

    @Test
    public void intDynamicDataValue() {
        int i = 42;
        DynamicDataValue<DynamicInt32> intDynamicDataValue = DynamicDataValue.fromInt(i);

        assertThat(intDynamicDataValue.hasIntValue()).isTrue();
        assertThat(intDynamicDataValue.getIntValue()).isEqualTo(i);
        assertThat(intDynamicDataValue.toDynamicDataValueProto().getInt32Val().getValue())
                .isEqualTo(i);

        assertThat(intDynamicDataValue.hasStringValue()).isFalse();
        assertThrows(IllegalStateException.class, intDynamicDataValue::getStringValue);
        assertThat(intDynamicDataValue.hasValueOfType(String.class)).isFalse();
        assertThat(intDynamicDataValue.hasValueOfType(int.class)).isTrue();
        assertThat(intDynamicDataValue.hasValueOfType(Integer.class)).isTrue();
        assertThat(intDynamicDataValue.hasValueOfType(DynamicInt32.class)).isTrue();
        assertThat(
                        intDynamicDataValue.hasValueOfType(
                                PlatformHealthSources.DynamicHeartRateAccuracy.class))
                .isTrue();
    }

    @Test
    public void stringDynamicDataValue() {
        String s = "constant-value";
        DynamicDataValue<DynamicString> stringDynamicDataValue = DynamicDataValue.fromString(s);

        assertThat(stringDynamicDataValue.hasStringValue()).isTrue();
        assertThat(stringDynamicDataValue.getStringValue()).isEqualTo(s);
        assertThat(stringDynamicDataValue.toDynamicDataValueProto().getStringVal().getValue())
                .isEqualTo(s);

        assertThat(stringDynamicDataValue.hasInstantValue()).isFalse();
        assertThrows(IllegalStateException.class, stringDynamicDataValue::getInstantValue);
        assertThat(stringDynamicDataValue.hasValueOfType(Instant.class)).isFalse();
        assertThat(stringDynamicDataValue.hasValueOfType(String.class)).isTrue();
        assertThat(stringDynamicDataValue.hasValueOfType(DynamicString.class)).isTrue();
    }

    @Test
    public void instantDynamicDataValue() {
        Instant instant = Instant.ofEpochSecond(12345);
        DynamicDataValue<DynamicBuilders.DynamicInstant> instantDynamicDataValue =
                DynamicDataValue.fromInstant(instant);

        assertThat(instantDynamicDataValue.hasInstantValue()).isTrue();
        assertThat(instantDynamicDataValue.getInstantValue()).isEqualTo(instant);
        assertThat(
                        instantDynamicDataValue
                                .toDynamicDataValueProto()
                                .getInstantVal()
                                .getEpochSeconds())
                .isEqualTo(instant.getEpochSecond());

        assertThat(instantDynamicDataValue.hasDurationValue()).isFalse();
        assertThrows(IllegalStateException.class, instantDynamicDataValue::getDurationValue);
        assertThat(instantDynamicDataValue.hasValueOfType(Duration.class)).isFalse();
        assertThat(instantDynamicDataValue.hasValueOfType(Instant.class)).isTrue();
        assertThat(instantDynamicDataValue.hasValueOfType(DynamicBuilders.DynamicInstant.class))
                .isTrue();
    }

    @Test
    public void durationDynamicDataValue() {
        Duration duration = Duration.ofSeconds(12345);
        DynamicDataValue<DynamicBuilders.DynamicDuration> durationDynamicDataValue =
                DynamicDataValue.fromDuration(duration);

        assertThat(durationDynamicDataValue.hasDurationValue()).isTrue();
        assertThat(durationDynamicDataValue.getDurationValue()).isEqualTo(duration);
        assertThat(durationDynamicDataValue.toDynamicDataValueProto().getDurationVal().getSeconds())
                .isEqualTo(duration.getSeconds());

        assertThat(durationDynamicDataValue.hasBoolValue()).isFalse();
        assertThrows(IllegalStateException.class, durationDynamicDataValue::getBoolValue);
        assertThat(durationDynamicDataValue.hasValueOfType(Boolean.class)).isFalse();
        assertThat(durationDynamicDataValue.hasValueOfType(Duration.class)).isTrue();
        assertThat(durationDynamicDataValue.hasValueOfType(DynamicBuilders.DynamicDuration.class))
                .isTrue();
    }
}
