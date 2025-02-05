/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.CustomDestinationResult.Cancelled
import androidx.compose.ui.focus.CustomDestinationResult.None
import androidx.compose.ui.focus.CustomDestinationResult.RedirectCancelled
import androidx.compose.ui.focus.CustomDestinationResult.Redirected
import androidx.compose.ui.focus.FocusDirection.Companion.Exit
import androidx.compose.ui.focus.FocusRequester.Companion.Cancel
import androidx.compose.ui.focus.FocusRequester.Companion.Redirect
import androidx.compose.ui.focus.FocusStateImpl.Active
import androidx.compose.ui.focus.FocusStateImpl.ActiveParent
import androidx.compose.ui.focus.FocusStateImpl.Captured
import androidx.compose.ui.focus.FocusStateImpl.Inactive
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.BeyondBoundsLayout
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.visitAncestors
import androidx.compose.ui.node.visitSelfAndAncestors
import androidx.compose.ui.node.visitSubtreeIf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.util.trace

internal class FocusTargetNode(
    focusability: Focusability = Focusability.Always,
    private val onFocusChange: ((previous: FocusState, current: FocusState) -> Unit)? = null,
    private val onDispatchEventsCompleted: ((FocusTargetNode) -> Unit)? = null
) :
    CompositionLocalConsumerModifierNode,
    FocusTargetModifierNode,
    ObserverModifierNode,
    ModifierLocalModifierNode,
    Modifier.Node() {

    private var isProcessingCustomExit = false
    private var isProcessingCustomEnter = false

    // During a transaction, changes to the state are stored as uncommitted focus state. At the
    // end of the transaction, this state is stored as committed focus state.
    private var committedFocusState: FocusStateImpl? = null

    override val shouldAutoInvalidate = false

    override var focusState: FocusStateImpl
        get() {
            if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                if (!isAttached) return Inactive
                val focusOwner = requireOwner().focusOwner
                val activeNode = focusOwner.activeFocusTargetNode ?: return Inactive
                return if (this === activeNode) {
                    if (focusOwner.isFocusCaptured) Captured else Active
                } else {
                    if (activeNode.isAttached) {
                        activeNode.visitAncestors(Nodes.FocusTarget) {
                            if (this === it) return ActiveParent
                        }
                    }
                    Inactive
                }
            } else {
                return focusTransactionManager?.run { uncommittedFocusState }
                    ?: committedFocusState
                    ?: Inactive
            }
        }
        set(value) {
            if (!@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                with(requireTransactionManager()) { uncommittedFocusState = value }
            }
        }

    @Deprecated(
        message = "Use the version accepting FocusDirection",
        replaceWith = ReplaceWith("this.requestFocus()"),
        level = DeprecationLevel.HIDDEN
    )
    override fun requestFocus(): Boolean {
        return requestFocus(FocusDirection.Enter)
    }

    override fun requestFocus(focusDirection: FocusDirection): Boolean {
        trace("FocusTransactions:requestFocus") {
            if (!fetchFocusProperties().canFocus) return false
            return if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                when (performCustomRequestFocus(focusDirection)) {
                    None -> performRequestFocus()
                    Redirected -> true
                    Cancelled,
                    RedirectCancelled -> false
                }
            } else {
                requireTransactionManager().withNewTransaction(
                    onCancelled = { if (node.isAttached) dispatchFocusCallbacks() }
                ) {
                    when (performCustomRequestFocus(focusDirection)) {
                        None -> performRequestFocus()
                        Redirected -> true
                        Cancelled,
                        RedirectCancelled -> false
                    }
                }
            }
        }
    }

    override var focusability: Focusability = focusability
        set(value) {
            if (field != value) {
                field = value
                if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                    if (
                        isAttached &&
                            this === requireOwner().focusOwner.activeFocusTargetNode &&
                            !field.canFocus(this)
                    ) {
                        clearFocus(forced = true, refreshFocusEvents = true)
                    }
                } else {
                    // Avoid invalidating if we have not been initialized yet: there is no need to
                    // invalidate since these property changes cannot affect anything.
                    if (isAttached && isInitialized()) {
                        // Invalidate focus if needed
                        onObservedReadsChanged()
                    }
                }
            }
        }

    var previouslyFocusedChildHash: Int = 0

    val beyondBoundsLayoutParent: BeyondBoundsLayout?
        get() = ModifierLocalBeyondBoundsLayout.current

    override fun onObservedReadsChanged() {
        if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
            invalidateFocus()
        } else {
            val previousFocusState = focusState
            invalidateFocus()
            if (previousFocusState != focusState) dispatchFocusCallbacks()
        }
    }

    override fun onAttach() {
        if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) return
        invalidateFocusTarget()
    }

    /** Clears focus if this focus target has it. */
    override fun onDetach() {
        //  Note: this is called after onEndApplyChanges, so we can't schedule any nodes for
        //  invalidation here. If we do, they will be run on the next onEndApplyChanges.
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active,
            Captured -> {
                val focusOwner = requireOwner().focusOwner
                focusOwner.clearFocus(
                    force = true,
                    refreshFocusEvents = true,
                    clearOwnerFocus = false,
                    focusDirection = Exit
                )
                // We don't clear the owner's focus yet, because this could trigger an initial
                // focus scenario after the focus is cleared. Instead, we schedule invalidation
                // after onApplyChanges. The FocusInvalidationManager contains the invalidation
                // logic and calls clearFocus() on the owner after all the nodes in the hierarchy
                // are invalidated.
                if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                    focusOwner.scheduleInvalidationForOwner()
                } else {
                    invalidateFocusTarget()
                }
            }
            // This node might be reused, so reset the state to Inactive.
            ActiveParent ->
                if (!@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
                    requireTransactionManager().withNewTransaction { focusState = Inactive }
                }
            Inactive -> {}
        }
        // This node might be reused, so we reset its state.
        committedFocusState = null
    }

    /**
     * Visits parent [FocusPropertiesModifierNode]s and runs
     * [FocusPropertiesModifierNode.applyFocusProperties] on each parent. This effectively collects
     * an aggregated focus state.
     */
    internal fun fetchFocusProperties(): FocusProperties {
        val properties = FocusPropertiesImpl()
        properties.canFocus = focusability.canFocus(this)
        visitSelfAndAncestors(Nodes.FocusProperties, untilType = Nodes.FocusTarget) {
            it.applyFocusProperties(properties)
        }
        return properties
    }

    private inline fun fetchCustomEnterOrExit(
        focusDirection: FocusDirection,
        block: (FocusRequester) -> Unit,
        enterOrExit: FocusProperties.(FocusEnterExitScope) -> Unit
    ) {
        val focusProperties = fetchFocusProperties()
        val scope = CancelIndicatingFocusBoundaryScope(focusDirection)
        val focusTransactionManager = focusTransactionManager
        val generationBefore = focusTransactionManager?.generation ?: 0
        val focusOwner = requireOwner().focusOwner
        val activeNodeBefore = focusOwner.activeFocusTargetNode
        focusProperties.enterOrExit(scope)
        val generationAfter = focusTransactionManager?.generation ?: 0
        val activeNodeAfter = focusOwner.activeFocusTargetNode
        if (scope.isCanceled) {
            block(Cancel)
        } else if (
            generationBefore != generationAfter ||
                (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled &&
                    (activeNodeBefore !== activeNodeAfter && activeNodeAfter != null))
        ) {
            block(Redirect)
        }
    }

    /**
     * Fetch custom enter destination associated with this [focusTarget].
     *
     * Custom focus enter properties are specified as a lambda. If the user runs code in this lambda
     * that triggers a focus search, or some other focus change that causes focus to leave the
     * sub-hierarchy associated with this node, we could end up in a loop as that operation will
     * trigger another invocation of the lambda associated with the focus exit property. This
     * function prevents that re-entrant scenario by ensuring there is only one concurrent
     * invocation of this lambda.
     */
    internal inline fun fetchCustomEnter(
        focusDirection: FocusDirection,
        block: (FocusRequester) -> Unit
    ) {
        if (!isProcessingCustomEnter) {
            isProcessingCustomEnter = true
            try {
                fetchCustomEnterOrExit(focusDirection, block) { it.onEnter() }
            } finally {
                isProcessingCustomEnter = false
            }
        }
    }

    /**
     * Fetch custom exit destination associated with this [focusTarget].
     *
     * Custom focus exit properties are specified as a lambda. If the user runs code in this lambda
     * that triggers a focus search, or some other focus change that causes focus to leave the
     * sub-hierarchy associated with this node, we could end up in a loop as that operation will
     * trigger another invocation of the lambda associated with the focus exit property. This
     * function prevents that re-entrant scenario by ensuring there is only one concurrent
     * invocation of this lambda.
     */
    internal inline fun fetchCustomExit(
        focusDirection: FocusDirection,
        block: (FocusRequester) -> Unit
    ) {
        if (!isProcessingCustomExit) {
            isProcessingCustomExit = true
            try {
                fetchCustomEnterOrExit(focusDirection, block) { it.onExit() }
            } finally {
                isProcessingCustomExit = false
            }
        }
    }

    internal fun commitFocusState() {
        with(requireTransactionManager()) {
            committedFocusState =
                checkPreconditionNotNull(uncommittedFocusState) {
                    "committing a node that was not updated in the current transaction"
                }
        }
    }

    internal fun invalidateFocus() {
        if (!isInitialized()) initializeFocusState()
        when (focusState) {
            // Clear focus from the current FocusTarget.
            // This currently clears focus from the entire hierarchy, but we can change the
            // implementation so that focus is sent to the immediate focus parent.
            Active,
            Captured -> {
                lateinit var focusProperties: FocusProperties
                observeReads { focusProperties = fetchFocusProperties() }
                if (!focusProperties.canFocus) {
                    requireOwner().focusOwner.clearFocus(force = true)
                }
            }
            ActiveParent,
            Inactive -> {}
        }
    }

    /**
     * Triggers [onFocusChange] and sends a "Focus Event" up the hierarchy that asks all
     * [FocusEventModifierNode]s to recompute their observed focus state.
     */
    internal fun dispatchFocusCallbacks() {
        val previousOrInactive = committedFocusState ?: Inactive
        val focusState = focusState
        // Avoid invoking callback when we initialize the state (from `null` to Inactive) or
        // if we are detached and go from Inactive to `null` - there isn't a conceptual focus
        // state change here
        if (previousOrInactive != focusState) {
            onFocusChange?.invoke(previousOrInactive, focusState)
        }
        visitSelfAndAncestors(Nodes.FocusEvent, untilType = Nodes.FocusTarget) {
            // TODO(251833873): Consider caching it.getFocusState().
            it.onFocusEvent(it.getFocusState())
        }
        onDispatchEventsCompleted?.invoke(this)
    }

    internal fun dispatchFocusCallbacks(previousState: FocusState, newState: FocusState) {
        val focusOwner = requireOwner().focusOwner
        val activeNode = focusOwner.activeFocusTargetNode
        if (previousState != newState) onFocusChange?.invoke(previousState, newState)
        visitSelfAndAncestors(Nodes.FocusEvent, untilType = Nodes.FocusTarget) {
            if (activeNode !== focusOwner.activeFocusTargetNode) {
                // Stop sending events, as focus changed in a callback
                return@visitSelfAndAncestors
            }
            it.onFocusEvent(newState)
        }
        onDispatchEventsCompleted?.invoke(this)
    }

    internal object FocusTargetElement : ModifierNodeElement<FocusTargetNode>() {
        override fun create() = FocusTargetNode()

        override fun update(node: FocusTargetNode) {}

        override fun InspectorInfo.inspectableProperties() {
            name = "focusTarget"
        }

        override fun hashCode() = "focusTarget".hashCode()

        override fun equals(other: Any?) = other === this
    }

    internal fun isInitialized(): Boolean =
        if (@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) true
        else committedFocusState != null

    internal fun initializeFocusState(initialFocusState: FocusStateImpl? = null) {
        fun isInActiveSubTree(): Boolean {
            visitAncestors(Nodes.FocusTarget) {
                if (!it.isInitialized()) return@visitAncestors

                return when (it.focusState) {
                    ActiveParent -> true
                    Active,
                    Captured,
                    Inactive -> false
                }
            }
            return false
        }

        fun hasActiveChild(): Boolean {
            visitSubtreeIf(Nodes.FocusTarget) {
                if (!it.isInitialized()) return@visitSubtreeIf true

                when (it.focusState) {
                    Active,
                    ActiveParent,
                    Captured -> return true
                    Inactive -> return@visitSubtreeIf false
                }
            }
            return false
        }

        check(!isInitialized()) { "Re-initializing focus target node." }

        if (!@OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isTrackFocusEnabled) {
            requireTransactionManager().withNewTransaction {
                // Note: hasActiveChild() is expensive since it searches the entire subtree. So we
                // only do this if we are part of the active subtree.
                this.focusState =
                    initialFocusState
                        ?: if (isInActiveSubTree() && hasActiveChild()) ActiveParent else Inactive
            }
        }
    }
}

internal fun FocusTargetNode.requireTransactionManager(): FocusTransactionManager {
    return requireOwner().focusOwner.focusTransactionManager
}

internal val FocusTargetNode.focusTransactionManager: FocusTransactionManager?
    get() = node.coordinator?.layoutNode?.owner?.focusOwner?.focusTransactionManager

internal fun FocusTargetNode.invalidateFocusTarget() {
    requireOwner().focusOwner.scheduleInvalidation(this)
}
