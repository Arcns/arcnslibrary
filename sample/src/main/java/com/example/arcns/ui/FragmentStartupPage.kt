package com.example.arcns.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.arcns.core.util.EventObserver
import com.arcns.core.util.autoCleared
import com.example.arcns.databinding.FragmentMapBinding
import com.example.arcns.databinding.FragmentStartupPageBinding
import com.example.arcns.viewmodel.ViewModelActivityMain
import com.example.arcns.viewmodel.ViewModelStartupPage

/**
 * 启动页
 */
class FragmentStartupPage : Fragment() {
    private var binding by autoCleared<FragmentStartupPageBinding>()
//private lateinit var binding:FragmentStartupPageBinding
    private val viewModel by viewModels<ViewModelStartupPage>()
    private val viewModelActivityMain by activityViewModels<ViewModelActivityMain>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStartupPageBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@FragmentStartupPage
            viewModel = this@FragmentStartupPage.viewModel
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupGo()
    }

    private fun setupGo() {
        viewModel.goLogin.observe(this, EventObserver {
            it?.apply {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        })
        viewModel.goMain.observe(this, EventObserver {
            findNavController().navigate(FragmentStartupPageDirections.actionFragmentStartupPageToFragmentMain())
        })
    }

}