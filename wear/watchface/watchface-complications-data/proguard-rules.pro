# Copyright (C) 2021 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Prevent Parcelizer objects from being removed or renamed.
-keep public class androidx.wear.watchface.complications.data.**Parcelizer { *; }
-keep public class androidx.wear.watchface.complications.data.DefaultComplicationDataSourcePolicyWireFormat { *; }
-keep public class android.support.wearable.complications.ComplicationData { *; }
-keep public class android.support.wearable.complications.ComplicationProviderInfo  { *; }
-keep public class android.support.wearable.complications.ComplicationText { *; }
-keep public class android.support.wearable.complications.ComplicationTextTemplate  { *; }
-keep public class android.support.wearable.complications.TimeDependentText { *; }
-keep public class android.support.wearable.complications.TimeDifferenceText { *; }
-keep public class android.support.wearable.complications.TimeFormatText { *; }
-keep public class android.graphics.drawable.Icon { *; }

# Ensure our sanitizing of EditorSession.usr_style doesn't break due to renames.
-keep public class kotlinx.coroutines.flow.MutableStateFlow { *; }

# Ensure that the class members of serialized form of complication data retained
-keepclassmembers class android.support.wearable.complications.ComplicationData$SerializedForm {
  <fields>;
  <methods>;
}
