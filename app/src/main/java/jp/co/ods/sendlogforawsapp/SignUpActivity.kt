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
import jp.co.ods.sendlogforawsapp.databinding.ActivitySignUpBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding : ActivitySignUpBinding
    private lateinit var mobileClient: AWSMobileClient

    private var mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobileClient = AWSMobileClient.getInstance()

        binding.signUpButton.setOnClickListener {
            val username = binding.userIdEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val email = binding.emailEditText.text.toString()

            Log.d("SignUp", "username : $username, password : $password, email : $email")

            signUp(username, password, email)
        }

    }

    private fun signUp(username: String, password: String, email: String) {
        val userAttributes = mapOf("email" to email)

        mobileClient.signUp(username, password, userAttributes, null, object : Callback<SignUpResult> {
            override fun onResult(result: SignUpResult?) {
                Log.d("SignUp", "signup onResult : ${result?.confirmationState}")
                result?.confirmationState?.let { confirmed ->
                    if (confirmed) {
                        val intent = Intent(this@SignUpActivity, MainActivity::class.java )
                        startActivity(intent)
                    }
                }

                result?.userCodeDeliveryDetails?.attributeName.let {
                    when(it) {
                        "email" -> {
                            val intent = Intent(this@SignUpActivity, VerificationActivity::class.java)
                                .putExtra("username", username)
                            startActivity(intent)
                        }
                        else -> {
                            Log.e("signup", "signUp onResult: $it")
                            launch(Dispatchers.Main){
                                mHandler.post { createDialog("unknown attribute: $it") }
                            }
                        }
                    }
                }
            }

            override fun onError (e: Exception?) {
                Log.e("onError", "signUp onError: ${e?.message}")
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