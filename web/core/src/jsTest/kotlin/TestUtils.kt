import androidx.compose.runtime.Composable
import androidx.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.promise
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/*
 * Copyright 2021 The Android Open Source Project
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

private val testScope = MainScope()

class TestScope : CoroutineScope by testScope {

    val root = "div".asHtmlElement()

    fun composition(content: @Composable () -> Unit) {
        root.clear()
        renderComposable(root) {
            content()
        }
    }

    suspend fun waitChanges() {
        waitForChanges(root)
    }
}

internal fun runTest(block: suspend TestScope.() -> Unit): dynamic {
    val scope = TestScope()
    return scope.promise { block(scope) }
}

internal fun runBlockingTest(
    block: suspend CoroutineScope.() -> Unit
): dynamic = testScope.promise { this.block() }

internal fun String.asHtmlElement() = document.createElement(this) as HTMLElement

/* Currently, the recompositionRunner relies on AnimationFrame to run the recomposition and
applyChanges. Therefore we can use this method after updating the state and before making
assertions.

If tests get broken, then DefaultMonotonicFrameClock need to be checked if it still
uses window.requestAnimationFrame */
internal suspend fun waitForAnimationFrame() {
    suspendCoroutine<Unit> { continuation ->
        window.requestAnimationFrame {
            continuation.resume(Unit)
        }
    }
}

private object MutationObserverOptions : MutationObserverInit {
    override var childList: Boolean? = true
    override var attributes: Boolean? = true
    override var characterData: Boolean? = true
    override var subtree: Boolean? = true
    override var attributeOldValue: Boolean? = true
}

internal suspend fun waitForChanges(elementId: String) {
    waitForChanges(document.getElementById(elementId) as HTMLElement)
}

internal suspend fun waitForChanges(element: HTMLElement) {
    suspendCoroutine<Unit> { continuation ->
        val observer = MutationObserver { mutations, observer ->
            continuation.resume(Unit)
            observer.disconnect()
        }
        observer.observe(element, MutationObserverOptions)
    }
}