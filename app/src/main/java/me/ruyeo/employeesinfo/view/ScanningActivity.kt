package me.ruyeo.employeesinfo.view

import android.app.Dialog
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.biometrics.BiometricPrompt
import android.hardware.camera2.CameraCharacteristics
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.zxing.integration.android.IntentIntegrator
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import me.ruyeo.employeesinfo.R
import me.ruyeo.employeesinfo.SharedPref
import me.ruyeo.employeesinfo.broadcastreceiver.NetworkConnectionLiveData
import me.ruyeo.employeesinfo.data.api.ApiClient
import me.ruyeo.employeesinfo.data.api.ApiHelper
import me.ruyeo.employeesinfo.data.api.ApiService
import me.ruyeo.employeesinfo.data.local.AppDatabase
import me.ruyeo.employeesinfo.data.model.FlowModel
import me.ruyeo.employeesinfo.data.model.Recognition
import me.ruyeo.employeesinfo.data.model.Staff
import me.ruyeo.employeesinfo.faceDetect.customviews.OverlayView
import me.ruyeo.employeesinfo.faceDetect.tflite.SimilarityClassifier
import me.ruyeo.employeesinfo.faceDetect.tflite.TFLiteFaceRecognitionModel
import me.ruyeo.employeesinfo.faceDetect.tracking.MultiBoxTracker
import me.ruyeo.employeesinfo.faceDetect.ui.BaseCameraActivity
import me.ruyeo.employeesinfo.faceDetect.utils.BorderedText
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_INPUT_SIZE
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_IS_QUANTIZED
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_LABELS_FILE
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.API_MODEL_FILE
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.MAINTAIN_ASPECT
import me.ruyeo.employeesinfo.faceDetect.utils.Constants.TEXT_SIZE_DIP
import me.ruyeo.employeesinfo.faceDetect.utils.ImageUtils
import me.ruyeo.employeesinfo.repository.factory.ScanningViewModelFactory
import me.ruyeo.employeesinfo.utils.Utils
import me.ruyeo.employeesinfo.viewModel.ScanningViewModel
import java.io.IOException
import java.util.*


class ScanningActivity : BaseCameraActivity(), OnImageAvailableListener {

    private var job: Job? = null
    private val dialog by lazy { Dialog(this) }
    private var idWent: Int = 0

    /**
    Xodim kelgani haqidami yoki ketgani haqidami POST yuborish uchun
    ScanningFragment dan olindi
     */
    private val come by lazy { intent.extras!!.getInt("come", 0) }
    private lateinit var userData: MutableLiveData<Staff>

    /**
    Xodim keldi ketdisi haqida (api/v1/flow endpoint) request yuborilganmi yo`qmi
    bilish uchun. Agar true bo`lsa hozir request ketgan va hali javob qaytmagan (loading holatda)
    Agar false bo`lsa yuborilmagan yoki javob qaytgan yoki error qaytgan
     */
    private var isFlowPostSent = false

    /**
    Eng yangi tanib olingan yuz.
    Shu yuzga id siga ko`ra xodimlar database dan kerakli xodimni olib,
    keldi ketdi haqida request yuborish uchun
     */
    private val lastRecognized = MutableLiveData<Recognition>()

    /**
    Keldi ketdi haqida oxirgi muvaffaqiyatli so`rov yuborilgan xodim id si
    Agar /flow request ga javob kelganda, xodim kameraga qarab turgan bo`lsa,
    yana bir marta request ketib qolmasligi uchun
     */
    private var prevStaffId: Int? = null

    //ScanningFragment dan olindi
    private val scanningViewModel by lazy { initViewModel() }
    private val appDatabase by lazy { AppDatabase.getDatabase(this) }
    private var cancellationSignal: CancellationSignal? = null
    private var sensorOrientation: Int? = null
    private lateinit var date: String

    //FaceID uchun
    private var detector: SimilarityClassifier? = null
    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var computingDetection = false
    private var timestamp: Long = 0
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null
    private var tracker: MultiBoxTracker? = null
    private var borderedText: BorderedText? = null
    private var faceDetector: FaceDetector? = null
    private var portraitBmp: Bitmap? = null
    private var faceBmp: Bitmap? = null

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        findViewById<ImageView>(R.id.idScanningBack).setOnClickListener {
            onBackPressed()
        }

        when (come) {
            1 -> {
                findViewById<TextView>(R.id.idScanningTv).text = getString(R.string.kelish)
            }
            2 -> {
                findViewById<TextView>(R.id.idScanningTv).text = getString(R.string.ketish)
            }
            3 -> {
                findViewById<TextView>(R.id.idScanningTv).text =
                    getString(R.string.tushlikka_chiqish)
            }
            4 -> {
                findViewById<TextView>(R.id.idScanningTv).text =
                    getString(R.string.tushlikdan_qaytish)
            }
        }

        setOnClickListeners()
        setObservers()

        userData = MutableLiveData()

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        faceDetector = FaceDetection.getClient(options)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setOnClickListeners() {
        findViewById<FloatingActionButton>(R.id.fab_qr).setOnClickListener {
            val scanner = IntentIntegrator(this)
            scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            scanner.setCameraId(1)
            scanner.setBeepEnabled(false)
            scanner.initiateScan()
        }

        if (checkBiometricSupport()) {
            findViewById<FloatingActionButton>(R.id.fab_finger).setOnClickListener {
                val biometricPrompt = BiometricPrompt.Builder(this)
                    .setTitle("Title")
                    .setSubtitle("Subtitle")
                    .setDescription("Description")
                    .setNegativeButton(
                        "Cancel",
                        mainExecutor,
                        { _, _ -> notifyUser("Canceled") }).build()

                biometricPrompt.authenticate(
                    getCancellationSignal(),
                    mainExecutor,
                    authenticationCallback
                )
            }
        } else {
            findViewById<FloatingActionButton>(R.id.fab_finger).setOnClickListener {
                notifyUser("Sizning qurilmangizda bu imkoniyat mavjud emas")
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setObservers() {
        var flow : FlowModel? = null
        /**
        Oxirgi tanib olingan yuz o`zgarganda, mos xodim topilib,
        agar hozir loading bo`layotgan request bo`lmasa va bu xodimga hozirgina
        muvaffaqiyatli request amalga oshirilmagan bo`lsa,
        keldi ketdi haqida post yuborish
         */
        lastRecognized.observe(this) { recognition ->
            if (recognition.distance != -1f) {
                if (!isFlowPostSent && prevStaffId != recognition.id) {

                    val staff = appDatabase.getStaffDao().getStaffById(recognition.id)
                    userData.value = staff

//                    Log.d("Test", staff.firstName)

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
//                        Toast.makeText(this, "$isConnected", Toast.LENGTH_SHORT).show()
                        if (!isConnected) {
                            Log.d("Internet Not Available", "Internet Not Available")
                            when (come) {
                                1 -> {
//                                    scanningViewModel.deleteFlow()
                                    scanningViewModel.getFlow().observe(this){
                                        scanningViewModel.insertFlow(
                                            FlowModel(
                                                staff.id,
                                                cameTime = date,
                                                came_lunch = it?.came_lunch,
                                                went_lunch = it?.went_lunch,
                                                wentTime = it?.wentTime
                                            )
                                        )
                                    }
                                }
                                2 -> {
//                                    scanningViewModel.deleteFlow()
                                    scanningViewModel.getFlow().observe(this){
                                        scanningViewModel.insertFlow(
                                            FlowModel(
                                                staff.id,
                                                wentTime = date,
                                                cameTime = it?.cameTime,
                                                came_lunch = it?.came_lunch,
                                                went_lunch = it?.went_lunch
                                            )
                                        )
                                    }
                                }
                                3 -> {
//                                    scanningViewModel.deleteFlow()
                                    scanningViewModel.getFlow().observe(this){
                                        scanningViewModel.insertFlow(
                                            FlowModel(
                                                staff.id,
                                                went_lunch = date,
                                                came_lunch = it?.came_lunch,
                                                cameTime = it?.cameTime,
                                                wentTime = it?.wentTime
                                            )
                                        )
                                    }
                                }
                                4 -> {
//                                    scanningViewModel.deleteFlow()
                                    scanningViewModel.getFlow().observe(this){
                                        scanningViewModel.insertFlow(
                                            FlowModel(
                                                staff.id,
                                                came_lunch = date,
                                                went_lunch = it?.went_lunch,
                                                cameTime = it?.cameTime,
                                                wentTime = it?.wentTime
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            idWent = SharedPref(this).getId()
                            val hashMap = HashMap<String, Any>()
                            val hashMapAction = HashMap<String, Any>()
                            when (come) {
                                1 -> {
                                    hashMap["staff"] = staff.id
                                    hashMapAction["staff"] = staff.id
                                    hashMap["came"] = date
                                    scanningViewModel.sendFlow(hashMap)
                                    scanningViewModel.sendFlowAction(hashMap)
                                }
                                2 -> {
                                    hashMap["staff"] = staff.id
                                    hashMapAction["staff"] = staff.id
                                    hashMap["went"] = date
                                    idWent = SharedPref(this).getId()
                                    scanningViewModel.sendFlowWent(idWent, hashMap)
                                    scanningViewModel.sendFlowWentAction(idWent,hashMap)
                                }
                                3 -> {
                                    hashMap["staff"] = staff.id
                                    hashMapAction["staff"] = staff.id
                                    hashMap["went_lunch"] = date
                                    idWent = SharedPref(this).getId()
                                    scanningViewModel.sendFlowWent(idWent, hashMap)
                                    scanningViewModel.sendFlowWentAction(idWent, hashMap)
                                }
                                4 -> {
                                    hashMap["staff"] = staff.id
                                    hashMapAction["staff"] = staff.id
                                    hashMap["came_lunch"] = date
                                    idWent = SharedPref(this).getId()
                                    scanningViewModel.sendFlow(hashMap)
                                    scanningViewModel.sendFlowAction(hashMap)
                                }
                            }

                        }
                        isFlowPostSent = true
                    }
                }
            }
//            else{
//                val repeat: Button
//                dialog.apply {
//                    val v = LayoutInflater.from(context).inflate(R.layout.employe_not_found, null)
//
//                    setContentView(v)
//                    window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//                    window?.setGravity(Gravity.BOTTOM)
//                    window?.setBackgroundDrawableResource(R.drawable.layout_bg)
//
//                    repeat = v.findViewById(R.id.sell_btn)
//
//                    job = CoroutineScope(Dispatchers.Main).launch {
//                        delay(7000)
//                        dismiss()
//                    }
//
//                    repeat.setOnClickListener {
//                        dismiss()
//                    }
//                    show()
//                }
//            }
        }

//        NetworkConnectionLiveData(applicationContext).observe(this,{isConnected ->
//            if (!isConnected){
//                Log.d("Internet Not Available","Internet Not Available")
//            }else{
//                if (scanningViewModel.getFlow().value != null){
//                    val flowModel = scanningViewModel.getFlow()
//                    val hashMap = HashMap<String, Any>()
//                    when(come) {
//                        1 -> {
//                            hashMap["staff"] = flowModel.observe(this,{
//                                it.staffId
//                            })
//                            hashMap["came"] = flowModel.observe(this,{
//                                it.cameTime
//                                it.cameTime?.let { it1 -> Log.d("Test", it1) }
//                            })
//                            scanningViewModel.sendFlow(hashMap)
//                            scanningViewModel.deleteFlow()
//                        }
//                        2 -> {
//                            hashMap["staff"] = flowModel.observe(this,{
//                                it.staffId
//                            })
//                            hashMap["went"] = flowModel.observe(this,{
//                                it.wentTime
//                            })
//                            idWent = SharedPref(this).getId()
//                            scanningViewModel.sendFlowWent(idWent, hashMap)
//                            scanningViewModel.deleteFlow()
//                        }
//                        3 -> {
//                            hashMap["staff"] = flowModel.observe(this,{
//                                it.staffId
//                            })
//                            hashMap["went_lunch"] = flowModel.observe(this,{
//                                it.went_lunch
//                            })
//                            idWent = SharedPref(this).getId()
//                            scanningViewModel.sendFlowWent(idWent,hashMap)
//                            scanningViewModel.deleteFlow()
//                        }
//                        4 -> {
//                            hashMap["staff"] = flowModel.observe(this,{
//                                it.staffId
//                            })
//                            hashMap["came_lunch"] = flowModel.observe(this,{
//                                it.came_lunch
//                            })
//                            scanningViewModel.sendFlow(hashMap)
//                            scanningViewModel.deleteFlow()
//                        }
//                    }
//                }
//            }
//        })

        lifecycleScope.launchWhenCreated {
            /**
            Xodim keldi ketdisiga request holatini kuzatish uchun
             */
            scanningViewModel.flowState.collect {
                when (it) {
                    is ScanningViewModel.FlowUiState.ERROR -> {
                        /**
                        Xatolik bo`lsa 4sekund toast ko`rsatib, keyin keyingi so`rovga
                        ruxsat berish
                         */
//                        Utils.toastLong(this@ScanningActivity, it.message)
//                        Log.d("errs", it.message + "sa")
                        delay(4000)
                        isFlowPostSent = false
                    }
                    is ScanningViewModel.FlowUiState.SUCCESS -> {
                        /**
                        Success bo`lsa 4sekund xodim ismi va keldi ketdi vaqtini ko`rsatib,
                        keyin keyingi requestga ruxsat berish, va bu xodim id sini [prevStaffId]
                        ga saqlab  qo`yish
                         */
                        Log.d("wehns", "ok")
                        SharedPref(this@ScanningActivity).setId(it.response.id)
                        Log.d("dlowsd", it.response.id.toString())
                        userData.observe(this@ScanningActivity) {
                            Log.d("sendsa", "ok")
                            showDialogs(
                                it.id,
                                it.firstName,
                                it.lastName,
                                it.qrCode,
                                it.image,
                                it.company,
                                it.position
                            )
                            Log.d("sendsx", "ok")
                            prevStaffId = it.id
                        }

                        isFlowPostSent = false
                    }
                    is ScanningViewModel.FlowUiState.LOADING -> {
                        /**
                        Xodim keldi ketdisiga request amalga oshirilyapti
                         */
                        isFlowPostSent = true
//                        Utils.toast(this@ScanningActivity, "Yuborilmoqda")
                    }
                    else -> Unit
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            /**
            Xodim keldi ketdisiga request holatini kuzatish uchun
             */
            scanningViewModel.flowStateWent.collect {
                when (it) {
                    is ScanningViewModel.FlowWentUiState.ERROR -> {
                        /**
                        Xatolik bo`lsa 4sekund toast ko`rsatib, keyin keyingi so`rovga
                        ruxsat berish
                         */
//                        Utils.toastLong(this@ScanningActivity, it.message)
//                        Log.d("errs", it.message + "sa")
                        delay(4000)
                        isFlowPostSent = false
                    }
                    is ScanningViewModel.FlowWentUiState.SUCCESS -> {
                        /**
                        Success bo`lsa 4sekund xodim ismi va keldi ketdi vaqtini ko`rsatib,
                        keyin keyingi requestga ruxsat berish, va bu xodim id sini [prevStaffId]
                        ga saqlab  qo`yish
                         */
                        Log.d("wehnsnh", "ok")
                        userData.observe(this@ScanningActivity) {
                            Log.d("sendsa", "ok")
                            showDialogs(
                                it.id,
                                it.firstName,
                                it.lastName,
                                it.qrCode,
                                it.image,
                                it.company,
                                it.position
                            )
                            Log.d("sendsx", "ok")
                            prevStaffId = it.id
                            Log.d("sendsx", "ok")
                            prevStaffId = it.id
                        }

                        isFlowPostSent = false
                    }
                    is ScanningViewModel.FlowWentUiState.LOADING -> {
                        /**
                        Xodim keldi ketdisiga request amalga oshirilyapti
                         */
                        isFlowPostSent = true
//                        Utils.toast(this@ScanningActivity, "Yuborilmoqda")
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
    ScanningViewModel yaratish
     */
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Utils.toast(this, "Cancelled")
            } else {
                Log.d("sends0", "ok")
                val mcurrentTime = Calendar.getInstance()
                val month = mcurrentTime.get(Calendar.MONTH) + 1

                date = "${mcurrentTime.get(Calendar.YEAR)}-${
                    String.format("%02d", month)
                }-${mcurrentTime.get(Calendar.DAY_OF_MONTH)}T${mcurrentTime.get(Calendar.HOUR_OF_DAY)}:${
                    mcurrentTime.get(
                        String.format("%02d", Calendar.MINUTE).toInt()
                    )
                }"

                val id = result.contents.filter {
                    it.isDigit()
                }
                val staff = appDatabase.getStaffDao().getStaffById(id.toInt())
                Log.d("sends1", "ok")
                userData.value = staff
                idWent = SharedPref(this).getId()
                val hashMap = HashMap<String, Any>()
                when (come) {
                    1 -> {
                        hashMap["staff"] = staff.id
                        hashMap["came"] = date
                        scanningViewModel.sendFlow(hashMap)
                    }
                    2 -> {
                        hashMap["staff"] = staff.id
                        hashMap["went"] = date
                        idWent = SharedPref(this).getId()
                        scanningViewModel.sendFlowWent(idWent, hashMap)
                    }
                    3 -> {
                        hashMap["staff"] = staff.id
                        hashMap["went_lunch"] = date
                        idWent = SharedPref(this).getId()
                        scanningViewModel.sendFlowWent(idWent, hashMap)
                    }
                    4 -> {
                        hashMap["staff"] = staff.id
                        hashMap["came_lunch"] = date
                        idWent = SharedPref(this).getId()
                        scanningViewModel.sendFlowWent(idWent, hashMap)
                    }
                }
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
        val keyManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyManager.isKeyguardSecure) {
            notifyUser("Biometric password o'rnatilmagan")
            return false
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.USE_BIOMETRIC
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyUser("Is not enabled")
            return false
        }

        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else true
    }

    private fun notifyUser(message: String) {
        Utils.toast(this, message)
    }

    private fun showDialogs(
        id: Int,
        firstName: String,
        lastNmae: String,
        qrCode: String,
        image: String,
        company: String,
        position: String
    ) {
        dialog.apply {
            val v = LayoutInflater.from(context).inflate(R.layout.dialog, null)

            setContentView(v)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setGravity(Gravity.BOTTOM)
            window?.setBackgroundDrawableResource(R.drawable.layout_bg)
            val staffImage = v.findViewById<CircleImageView>(R.id.idStaffImage)
            val qrcode = v.findViewById<ImageView>(R.id.idStaffQrCode)
            val staffName = v.findViewById<TextView>(R.id.idStaffName)
            val staff = v.findViewById<TextView>(R.id.idPosition)
            val cameOrWentTime = v.findViewById<TextView>(R.id.idTime)
            val fillial = v.findViewById<TextView>(R.id.idSubsidiary)
            val sell = v.findViewById<Button>(R.id.idConfirm)

            job = CoroutineScope(Dispatchers.Main).launch {
                delay(7000)
                dismiss()
            }

            when (come) {
                1 -> {
                    v.findViewById<TextView>(R.id.idScanningTv).text = getString(R.string.kelish)
                }
                2 -> {
                    v.findViewById<TextView>(R.id.idScanningTv).text = getString(R.string.ketish)
                }
                3 -> {
                    v.findViewById<TextView>(R.id.idScanningTv).text =
                        getString(R.string.tushlikka_chiqish)
                }
                4 -> {
                    v.findViewById<TextView>(R.id.idScanningTv).text =
                        getString(R.string.tushlikdan_qaytish)
                }
            }

            Log.d("logick", "ok")
            Glide.with(v).load(qrCode).into(qrcode)
            Glide.with(v).asBitmap().load(image).into(staffImage)
            staffName.text = firstName + " " + lastNmae
            cameOrWentTime.text = date
            staff.text = position
            fillial.text = company

            sell.setOnClickListener {
                dismiss()
            }
            show()
        }
    }

    /**
    Bundan pasi FaceID
    Log lar kommentga olingan tekshirish uchun log lar ni kommentdan chiqarish mumkin
     */
    override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
        borderedText!!.setTypeface(Typeface.MONOSPACE)
        tracker = MultiBoxTracker(this)
        try {
            detector = TFLiteFaceRecognitionModel.create(
                assets,
                API_MODEL_FILE,
                API_LABELS_FILE,
                API_INPUT_SIZE,
                API_IS_QUANTIZED,
                this
            )
        } catch (e: IOException) {
            e.printStackTrace()
//            Log.e("onPreviewSizeChosen", "Exception initializing classifier")
            Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            ).show()
            finish()
        }
        previewWidth = size!!.width
        previewHeight = size.height
        sensorOrientation = rotation - getScreenOrientation()
//        Log.e(
//            "onPreviewSizeChosen",
//            "Camera orientation relative to screen canvas: $sensorOrientation"
//        )
//        Log.e("onPreviewSizeChosen", "Initializing at size $previewWidth*$previewHeight")
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        val targetW: Int
        val targetH: Int
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth
            targetW = previewHeight
        } else {
            targetW = previewWidth
            targetH = previewHeight
        }
        val cropW = (targetW / 2.0).toInt()
        val cropH = (targetH / 2.0).toInt()
        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        faceBmp =
            Bitmap.createBitmap(API_INPUT_SIZE, API_INPUT_SIZE, Bitmap.Config.ARGB_8888)
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            cropW, cropH,
            sensorOrientation!!, MAINTAIN_ASPECT
        )

        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        val frameToPortraitTransform: Matrix = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight,
            targetW, targetH,
            sensorOrientation!!, MAINTAIN_ASPECT
        )
        findViewById<OverlayView>(R.id.tracking_overlay_view).addCallback(
            object : OverlayView.DrawCallback {
                override fun drawCallback(canvas: Canvas?) {
                    tracker!!.draw(canvas!!)
                    if (isDebug()) {
                        tracker!!.drawDebug(canvas)
                    }
                }
            })
        tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }

    override fun processImage() {
        ++timestamp
        val currTimestamp = timestamp
        findViewById<OverlayView>(R.id.tracking_overlay_view).postInvalidate()

        if (computingDetection) {
            readyForNextImage()
            return
        }
        computingDetection = true
//        Log.e("processImage", "Preparing image $currTimestamp for detection in bg thread.")
        rgbFrameBitmap!!.setPixels(
            getRgbBytes(),
            0,
            previewWidth,
            0,
            0,
            previewWidth,
            previewHeight
        )
        readyForNextImage()
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
        val image = InputImage.fromBitmap(croppedBitmap!!, 0)
        faceDetector!!
            .process(image)
            .addOnSuccessListener(OnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    updateResults(currTimestamp, LinkedList<Recognition>())
                    return@OnSuccessListener
                }
                runInBackground {
                    onFacesDetected(currTimestamp, faces)
                }
            })
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_camera
    }

    override fun setNumThreads(numThreads: Int) {
//        runInBackground { detector!!.setNumThreads(numThreads) }
    }

    private fun createTransform(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int
    ): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
//            if (applyRotation % 90 != 0) {
//                Log.e("createTransform", "Rotation of $applyRotation % 90 != 0")
//            }
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)
            matrix.postRotate(applyRotation.toFloat())
        }
        if (applyRotation != 0) {
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }

    private fun showAddFaceDialog(rec: Recognition) {
    }

    private fun updateResults(currTimestamp: Long, mappedRecognitions: List<Recognition>) {
        tracker!!.trackResults(mappedRecognitions, currTimestamp)
        findViewById<OverlayView>(R.id.tracking_overlay_view).postInvalidate()
        computingDetection = false
        if (mappedRecognitions.isNotEmpty()) {
//            Log.e("updateResults", "Adding results")
            val rec: Recognition = mappedRecognitions[0]
            if (rec.extra != null) {
                showAddFaceDialog(rec)
            }
        }
//        val handler = Handler(Looper.getMainLooper())
//        handler.postDelayed({
//            mappedRecognitions.forEach {
////            if (it.distance != -1f) {
//            lastRecognized.postValue(it)
////            }
//        }
//        }, 5000)
        mappedRecognitions.forEach {
            if (it.distance != -1f) {
                lastRecognized.postValue(it)
            }
        }
        runOnUiThread {
            showInference(lastProcessingTimeMs.toString() + "ms")
        }
    }

    private fun onFacesDetected(currTimestamp: Long, faces: List<Face>) {
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
        val canvas = Canvas(cropCopyBitmap!!)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        val mappedRecognitions: MutableList<Recognition> = LinkedList<Recognition>()
        val sourceW = rgbFrameBitmap!!.width
        val sourceH = rgbFrameBitmap!!.height
        val targetW = portraitBmp!!.width
        val targetH = portraitBmp!!.height
        val transform = createTransform(
            sourceW,
            sourceH,
            targetW,
            targetH,
            sensorOrientation!!
        )
        val cv = Canvas(portraitBmp!!)
        cv.drawBitmap(rgbFrameBitmap!!, transform, null)
        val cvFace = Canvas(faceBmp!!)
        val saved = false
        for (face in faces) {
//            Log.e("onFacesDetected", "Face: $face")
//            Log.e("onFacesDetected", "Running detection on face $currTimestamp")
            val boundingBox = RectF(face.boundingBox)
            val goodConfidence = true
            if (goodConfidence) {
                cropToFrameTransform!!.mapRect(boundingBox)
                val faceBB = RectF(boundingBox)
                transform.mapRect(faceBB)
                val sx = API_INPUT_SIZE.toFloat() / faceBB.width()
                val sy = API_INPUT_SIZE.toFloat() / faceBB.height()
                val matrix = Matrix()
                matrix.postTranslate(-faceBB.left, -faceBB.top)
                matrix.postScale(sx, sy)
                cvFace.drawBitmap(portraitBmp!!, matrix, null)
                var id = 0
                var label = ""
                var confidence = -1f
                var color = Color.BLUE
                var extra: Any? = null
                val crop: Bitmap? = null
                val startTime = SystemClock.uptimeMillis()
                val resultsAux: List<Recognition?> = detector!!.recognizeImage(faceBmp!!, false)
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                if (resultsAux.isNotEmpty()) {
                    val result = resultsAux[0]!!
                    extra = result.extra
                    result.distance.let {
                        if (it < 1f) {
                            id = result.id
                            confidence = it
                            label = result.title
                            color = Color.GREEN
                        }
                    }
                }
                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {
                    val flip = Matrix()
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1f, -1f, previewWidth / 2.0f, previewHeight / 2.0f)
                    } else {
                        flip.postScale(-1f, 1f, previewWidth / 2.0f, previewHeight / 2.0f)
                    }
                    flip.mapRect(boundingBox)
                }
                val result = Recognition(
                    id, label, confidence, boundingBox
                )
                result.color = color
                result.location = boundingBox
                result.extra = extra
                result.crop = crop
                mappedRecognitions.add(result)
            }
        }
        updateResults(currTimestamp, mappedRecognitions)
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()

        if (dialog != null && dialog.isShowing()) {
            dialog.cancel();

        }
    }
}