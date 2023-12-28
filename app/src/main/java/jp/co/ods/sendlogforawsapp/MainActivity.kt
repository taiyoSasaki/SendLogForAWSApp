package jp.co.ods.sendlogforawsapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import jp.co.ods.sendlogforawsapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.Exception

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mobileClient: AWSMobileClient

    private var mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    override fun onResume() {
        super.onResume()

        // インターネットに接続されているかを確認する
        checkNetworkStatus()
    }

    private fun checkNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))) {
            //インターネットに接続されている
            mobileClient = AWSMobileClient.getInstance()
            val mobileClientLatch = CountDownLatch(1)
            mobileClient.initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) {
                    mobileClientLatch.countDown()
                }

                override fun onError(e: Exception?) {
                    Log.e("onError", "Initialization error.", e)
                }
            })

            try {
                if (!mobileClientLatch.await(2000L, TimeUnit.MILLISECONDS)) throw Exception("Failed to initialize mobile client.")
            } catch (exception: Exception) {
                Log.d("exception", "${exception.message}")
            }

            login("taiyo.sasaki@ods.co.jp", "0601Shine")

        } else { //インーネットに接続されていない
            createDialog("インターネットに接続してください")
        }


    }

    private fun login(username: String, password: String) {
        mobileClient.signIn(username, password, null, object : Callback<SignInResult> {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onResult(result: SignInResult?) {
                Log.d("Login", "initialize onResult: ${result?.signInState}")
                when (result?.signInState) {
                    SignInState.DONE -> {
                        val intent = Intent(this@MainActivity, UploadActivity::class.java)
                        startActivity(intent)
                    }
                    else -> {
                        Log.e("Login", "initialize onResult: ${result?.signInState}")
                        mHandler.post { createDialog("${result?.signInState}") }
                    }
                }
            }

            override fun onError(e: Exception?) {
                e?.printStackTrace()
                Log.e("onError", "signIn onError: ${e?.message}")
                launch(Dispatchers.Main) {
                    mHandler.post { createDialog("${e?.message}") }
                }
            }
        })
    }

    private fun createDialog(message: String?) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .show()
    }

}