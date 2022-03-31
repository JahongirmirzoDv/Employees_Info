package me.ruyeo.employeesinfo.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.databinding.FragmentInformationBinding
import me.ruyeo.employeesinfo.utils.extensions.viewBinding

class InformationFragment : Fragment(R.layout.fragment_information) {
    private val binding by viewBinding { FragmentInformationBinding.bind(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}