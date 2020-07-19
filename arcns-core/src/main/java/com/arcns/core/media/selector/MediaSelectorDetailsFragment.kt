package com.zkxt.datamonitoring.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.arcns.core.R
import com.arcns.core.databinding.MediaSelectorFragmentImageSelectorDetailsBinding
import com.arcns.core.media.selector.MediaSelectorViewModel
import com.arcns.core.util.*
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector.*
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector.view.*
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector_details.*
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector_details.toolbar
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector_details.view.*
import kotlinx.android.synthetic.main.media_selector_fragment_image_selector_details.view.tvCenterTitle

/**
 * 照片选择器（图片详情）
 */
class MediaSelectorDetailsFragment : Fragment() {
    private var binding by autoCleared<MediaSelectorFragmentImageSelectorDetailsBinding>()
    private val viewModel by activityViewModels<MediaSelectorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            MediaSelectorFragmentImageSelectorDetailsBinding.inflate(inflater, container, false)
                .apply {
                    lifecycleOwner = this@MediaSelectorDetailsFragment
                    viewModel = this@MediaSelectorDetailsFragment.viewModel
                }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBar(
            toolbar,
            displayShowTitleEnabled = false,
            menuResId = if (viewModel.isSelector.value == true) R.menu.media_selector_menu_default else null
        ) {
            when (it.itemId) {
                R.id.mediaSelectorMenuDefaultItem -> viewModel.onCompleteSelectedMedias()
            }
        }
        setupImageSelector()
        setupOnBackPressedDelayedNavigateUp {
            if (viewModel.isOnlyPreview.value == true) {
                viewModel.destroy()
            }
            0
        }
    }

    private fun setTitle(title: String? = null) {
        val content =
            title ?: viewModel.defaultNavigationTitleConfig?.title ?: navigationDestinationLabel
            ?: return
        val isCenter = viewModel.defaultNavigationTitleConfig?.isCenter ?: false
        if (isCenter) {
            toolbar.title = null
            toolbar.tvCenterTitle.text = content
            toolbar.tvCenterTitle.visibility = View.VISIBLE
        } else {
            toolbar.title = content
            toolbar.tvCenterTitle.text = null
            toolbar.tvCenterTitle.visibility = View.GONE
        }
    }

    private fun setupImageSelector() {
        viewModel.selectedMedias.observe(viewLifecycleOwner, Observer {
            toolbar.menu.findItem(R.id.mediaSelectorMenuDefaultItem)?.isEnabled =
                viewModel.selectedMediasSize > 0
            toolbar.menu.findItem(R.id.mediaSelectorMenuDefaultItem)?.title =
                viewModel.completeButtonText
        })
        viewModel.currentMedia.observe(viewLifecycleOwner, Observer {
            setTitle(viewModel.detailsTitleText)
        })
        viewModel.toast.observe(viewLifecycleOwner, EventObserver {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })
        viewModel.eventCompleteSelectedMedias.observe(viewLifecycleOwner, EventObserver {
            viewModel.defaultNavigationConfig?.navigationBackToStart(findNavController())
                ?: findNavController().navigateUp()
        })
    }
}