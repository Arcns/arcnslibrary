package com.example.arcns.ui

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.arcns.core.util.setActionBarAsToolbar
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.example.arcns.databinding.FragmentEmptyBinding
import com.example.arcns.viewmodel.*
import kotlinx.android.synthetic.main.fragment_empty.*

/**
 *
 */
class FragmentEmpty : Fragment() {
    private var binding by autoCleared<FragmentEmptyBinding>()
    private val viewModel by viewModels<ViewModelEmpty>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmptyBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentEmpty
            viewModel = this@FragmentEmpty.viewModel
        }
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setActionBarAsToolbar(toolbar)
        setupResult()
    }

    private fun setupResult() {
        viewModel.toast.observe(this, EventObserver {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        })
    }
}