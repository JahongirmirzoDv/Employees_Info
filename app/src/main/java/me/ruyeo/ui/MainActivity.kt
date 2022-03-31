package me.ruyeo.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import kotlinx.coroutines.*
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.SharedPref
import me.ruyeo.employeesinfo.broadcastreceiver.NetworkConnectionLiveData
import me.ruyeo.employeesinfo.data.api.ApiClient
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.api.ApiService
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.repository.factory.ScanningViewModelFactory
import me.ruyeo.employeesinfo.viewModel.ScanningViewModel
import java.util.*

class MainActivity : AppCompatActivity() {
    private val navController by lazy { findNavController(R.id.nav_host) }
    private lateinit var date: String
    private val scanningViewModel by lazy { initViewModel() }
    private var idWent: Int = 0
    private val appDatabase by lazy { AppDatabase.getDatabase(this) }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        idWent = SharedPref(this).getId()

        val mcurrentTime = Calendar.getInstance()
        val month = mcurrentTime.get(Calendar.MONTH) + 1

        date = "${mcurrentTime.get(Calendar.YEAR)}-${
            String.format(
                "%02d",
                month
            )
        }-${mcurrentTime.get(Calendar.DAY_OF_MONTH)} ${mcurrentTime.get(Calendar.HOUR_OF_DAY)}:${
            mcurrentTime.get(
                String.format("%02d", Calendar.MINUTE).toInt()
            )
        }"

        NetworkConnectionLiveData(applicationContext).observe(this) { isConnected ->
            if (isConnected){
                GlobalScope.launch(Dispatchers.Main) {
                    async {
                        val hashMap1 = HashMap<String, Any>()
                        scanningViewModel.getFlow()
                            .observe(this@MainActivity) { flow ->
                                if (flow.cameTime != null) {
                                    hashMap1["staff"] = flow.staffId
                                    hashMap1["came"] = flow.cameTime
                                    scanningViewModel.sendFlow(hashMap1)
                                    Log.e("offline_work", "offline come sended")
                                }
                                if (flow.wentTime != null) {
                                    hashMap1["staff"] = flow.staffId
                                    hashMap1["went"] = date
                                    idWent = SharedPref(this@MainActivity).getId()
                                    scanningViewModel.sendFlowWent(idWent, hashMap1)
                                    Log.e("offline_work", "offline went sended")
                                }
                                if (flow.went_lunch != null) {
                                    hashMap1["staff"] = flow.staffId
                                    hashMap1["went_lunch"] = date
                                    idWent = SharedPref(this@MainActivity).getId()
                                    scanningViewModel.sendFlowWent(idWent, hashMap1)
                                    Log.e("offline_work", "offline went_lunch sended")
                                }
                                if (flow.came_lunch != null) {
                                    hashMap1["staff"] = flow.staffId
                                    hashMap1["came_lunch"] = flow.came_lunch
                                    idWent = SharedPref(this@MainActivity).getId()
                                    scanningViewModel.sendFlow(hashMap1)
                                    Log.e("offline_work", "offline came_lunch sended")
                                }
                            }
                    }
                }
            }
        }
    }
    private fun initViewModel() = ViewModelProvider(
        this,
        ScanningViewModelFactory(
            ApiHelper(
                ApiClient.createServiceWithAuth(
                    ApiService::class.java,
                    this
                )
            ), AppDatabase.getDatabase(this),
            appDatabase.getFlowDao()
        )
    ).get(ScanningViewModel::class.java)
}