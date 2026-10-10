package me.proxer.app.util.data

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * A LiveData for one-shot events: the value is reset to null once it is delivered to all observers.
 *
 * Observe it with [me.proxer.app.ui.components.LiveDataEffect] in Compose, not with observeAsState, which only sees
 * the reset.
 *
 * @author Ruben Gees
 */
class ResettingMutableLiveData<T> : MutableLiveData<T>() {

    private val observerAmount = AtomicInteger()
    private val deliveredAmount = AtomicInteger()
    private val wrappers = ConcurrentHashMap<Observer<in T>, Observer<T>>()

    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        val wrapper = Observer<T> {
            observer.onChanged(it)

            if (it != null) {
                deliveredAmount.incrementAndGet()

                if (deliveredAmount.compareAndSet(observerAmount.get(), 0)) {
                    value = null
                }
            }
        }

        wrappers[observer] = wrapper
        observerAmount.incrementAndGet()

        super.observe(owner, wrapper)
    }

    override fun removeObserver(observer: Observer<in T>) {
        // The observer is registered as its wrapper, which LiveData itself passes here when the owner is destroyed.
        val wrapper = wrappers.remove(observer)
            ?: wrappers.entries.find { it.value === observer }?.also { wrappers.remove(it.key) }?.value

        if (wrapper != null) {
            observerAmount.decrementAndGet()
            super.removeObserver(wrapper)
        } else {
            super.removeObserver(observer)
        }
    }
}
