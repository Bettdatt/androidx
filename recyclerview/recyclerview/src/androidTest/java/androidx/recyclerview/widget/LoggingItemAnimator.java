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

package androidx.recyclerview.widget;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoggingItemAnimator extends DefaultItemAnimator {

    final ArrayList<RecyclerView.ViewHolder> mAddVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mRemoveVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mMoveVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mChangeOldVHs = new ArrayList<RecyclerView.ViewHolder>();

    final ArrayList<RecyclerView.ViewHolder> mChangeNewVHs = new ArrayList<RecyclerView.ViewHolder>();

    List<BaseRecyclerViewAnimationsTest.AnimateAppearance> mAnimateAppearanceList = new ArrayList<>();
    List<BaseRecyclerViewAnimationsTest.AnimateDisappearance> mAnimateDisappearanceList = new ArrayList<>();
    List<BaseRecyclerViewAnimationsTest.AnimatePersistence> mAnimatePersistenceList = new ArrayList<>();
    List<BaseRecyclerViewAnimationsTest.AnimateChange> mAnimateChangeList = new ArrayList<>();

    CountDownLatch mWaitForPendingAnimations;

    public boolean contains(RecyclerView.ViewHolder viewHolder,
            List<? extends BaseRecyclerViewAnimationsTest.AnimateLogBase> list) {
        for (BaseRecyclerViewAnimationsTest.AnimateLogBase log : list) {
            if (log.viewHolder == viewHolder) {
                return true;
            }
            if (log instanceof BaseRecyclerViewAnimationsTest.AnimateChange) {
                if (((BaseRecyclerViewAnimationsTest.AnimateChange) log).newHolder == viewHolder) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public @NonNull ItemHolderInfo recordPreLayoutInformation(RecyclerView.@NonNull State state,
            RecyclerView.@NonNull ViewHolder viewHolder,
            @AdapterChanges int changeFlags, @NonNull List<Object> payloads) {
        BaseRecyclerViewAnimationsTest.LoggingInfo
                loggingInfo = new BaseRecyclerViewAnimationsTest.LoggingInfo(viewHolder, changeFlags, payloads);
        return loggingInfo;
    }

    @Override
    public @NonNull ItemHolderInfo recordPostLayoutInformation(RecyclerView.@NonNull State state,
            RecyclerView.@NonNull ViewHolder viewHolder) {
        BaseRecyclerViewAnimationsTest.LoggingInfo
                loggingInfo = new BaseRecyclerViewAnimationsTest.LoggingInfo(viewHolder, 0, null);
        return loggingInfo;
    }


    @Override
    public boolean animateDisappearance(RecyclerView.@NonNull ViewHolder viewHolder,
            @NonNull ItemHolderInfo preLayoutInfo, ItemHolderInfo postLayoutInfo) {
        mAnimateDisappearanceList
                .add(new BaseRecyclerViewAnimationsTest.AnimateDisappearance(viewHolder,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) preLayoutInfo,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) postLayoutInfo));
        return super.animateDisappearance(viewHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public boolean animateAppearance(RecyclerView.@NonNull ViewHolder viewHolder,
            ItemHolderInfo preLayoutInfo,
            @NonNull ItemHolderInfo postLayoutInfo) {
        mAnimateAppearanceList
                .add(new BaseRecyclerViewAnimationsTest.AnimateAppearance(viewHolder,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) preLayoutInfo,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) postLayoutInfo));
        return super.animateAppearance(viewHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public boolean animatePersistence(RecyclerView.@NonNull ViewHolder viewHolder,
            @NonNull ItemHolderInfo preLayoutInfo,
            @NonNull ItemHolderInfo postLayoutInfo) {
        mAnimatePersistenceList
                .add(new BaseRecyclerViewAnimationsTest.AnimatePersistence(viewHolder,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) preLayoutInfo,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) postLayoutInfo));
        return super.animatePersistence(viewHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public boolean animateChange(RecyclerView.@NonNull ViewHolder oldHolder,
            RecyclerView.@NonNull ViewHolder newHolder, @NonNull ItemHolderInfo preLayoutInfo,
            @NonNull ItemHolderInfo postLayoutInfo) {
        mAnimateChangeList
                .add(new BaseRecyclerViewAnimationsTest.AnimateChange(oldHolder, newHolder,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) preLayoutInfo,
                        (BaseRecyclerViewAnimationsTest.LoggingInfo) postLayoutInfo));
        return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo);
    }

    @Override
    public void runPendingAnimations() {
        if (mWaitForPendingAnimations != null) {
            mWaitForPendingAnimations.countDown();
        }
        super.runPendingAnimations();
    }

    public void expectRunPendingAnimationsCall(int count) {
        mWaitForPendingAnimations = new CountDownLatch(count);
    }

    public void waitForPendingAnimationsCall(int seconds) throws InterruptedException {
        mWaitForPendingAnimations.await(seconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean animateAdd(RecyclerView.@NonNull ViewHolder holder) {
        mAddVHs.add(holder);
        return super.animateAdd(holder);
    }

    @Override
    public boolean animateRemove(RecyclerView.@NonNull ViewHolder holder) {
        mRemoveVHs.add(holder);
        return super.animateRemove(holder);
    }

    @Override
    public boolean animateMove(RecyclerView.@NonNull ViewHolder holder, int fromX, int fromY,
            int toX, int toY) {
        mMoveVHs.add(holder);
        return super.animateMove(holder, fromX, fromY, toX, toY);
    }

    @Override
    public boolean animateChange(RecyclerView.@NonNull ViewHolder oldHolder,
            RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
        if (oldHolder != null) {
            mChangeOldVHs.add(oldHolder);
        }
        if (newHolder != null) {
            mChangeNewVHs.add(newHolder);
        }
        return super.animateChange(oldHolder, newHolder, fromLeft, fromTop, toLeft, toTop);
    }

    public void reset() {
        mAddVHs.clear();
        mRemoveVHs.clear();
        mMoveVHs.clear();
        mChangeOldVHs.clear();
        mChangeNewVHs.clear();
        mAnimateChangeList.clear();
        mAnimatePersistenceList.clear();
        mAnimateAppearanceList.clear();
        mAnimateDisappearanceList.clear();
    }
}