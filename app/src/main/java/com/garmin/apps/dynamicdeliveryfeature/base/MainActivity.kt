
package com.garmin.apps.dynamicdeliveryfeature.base

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.splitinstall.*
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.android.synthetic.main.activity_main.*

private const val packageName = "com.garmin.apps.dynamicdeliveryfeature"
private const val imageFeatureClassName = "$packageName.ImageActivity"
private const val imageLargeFeatureClassName = "$packageName.ImageLargeActivity"


class MainActivity : AppCompatActivity() {

    private val TAG = "DynamicDeliveryFeature"

    /** Listener used to handle changes in state for install requests. */
    private val listener = SplitInstallStateUpdatedListener { state ->
        val multiInstall = state.moduleNames().size > 1
        state.moduleNames().forEach { name ->
            // Handle changes in state.
            when (state.status()) {
                SplitInstallSessionStatus.DOWNLOADING -> {
                    Log.d(
                        TAG,
                        "SplitInstallStatus : Downloading $name : ${state.bytesDownloaded().toInt()} / ${state.totalBytesToDownload()}"
                    )
                    displayLoadingState(
                        state,
                        "Downloading $name : ${state.bytesDownloaded().toInt()} / ${state.totalBytesToDownload()} "
                    )
                }
                SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                    Log.d(TAG,"SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION")
                    //This may occur when attempting to download a sufficiently large module (4 MB or more).In our case image_large_feature
                    startIntentSender(state.resolutionIntent()?.intentSender, null, 0, 0, 0)
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    Log.d(TAG,"SplitInstallSessionStatus.INSTALLED")
                    //Launch module on successful install
                    launchActivity(name)
                }

                // SplitInstallSessionStatus.INSTALLING -> displayLoadingState(state, "Installing $name")
                SplitInstallSessionStatus.FAILED -> {
                    Log.d(TAG,"Error: ${state.errorCode()} for module ${state.moduleNames()}")
                    displayToast("Error: ${state.errorCode()} for module ${state.moduleNames()}")
                }
            }
        }
    }

    //Dynamic Modules IDs
    private val moduleImage by lazy { getString(R.string.title_image_feature) }
    private val moduleLargeImage by lazy { getString(R.string.title_image_large_feature) }

    private lateinit var splitManager: SplitInstallManager
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        //InitView
        progressBar = findViewById(R.id.progress_bar)
        progressText = findViewById(R.id.progressbar_textview)

        //Init SplitInstallManager
        splitManager = SplitInstallManagerFactory.create(this)

        btn_launch_img_module.setOnClickListener{onluLaunchModule(moduleImage)}
        btn_launch_img_large_module.setOnClickListener { onluLaunchModule(moduleLargeImage) }
        btn_req_image_feature.setOnClickListener { requestModule(moduleImage) }
        btn_req_image_large_feature.setOnClickListener { requestModule(moduleLargeImage) }
        btn_req_deferred_image_feature.setOnClickListener { installImageFeatureDeferred() }
        btn_uninstall_features.setOnClickListener { uninstallAllModules() }


        btn_launch_img_module.setOnLongClickListener {
            displayToast("This will launch the image module if its installed ")
            true
        }

        btn_launch_img_large_module.setOnLongClickListener {
            displayToast("This will launch the image large module if its installed ")
            true
        }

        btn_req_image_feature.setOnLongClickListener {
            displayToast("This will download and install Image Feature Module from PlayStore! If the Module is already installed then it will launch it.")
            true
        }

        btn_req_image_large_feature.setOnLongClickListener {
            displayToast("This will download and install Image Large Feature Module from PlayStore! This module requires User Confirmation before download.")
            true
        }

        btn_req_deferred_image_feature.setOnLongClickListener {
            displayToast("This will download and install all Module from PlayStore in background! The idea is : If you do not need your app to immediately download and install an on demand module, you can defer installation for when the app is in the background. ")
            true
        }

        btn_uninstall_features.setOnLongClickListener {
            displayToast("This will un install all the installed Modules in background(at some point in the future!)")
            true
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        splitManager.registerListener(listener)
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        splitManager.unregisterListener(listener)
        super.onPause()
    }

    private fun requestModule(name: String) {
        Log.d(TAG, "requestModule")
        updateProgressMessage("Loading $name")

        //If we already installed module previously then simply launch that module
        if (splitManager.installedModules.contains(name)) {
            updateProgressMessage("Module $name Already Installed")
            displayToast("Module $name Already Installed")
            launchActivity(name)
            return
        }

        try {
            // Create request to install a feature module by name.
            val request = SplitInstallRequest.newBuilder()
                .addModule(name)
                .build()

            // Load and install the requested feature module.
            splitManager.startInstall(request)
                .addOnSuccessListener {
                    Log.d(TAG, "onSuccess : Session id is $it")
                    displayToast("onSuccess : Session id is $it")
                }
                .addOnFailureListener {
                    Log.d(TAG, "onError : Exception :  $it")
                    displayToast("onError : $it")
                }
        } catch (e: Exception) {
            Log.d(TAG, "Error while requesting Module $e")
        }

        updateProgressMessage("Starting install for $name")

    }

    private fun installImageFeatureDeferred() {
        Log.d(TAG, "installImageFeatureDeferred")
        val modules = listOf(moduleImage, moduleLargeImage)
        splitManager.deferredInstall(modules).addOnSuccessListener {
            Log.d(TAG, "Deferred installation of $modules")
            displayToast("Deferred installation of $modules")
        }
    }

    private fun uninstallAllModules(){
        Log.d(TAG, "uninstallAllModules")
        displayToast("Uninstalling Image_Feature and Image_Large_Feature. This will happen at some point in the future!")
        val installedModules = splitManager.installedModules.toList()
        Log.d(TAG, "Modules to be uninstalled : $installedModules")
        splitManager.deferredUninstall(installedModules).addOnSuccessListener {
            displayToast("Uninstalling $installedModules")
        }
    }

    private fun displayLoadingState(state: SplitInstallSessionState, message: String) {
        displayProgress(true)

        //Show downloading progress
        progressBar.max = state.totalBytesToDownload().toInt()
        progressBar.progress = state.bytesDownloaded().toInt()

        progressText.text = message
    }


    private fun displayProgress(status: Boolean) {
        if (status) {
            progressBar.visibility = View.VISIBLE
            progressText.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
            progressText.visibility = View.INVISIBLE
        }
    }

    private fun updateProgressMessage(message: String) {
        if (progressBar.visibility != View.VISIBLE) {
            displayProgress(true)
        }
        progressText.text = message
    }

    private fun displayToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Launch an activity by its class name.
     */
    private fun launchActivity(name: String) {
        displayProgress(false)
        var className: String? = null
        when (name) {
            moduleImage -> className = imageFeatureClassName
            moduleLargeImage -> className = imageLargeFeatureClassName
        }

        className?.let {
            Intent().setClassName(packageName, className)
                .also {
                    startActivity(it)
                }
        }

    }

    private fun onluLaunchModule(name: String) {
       if (splitManager.installedModules.contains(name)) {
           displayToast("Module $name is Installed!")
           launchActivity(name)
       } else {
           displayToast("Module $name is not Installed!")
       }

    }

}
