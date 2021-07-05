package com.arcns.core.util

import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class InjectSuperViewModel

@MainThread
inline fun <reified VM : ViewModel> Fragment.viewModelsAndInjectSuper(
    noinline ownerProducer: () -> ViewModelStoreOwner = { this },
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null,
    noinline superOwnerProducer: () -> ViewModelStoreOwner = { this.requireActivity() },
    noinline superFactoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> = ViewModelAndInjectSuperLazy(
    VM::class,
    { ownerProducer().viewModelStore },
    factoryProducer ?: {
        requireActivity().defaultViewModelProviderFactory
    },
    { superOwnerProducer().viewModelStore },
    superFactoryProducer ?: { this.requireActivity().defaultViewModelProviderFactory }
)


class ViewModelAndInjectSuperLazy<VM : ViewModel>(
    private val viewModelClass: KClass<VM>,
    private val storeProducer: () -> ViewModelStore,
    private val factoryProducer: () -> ViewModelProvider.Factory,
    private val superStoreProducer: () -> ViewModelStore,
    private val superFactoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null

    init {

        LOG("ViewModelAndInjectSuperLazy:init")
    }

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                ViewModelProvider(storeProducer(), factoryProducer()).get(viewModelClass.java)
                    .also { viewModel ->
                        // 获取到SuperViewModel注解后，注入父级ViewModel
                        viewModelClass.declaredMemberProperties.forEach {
                            val superViewModel = it.findAnnotation<InjectSuperViewModel>()
                            if (superViewModel != null && it is KMutableProperty<*>) {
                                LOG("ViewModelAndInjectSuperLazy:for:" + it.name + "  " + it.returnType)
                                val javaClass =
                                    (it.returnType.classifier as? KClass<out ViewModel>)?.java
                                        ?: return@forEach
                                LOG("ViewModelAndInjectSuperLazy:type:" + it.returnType)
                                it.setter.call(
                                    viewModel,
                                    ViewModelProvider(
                                        superStoreProducer(),
                                        superFactoryProducer()
                                    ).get(javaClass)
                                )
                            }
                        }
                        cached = viewModel
                    }
            } else {
                viewModel
            }
        }

    override fun isInitialized() = cached != null
}
