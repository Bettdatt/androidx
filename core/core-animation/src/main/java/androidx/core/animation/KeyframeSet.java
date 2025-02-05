/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.animation;

import android.graphics.Path;
import android.util.Log;

import androidx.core.animation.Keyframe.FloatKeyframe;
import androidx.core.animation.Keyframe.IntKeyframe;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class holds a collection of Keyframe objects and is called by ValueAnimator to calculate
 * values between those keyframes for a given animation. The class internal to the animation
 * package because it is an implementation detail of how Keyframes are stored and used.
 */
class KeyframeSet<T> implements Keyframes<T> {

    int mNumKeyframes;

    Keyframe<T> mFirstKeyframe;
    Keyframe<T> mLastKeyframe;
    Interpolator mInterpolator; // only used in the 2-keyframe case
    List<Keyframe<T>> mKeyframes; // only used when there are not 2 keyframes
    TypeEvaluator<T> mEvaluator;


    @SafeVarargs
    KeyframeSet(Keyframe<T>... keyframes) {
        // immutable list
        mNumKeyframes = keyframes.length;
        mKeyframes = Arrays.asList(keyframes);
        mFirstKeyframe = keyframes[0];
        mLastKeyframe = keyframes[mNumKeyframes - 1];
        mInterpolator = mLastKeyframe.getInterpolator();
    }

    KeyframeSet(List<Keyframe<T>> keyframes) {
        mKeyframes = keyframes;
        mNumKeyframes = keyframes.size();
        mFirstKeyframe = keyframes.get(0);
        mLastKeyframe = keyframes.get(mNumKeyframes - 1);
        mInterpolator = mLastKeyframe.getInterpolator();
    }

    @Override
    public List<Keyframe<T>> getKeyframes() {
        return mKeyframes;
    }

    static KeyframeSet<Integer> ofInt(int... values) {
        int numKeyframes = values.length;
        IntKeyframe[] keyframes = new IntKeyframe[Math.max(numKeyframes, 2)];
        if (numKeyframes == 1) {
            keyframes[0] = (IntKeyframe) Keyframe.ofInt(0f);
            keyframes[1] = (IntKeyframe) Keyframe.ofInt(1f, values[0]);
        } else {
            keyframes[0] = (IntKeyframe) Keyframe.ofInt(0f, values[0]);
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes[i] =
                        (IntKeyframe) Keyframe.ofInt((float) i / (numKeyframes - 1), values[i]);
            }
        }
        return new IntKeyframeSet(keyframes);
    }

    static KeyframeSet<Float> ofFloat(float... values) {
        boolean badValue = false;
        int numKeyframes = values.length;
        FloatKeyframe[] keyframes = new FloatKeyframe[Math.max(numKeyframes, 2)];
        if (numKeyframes == 1) {
            keyframes[0] = (FloatKeyframe) Keyframe.ofFloat(0f);
            keyframes[1] = (FloatKeyframe) Keyframe.ofFloat(1f, values[0]);
            if (Float.isNaN(values[0])) {
                badValue = true;
            }
        } else {
            keyframes[0] = (FloatKeyframe) Keyframe.ofFloat(0f, values[0]);
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes[i] = (FloatKeyframe)
                        Keyframe.ofFloat((float) i / (numKeyframes - 1), values[i]);
                if (Float.isNaN(values[i])) {
                    badValue = true;
                }
            }
        }
        if (badValue) {
            Log.w("Animator", "Bad value (NaN) in float animator");
        }
        return new FloatKeyframeSet(keyframes);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> KeyframeSet<T> ofKeyframe(Keyframe<T>... keyframes) {
        // if all keyframes of same primitive type, create the appropriate KeyframeSet
        int numKeyframes = keyframes.length;
        boolean hasFloat = false;
        boolean hasInt = false;
        boolean hasOther = false;
        for (int i = 0; i < numKeyframes; ++i) {
            if (keyframes[i] instanceof FloatKeyframe) {
                hasFloat = true;
            } else if (keyframes[i] instanceof IntKeyframe) {
                hasInt = true;
            } else {
                hasOther = true;
            }
        }
        if (hasFloat && !hasInt && !hasOther) {
            FloatKeyframe[] floatKeyframes = new FloatKeyframe[numKeyframes];
            for (int i = 0; i < numKeyframes; ++i) {
                floatKeyframes[i] = (FloatKeyframe) keyframes[i];
            }
            return (KeyframeSet<T>) new FloatKeyframeSet(floatKeyframes);
        } else if (hasInt && !hasFloat && !hasOther) {
            IntKeyframe[] intKeyframes = new IntKeyframe[numKeyframes];
            for (int i = 0; i < numKeyframes; ++i) {
                intKeyframes[i] = (IntKeyframe) keyframes[i];
            }
            return (KeyframeSet<T>) new IntKeyframeSet(intKeyframes);
        } else {
            return new KeyframeSet<>(keyframes);
        }
    }

    @SafeVarargs
    public static <T> KeyframeSet<T> ofObject(T... values) {
        int numKeyframes = values.length;
        ArrayList<Keyframe<T>> keyframes = new ArrayList<>(Math.max(numKeyframes, 2));
        if (numKeyframes == 1) {
            keyframes.add(Keyframe.ofObject(0f));
            keyframes.add(Keyframe.ofObject(1f, values[0]));
        } else {
            keyframes.add(Keyframe.ofObject(0f, values[0]));
            for (int i = 1; i < numKeyframes; ++i) {
                keyframes.add(Keyframe.ofObject((float) i / (numKeyframes - 1), values[i]));
            }
        }
        return new KeyframeSet<T>(keyframes);
    }

    public static PathKeyframes ofPath(Path path) {
        return new PathKeyframes(path);
    }

    public static PathKeyframes ofPath(Path path, float error) {
        return new PathKeyframes(path, error);
    }

    /**
     * Sets the TypeEvaluator to be used when calculating animated values. This object
     * is required only for KeyframeSets that are not either IntKeyframeSet or FloatKeyframeSet,
     * both of which assume their own evaluator to speed up calculations with those primitive
     * types.
     *
     * @param evaluator The TypeEvaluator to be used to calculate animated values.
     */
    @Override
    public void setEvaluator(TypeEvaluator<T> evaluator) {
        mEvaluator = evaluator;
    }

    @Override
    public Class<?> getType() {
        return mFirstKeyframe.getType();
    }

    @Override
    public @NonNull KeyframeSet<T> clone() {
        List<Keyframe<T>> keyframes = mKeyframes;
        int numKeyframes = mKeyframes.size();
        final ArrayList<Keyframe<T>> newKeyframes = new ArrayList<>(numKeyframes);
        for (int i = 0; i < numKeyframes; i++) {
            @SuppressWarnings("unchecked")
            Keyframe<T> clone = keyframes.get(i).clone();
            newKeyframes.add(clone);
        }
        KeyframeSet<T> newSet = new KeyframeSet<>(newKeyframes);
        return newSet;
    }

    /**
     * Gets the animated value, given the elapsed fraction of the animation (interpolated by the
     * animation's interpolator) and the evaluator used to calculate in-between values. This
     * function maps the input fraction to the appropriate keyframe interval and a fraction
     * between them and returns the interpolated value. Note that the input fraction may fall
     * outside the [0-1] bounds, if the animation's interpolator made that happen (e.g., a
     * spring interpolation that might send the fraction past 1.0). We handle this situation by
     * just using the two keyframes at the appropriate end when the value is outside those bounds.
     *
     * @param fraction The elapsed fraction of the animation
     * @return The animated value.
     */
    @Override
    public T getValue(float fraction) {
        // Special-case optimization for the common case of only two keyframes
        if (mNumKeyframes == 2) {
            if (mInterpolator != null) {
                fraction = mInterpolator.getInterpolation(fraction);
            }
            return mEvaluator.evaluate(fraction, mFirstKeyframe.getValue(),
                    mLastKeyframe.getValue());
        }
        if (fraction <= 0f) {
            final Keyframe<T> nextKeyframe = mKeyframes.get(1);
            final Interpolator interpolator = nextKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = mFirstKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction)
                    / (nextKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, mFirstKeyframe.getValue(),
                    nextKeyframe.getValue());
        } else if (fraction >= 1f) {
            final Keyframe<T> prevKeyframe = mKeyframes.get(mNumKeyframes - 2);
            final Interpolator interpolator = mLastKeyframe.getInterpolator();
            if (interpolator != null) {
                fraction = interpolator.getInterpolation(fraction);
            }
            final float prevFraction = prevKeyframe.getFraction();
            float intervalFraction = (fraction - prevFraction)
                    / (mLastKeyframe.getFraction() - prevFraction);
            return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(),
                    mLastKeyframe.getValue());
        }
        Keyframe<T> prevKeyframe = mFirstKeyframe;
        for (int i = 1; i < mNumKeyframes; ++i) {
            Keyframe<T> nextKeyframe = mKeyframes.get(i);
            if (fraction < nextKeyframe.getFraction()) {
                final Interpolator interpolator = nextKeyframe.getInterpolator();
                final float prevFraction = prevKeyframe.getFraction();
                float intervalFraction = (fraction - prevFraction)
                        / (nextKeyframe.getFraction() - prevFraction);
                // Apply interpolator on the proportional duration.
                if (interpolator != null) {
                    intervalFraction = interpolator.getInterpolation(intervalFraction);
                }
                return mEvaluator.evaluate(intervalFraction, prevKeyframe.getValue(),
                        nextKeyframe.getValue());
            }
            prevKeyframe = nextKeyframe;
        }
        // shouldn't reach here
        return mLastKeyframe.getValue();
    }

    @Override
    public @NonNull String toString() {
        String returnVal = " ";
        for (int i = 0; i < mNumKeyframes; ++i) {
            returnVal += mKeyframes.get(i).getValue() + "  ";
        }
        return returnVal;
    }
}
