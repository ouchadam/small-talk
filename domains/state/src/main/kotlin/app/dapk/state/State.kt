package app.dapk.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

fun <S> createStore(reducerFactory: ReducerFactory<S>, coroutineScope: CoroutineScope): Store<S> {
    val subscribers = mutableListOf<(S) -> Unit>()
    var state: S = reducerFactory.initialState()
    return object : Store<S> {
        private val scope = createScope(coroutineScope, this)
        private val reducer = reducerFactory.create(scope)

        override fun dispatch(action: Action) {
            coroutineScope.launch {
                state = reducer.reduce(action).also { nextState ->
                    if (nextState != state) {
                        subscribers.forEach { it.invoke(nextState) }
                    }
                }
            }
        }

        override fun getState() = state

        override fun subscribe(subscriber: (S) -> Unit) {
            subscribers.add(subscriber)
        }
    }
}

interface ReducerFactory<S> {
    fun create(scope: ReducerScope<S>): Reducer<S>
    fun initialState(): S
}

fun interface Reducer<S> {
    fun reduce(action: Action): S
}

private fun <S> createScope(coroutineScope: CoroutineScope, store: Store<S>) = object : ReducerScope<S> {
    override val coroutineScope = coroutineScope
    override fun dispatch(action: Action) = store.dispatch(action)
    override fun getState(): S = store.getState()
}

interface Store<S> {
    fun dispatch(action: Action)
    fun getState(): S
    fun subscribe(subscriber: (S) -> Unit)
}

interface ReducerScope<S> {
    val coroutineScope: CoroutineScope
    fun dispatch(action: Action)
    fun getState(): S
}

sealed interface ActionHandler<S> {
    val key: KClass<Action>

    class Async<S>(override val key: KClass<Action>, val handler: suspend ReducerScope<S>.(Action) -> Unit) : ActionHandler<S>
    class Sync<S>(override val key: KClass<Action>, val handler: (Action, S) -> S) : ActionHandler<S>
    class Delegate<S>(override val key: KClass<Action>, val handler: ReducerScope<S>.(Action) -> ActionHandler<S>) : ActionHandler<S>
}

data class Combined2<S1, S2>(val state1: S1, val state2: S2)

fun interface SharedStateScope<C> {
    fun getSharedState(): C
}

fun <S> shareState(block: SharedStateScope<S>.() -> ReducerFactory<S>): ReducerFactory<S> {
    var internalScope: ReducerScope<S>? = null
    val scope = SharedStateScope { internalScope!!.getState() }
    val combinedFactory = block(scope)
    return object : ReducerFactory<S> {
        override fun create(scope: ReducerScope<S>) = combinedFactory.create(scope).also { internalScope = scope }
        override fun initialState() = combinedFactory.initialState()
    }
}

fun <S1, S2> combineReducers(r1: ReducerFactory<S1>, r2: ReducerFactory<S2>): ReducerFactory<Combined2<S1, S2>> {
    return object : ReducerFactory<Combined2<S1, S2>> {
        override fun create(scope: ReducerScope<Combined2<S1, S2>>): Reducer<Combined2<S1, S2>> {
            val r1Scope = createReducerScope(scope) { scope.getState().state1 }
            val r2Scope = createReducerScope(scope) { scope.getState().state2 }

            val r1Reducer = r1.create(r1Scope)
            val r2Reducer = r2.create(r2Scope)
            return Reducer {
                Combined2(r1Reducer.reduce(it), r2Reducer.reduce(it))
            }
        }

        override fun initialState(): Combined2<S1, S2> = Combined2(r1.initialState(), r2.initialState())
    }
}

private fun <S> createReducerScope(scope: ReducerScope<*>, state: () -> S) = object : ReducerScope<S> {
    override val coroutineScope: CoroutineScope = scope.coroutineScope
    override fun dispatch(action: Action) = scope.dispatch(action)
    override fun getState() = state.invoke()
}

fun <S> createReducer(
    initialState: S,
    vararg reducers: (ReducerScope<S>) -> ActionHandler<S>,
): ReducerFactory<S> {
    return object : ReducerFactory<S> {
        override fun create(scope: ReducerScope<S>): Reducer<S> {
            val reducersMap = reducers
                .map { it.invoke(scope) }
                .groupBy { it.key }

            return Reducer { action ->
                val result = reducersMap.keys
                    .filter { it.java.isAssignableFrom(action::class.java) }
                    .fold(scope.getState()) { acc, key ->
                        val actionHandlers = reducersMap[key]!!
                        actionHandlers.fold(acc) { acc, handler ->
                            when (handler) {
                                is ActionHandler.Async -> {
                                    scope.coroutineScope.launch {
                                        handler.handler.invoke(scope, action)
                                    }
                                    acc
                                }

                                is ActionHandler.Sync -> handler.handler.invoke(action, acc)
                                is ActionHandler.Delegate -> when (val next = handler.handler.invoke(scope, action)) {
                                    is ActionHandler.Async -> {
                                        scope.coroutineScope.launch {
                                            next.handler.invoke(scope, action)
                                        }
                                        acc
                                    }

                                    is ActionHandler.Sync -> next.handler.invoke(action, acc)
                                    is ActionHandler.Delegate -> error("is not possible")
                                }
                            }
                        }
                    }
                result
            }
        }

        override fun initialState(): S = initialState

    }
}

fun <A : Action, S> sideEffect(klass: KClass<A>, block: suspend (A, S) -> Unit): (ReducerScope<S>) -> ActionHandler<S> {
    return {
        ActionHandler.Async(key = klass as KClass<Action>) { action -> block(action as A, getState()) }
    }
}

fun <A : Action, S> change(klass: KClass<A>, block: (A, S) -> S): (ReducerScope<S>) -> ActionHandler<S> {
    return {
        ActionHandler.Sync(key = klass as KClass<Action>, block as (Action, S) -> S)
    }
}

fun <A : Action, S> async(klass: KClass<A>, block: suspend ReducerScope<S>.(A) -> Unit): (ReducerScope<S>) -> ActionHandler<S> {
    return {
        ActionHandler.Async(key = klass as KClass<Action>, block as suspend ReducerScope<S>.(Action) -> Unit)
    }
}

fun <A : Action, S> multi(klass: KClass<A>, block: Multi<A, S>.(A) -> (ReducerScope<S>) -> ActionHandler<S>): (ReducerScope<S>) -> ActionHandler<S> {
    val multiScope = object : Multi<A, S> {
        override fun sideEffect(block: suspend (S) -> Unit): (ReducerScope<S>) -> ActionHandler<S> = sideEffect(klass) { _, state -> block(state) }
        override fun change(block: (A, S) -> S): (ReducerScope<S>) -> ActionHandler<S> = change(klass, block)
        override fun async(block: suspend ReducerScope<S>.(A) -> Unit): (ReducerScope<S>) -> ActionHandler<S> = async(klass, block)
        override fun nothing() = sideEffect { }
    }

    return {
        ActionHandler.Delegate(key = klass as KClass<Action>) { action ->
            block(multiScope, action as A).invoke(this)
        }
    }
}

interface Multi<A : Action, S> {
    fun sideEffect(block: suspend (S) -> Unit): (ReducerScope<S>) -> ActionHandler<S>
    fun nothing(): (ReducerScope<S>) -> ActionHandler<S>
    fun change(block: (A, S) -> S): (ReducerScope<S>) -> ActionHandler<S>
    fun async(block: suspend ReducerScope<S>.(A) -> Unit): (ReducerScope<S>) -> ActionHandler<S>
}

interface Action