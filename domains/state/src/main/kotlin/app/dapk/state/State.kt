package app.dapk.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

fun <S> createStore(reducerFactory: ReducerFactory<S>, coroutineScope: CoroutineScope, log: Boolean = false): Store<S> {
    val subscribers = mutableListOf<(S) -> Unit>()
    var state: S = reducerFactory.initialState()
    return object : Store<S> {
        private val scope = createScope(coroutineScope, this)
        private val reducer = reducerFactory.create(scope)

        override fun dispatch(action: Action) {
            coroutineScope.launch {
                if (log) println("??? dispatch $action")
                state = reducer.reduce(action).also { nextState ->
                    if (nextState != state) {
                        if (log) {
                            println("??? action: $action result...")
                            println("??? current: $state")
                            println("??? next: $nextState")
                        }
                        subscribers.forEach { it.invoke(nextState) }
                    } else {
                        if (log) println("??? action: $action skipped, no change")
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
    override suspend fun dispatch(action: Action) = store.dispatch(action)
    override fun getState(): S = store.getState()
}

interface Store<S> {
    fun dispatch(action: Action)
    fun getState(): S
    fun subscribe(subscriber: (S) -> Unit)
}

interface ReducerScope<S> {
    val coroutineScope: CoroutineScope
    suspend fun dispatch(action: Action)
    fun getState(): S
}

sealed interface ActionHandler<S> {
    val key: KClass<Action>

    class Async<S>(override val key: KClass<Action>, val handler: suspend ReducerScope<S>.(Action) -> Unit) : ActionHandler<S>
    class Sync<S>(override val key: KClass<Action>, val handler: (Action, S) -> S) : ActionHandler<S>
    class Delegate<S>(override val key: KClass<Action>, val handler: ReducerScope<S>.(Action) -> ActionHandler<S>) : ActionHandler<S>
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
        override fun sideEffect(block: (A, S) -> Unit): (ReducerScope<S>) -> ActionHandler<S> = sideEffect(klass, block)
        override fun change(block: (A, S) -> S): (ReducerScope<S>) -> ActionHandler<S> = change(klass, block)
        override fun async(block: suspend ReducerScope<S>.(A) -> Unit): (ReducerScope<S>) -> ActionHandler<S> = async(klass, block)
    }

    return {
        ActionHandler.Delegate(key = klass as KClass<Action>) { action ->
            block(multiScope, action as A).invoke(this)
        }
    }
}

interface Multi<A : Action, S> {
    fun sideEffect(block: (A, S) -> Unit): (ReducerScope<S>) -> ActionHandler<S>
    fun change(block: (A, S) -> S): (ReducerScope<S>) -> ActionHandler<S>
    fun async(block: suspend ReducerScope<S>.(A) -> Unit): (ReducerScope<S>) -> ActionHandler<S>
}

interface Action