package jp.co.ods.sendlogforawsapp

import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignUpResult
import jp.co.ods.sendlogforawsapp.databinding.ActivityVerificationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.Exception

class VerificationActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityVerificationBinding
    private lateinit var mobileClient: AWSMobileClient
    var username :String? = null

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobileClient = AWSMobileClient.getInstance()

        username = intent.getStringExtra("username")

        binding.submitButton.setOnClickListener {
            val code = binding.confirmationCode.text.toString()
            confirm(code)
        }
    }

    private fun confirm(code :String) {
        mobileClient.confirmSignUp(username, code, object : Callback<SignUpResult> {
            override fun onResult(result: SignUpResult?) {
                Log.d("confirm", "signUp onResult: ${result?.confirmationState}")
                result?.confirmationState?.let {
                    if (it) {
                        val intent = Intent(this@VerificationActivity, MainActivity::class.java)
                        startActivity(intent)
                    }
                }
            }

            override fun onError(e: Exception?) {
                Log.e("confirm", "sigUp onError: ${e?.message}")
                launch(Dispatchers.Main) {
                    mHandler.post { createDialog("${e?.message}") }
                }
            }
        })
    }

    private fun createDialog(message: String?) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setNeutralButton("OK", null)
            .show()
    }

}