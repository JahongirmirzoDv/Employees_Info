package me.ruyeo.employeesinfo.utils.extensions

import android.os.Build
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }

fun FragmentActivity.changeStatusBarColor(color: Int) {
    window.statusBarColor = color
}

fun FragmentActivity.changeNavigationBarColor(color: Int) {
    window.navigationBarColor = color
}