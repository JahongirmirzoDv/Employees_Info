package me.ruyeo.employeesinfo.view

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.SharedPref
import me.ruyeo.employeesinfo.data.api.ApiClient
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.api.ApiService
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.data.model.TokenManager
import me.ruyeo.employeesinfo.databinding.FragmentLoginBinding
import me.ruyeo.employeesinfo.repository.factory.LoginViewModelFactory
import me.ruyeo.employeesinfo.utils.Utils
import me.ruyeo.employeesinfo.utils.extensions.viewBinding
import me.ruyeo.employeesinfo.viewModel.LoginViewModel


class LoginFragment : Fragment(R.layout.fragment_login) {
    private val binding by viewBinding { FragmentLoginBinding.bind(it) }
    private val navController by lazy { Navigation.findNavController(binding.root) }
    private val viewModel by lazy { setupViewModel() }
    private val progressDialog by lazy { setupDialog() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(requireActivity())
                .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) { /* ... */
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) { /* ... */
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) { /* ... */
                    }
                }).check()
        }

        TokenManager.getInstance(
            requireActivity().getSharedPreferences(
                "prefs",
                Context.MODE_PRIVATE
            )
        ).also {
            if (it?.token == ""){
                setupUI()
                setupObservers()
            }else{
                navController.navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }


    }

    private fun setupUI() {
        binding.loginBtn.setOnClickListener {
            viewModel.clearAll()
            viewModel.login(binding.loginEt.text.toString(), binding.passwordEt.text.toString())
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.loginState.collect {
                when (it) {
                    is LoginViewModel.LoginUiState.LOADING -> {
                        progressDialog.show()
                    }
                    is LoginViewModel.LoginUiState.SUCCESS -> {
                        progressDialog.dismiss()
                        SharedPref(requireContext()).setFirstEnter(true)
                        TokenManager.getInstance(
                            requireActivity().getSharedPreferences(
                                "prefs",
                                Context.MODE_PRIVATE
                            )
                        ).also { tokenManager ->
                            tokenManager?.saveToken(it.user.access)
                            tokenManager?.saveRefreshToken(it.user.refresh)
                        }

                    //    SharedPref(requireContext()).setToken(it.user.access) // user datalarni saqlash kerak
                        viewModel.getAllStaff()
                    }
                    is LoginViewModel.LoginUiState.ERROR -> {
                        progressDialog.dismiss()
                        Utils.toast(requireContext(), it.message)
                    }
                    else -> Unit
                }
            }
        }

        /**
        Backenddan olinayotgan xodimlar ro`yxatini kuzatish uchun
        [LoginViewModel] va [me.ruyeo.employeesinfo.repository.LoginRepository]
        larni ko`rib chiqing
         */
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.staffState.collect {
                when (it) {
                    is LoginViewModel.StaffListState.LOADING -> {
                        progressDialog.setMessage("Xodimlar ro`yxati yuklanmoqda")
                        progressDialog.show()
                        Log.d("loadew","true")
                    }
                    is LoginViewModel.StaffListState.SUCCESS -> {
                        Log.d("succeslogo","true")
                        progressDialog.dismiss()
                        Utils.toast(
                            requireContext(),
                            "${it.staffList.size} ta xodim ma`lumotlari yuklandi"
                        )
                        navController.navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    is LoginViewModel.StaffListState.ERROR -> {
                        progressDialog.dismiss()
                        Utils.toast(requireContext(), it.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun setupViewModel() = ViewModelProvider(
        this, LoginViewModelFactory(
            ApiHelper(
                ApiClient.createServiceWithAuth(ApiService::class.java, requireContext())
            ),
            AppDatabase.getDatabase(requireContext()),
            requireContext()
        )
    ).get(LoginViewModel::class.java)

    private fun setupDialog() = SpotsDialog.Builder()
        .setContext(requireContext())
        .setMessage("Yuklanmoqda")
        .setCancelable(false)
        .build()
}