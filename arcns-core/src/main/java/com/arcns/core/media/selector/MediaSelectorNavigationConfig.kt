package com.arcns.core.media.selector

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController


class MediaSelectorNavigationConfig {
    var selectorActionID: Int? = null
    var selectorDetailsActionID: Int? = null
    var onNavigationSelectorCallback: OnMediaSelectorNavigationCallback? = null
    var onNavigationSelectorDetailsCallback: OnMediaSelectorNavigationCallback? = null
    var onNavigationBackToStartCallback: OnMediaSelectorNavigationCallback? = null

    constructor(
        selectorActionID: Int,
        selectorDetailsActionID: Int
    ) {
        this.selectorActionID = selectorActionID
        this.selectorDetailsActionID = selectorDetailsActionID
    }

    constructor(
        onNavigationSelectorCallback: OnMediaSelectorNavigationCallback,
        onNavigationSelectorDetailsCallback: OnMediaSelectorNavigationCallback,
        onNavigationBackToStartCallback: OnMediaSelectorNavigationCallback
    ) {
        this.onNavigationSelectorCallback = onNavigationSelectorCallback
        this.onNavigationSelectorDetailsCallback = onNavigationSelectorDetailsCallback
        this.onNavigationBackToStartCallback = onNavigationBackToStartCallback
    }

    fun navigationMediaSelector(navController: NavController): Boolean {
        onNavigationSelectorCallback?.invoke(navController) ?: navController.navigate(
            selectorActionID ?: return false
        )
        return true
    }

    fun navigationMediaSelectorDetails(navController: NavController): Boolean {
        onNavigationSelectorDetailsCallback?.invoke(navController) ?: navController.navigate(
            selectorDetailsActionID ?: return false
        )
        return true
    }

    fun navigationBackToStart(navController: NavController): Boolean {
        if (onNavigationBackToStartCallback == null && selectorActionID == null) {
            navController.navigateUp()
            return true
        }
        onNavigationBackToStartCallback?.invoke(navController) ?: navController.popBackStack(
            navController.currentDestination?.getAction(
                selectorActionID ?: return false
            )?.destinationId
                ?: return false,
            true
        )
        return true
    }


}

data class MediaSelectorNavigationTitleConfig(
    val title: String? = null,
    val isCenter: Boolean = false
)

typealias OnMediaSelectorNavigationCallback = (NavController) -> Unit


fun Fragment.navigationDefaultMediaSelector(
    navigationConfig: MediaSelectorNavigationConfig? = null,
    navigationTitleConfig: MediaSelectorNavigationTitleConfig? = null
) {
    val mediaSelectorViewModel =
        ViewModelProvider(requireActivity()).get(MediaSelectorViewModel::class.java)
    if (navigationConfig != null) {
        mediaSelectorViewModel.defaultNavigationConfig = navigationConfig
    }
    if (navigationTitleConfig != null) {
        mediaSelectorViewModel.defaultNavigationTitleConfig = navigationTitleConfig
    }
    mediaSelectorViewModel.defaultNavigationConfig?.navigationMediaSelector(
        findNavController()
    )
}

fun Fragment.navigationDefaultMediaSelectorDetails(
    navigationConfig: MediaSelectorNavigationConfig? = null,
    navigationTitleConfig: MediaSelectorNavigationTitleConfig? = null
) {
    val mediaSelectorViewModel =
        ViewModelProvider(requireActivity()).get(MediaSelectorViewModel::class.java)
    if (navigationConfig != null) {
        mediaSelectorViewModel.defaultNavigationConfig = navigationConfig
    }
    if (navigationTitleConfig != null) {
        mediaSelectorViewModel.defaultNavigationTitleConfig = navigationTitleConfig
    }
    mediaSelectorViewModel.defaultNavigationConfig?.navigationMediaSelectorDetails(
        findNavController()
    )
}

fun Fragment.navigationDefaultMediaSelectorDetails(
    selectorDetailsActionID: Int,
    navigationTitleConfig: MediaSelectorNavigationTitleConfig? = null
) {
    val mediaSelectorViewModel =
        ViewModelProvider(requireActivity()).get(MediaSelectorViewModel::class.java)
    if (navigationTitleConfig != null) {
        mediaSelectorViewModel.defaultNavigationTitleConfig = navigationTitleConfig
    }
    findNavController().navigate(selectorDetailsActionID)
}