package me.ruyeo.employeesinfo.view

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.zxing.integration.android.IntentIntegrator
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.databinding.FragmentScanningBinding
import me.ruyeo.employeesinfo.utils.Utils
import me.ruyeo.employeesinfo.utils.extensions.viewBinding


/**
 DIQQAT! Bu fragment o`rniga [ScanningActivity] ishlatiladi.
 Bu yerdagi barcha funksional activity ga olib o`tildi
 TODO: Har ehtimolga qarshi bu fragment delete qilinmadi. Activity va Fragment
 TODO: kodlarini solishtirib tekshirib ko`ring
 */
class ScanningFragment : Fragment(R.layout.fragment_scanning) {
    private val binding by viewBinding { FragmentScanningBinding.bind(it) }
    private val navController by lazy { Navigation.findNavController(binding.root) }
    private var cancellationSignal: CancellationSignal? = null
    private val isCome by lazy { ScanningFragmentArgs.fromBundle(requireArguments()).isCome }
    private val authenticationCallback
        get() = @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                notifyUser("Success")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                notifyUser("Auth error $errString")
            }
        }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tooolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.qrcode.setOnClickListener {
            val scanner = IntentIntegrator.forSupportFragment(this)
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            scanner.setCameraId(1)
            scanner.setBeepEnabled(false)
            scanner.initiateScan()
        }
        checkBiometricSupport()

        //     BioAuthManager.Builder(context, BioAuthSettings).build()

        binding.fingerprint.setOnClickListener {
            val biometricPrompt = BiometricPrompt.Builder(requireContext())
                .setTitle("Title")
                .setSubtitle("Subtitle")
                .setDescription("Description")
                .setNegativeButton(
                    "Cancel",
                    requireActivity().mainExecutor,
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        notifyUser("Canceled")
                    }).build()

            biometricPrompt.authenticate(
                getCancellationSignal(),
                requireActivity().mainExecutor,
                authenticationCallback
            )
        }

        binding.face.setOnClickListener {
            Intent(requireContext(), ScanningActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Utils.toast(requireContext(), "Cancelled")
            } else {
                Utils.toast(requireContext(), "Scanned: " + result.contents)
                showDialog()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("auth cancel by user")
        }
        return cancellationSignal as CancellationSignal
    }

    private fun checkBiometricSupport(): Boolean {
        val keyManager =
            requireActivity().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyManager.isKeyguardSecure) {
            notifyUser("Biometric password o'rnatilmagan")
            return false
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.USE_BIOMETRIC
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyUser("Is not enabled")
            return false
        }

        return if (requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true

    }

    private fun notifyUser(message: String) {
        Utils.toast(requireContext(), message)
    }

    private fun showDialog() {
        Dialog(requireActivity()).apply {
            val v = if (isCome == 1) {
                LayoutInflater.from(context).inflate(R.layout.dialog, null)
            } else {
                LayoutInflater.from(context).inflate(R.layout.employe_not_found, null)
            }

            setContentView(v)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val sell = v.findViewById<Button>(R.id.sell_btn)

            sell.setOnClickListener {
                dismiss()
            }
            show()
        }
    }
}