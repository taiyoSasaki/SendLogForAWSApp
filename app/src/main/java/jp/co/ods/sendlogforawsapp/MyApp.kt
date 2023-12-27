package jp.co.ods.sendlogforawsapp

import android.app.Application
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.AWSS3StoragePlugin

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Add this line, to include the Auth plugin.
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())
            Amplify.configure(applicationContext)
            Log.i("MyApp", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("MyApp", "Could not initialize Amplify", error)
        }
    }
}