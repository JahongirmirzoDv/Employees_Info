package me.ruyeo.employeesinfo.faceDetect.utils

import android.app.Dialog
import android.os.Bundle
import android.util.Size
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.util.Comparator

/**
 *Created by farrukh_kh on 6/8/21 6:57 PM
 *kh.farrukh.facerecognition.utils
 **/
class ErrorDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity())
            .setMessage(requireArguments().getString(ARG_MESSAGE))
            .setPositiveButton(
                android.R.string.ok
            ) { _, _ -> requireActivity().finish() }
            .create()
    }

    companion object {
        private const val ARG_MESSAGE = "message"
        fun newInstance(message: String?): ErrorDialog {
            val dialog = ErrorDialog()
            val args = Bundle()
            args.putString(ARG_MESSAGE, message)
            dialog.arguments = args
            return dialog
        }
    }
}

interface ConnectionCallback {
    fun onPreviewSizeChosen(size: Size?, cameraRotation: Int)
}

internal class CompareSizesByArea : Comparator<Size?> {
    override fun compare(lhs: Size?, rhs: Size?): Int {
        return java.lang.Long.signum(
            lhs!!.width.toLong() * lhs.height - rhs!!.width.toLong() * rhs.height
        )
    }
}