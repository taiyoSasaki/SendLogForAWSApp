package jp.co.ods.sendlogforawsapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobile.client.AWSMobileClient
import com.amplifyframework.core.Amplify
import jp.co.ods.sendlogforawsapp.databinding.ActivityUploadBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RequiresApi(Build.VERSION_CODES.R)
class UploadActivity :AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityUploadBinding
    private lateinit var mobileClient: AWSMobileClient

    private lateinit var dialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobileClient = AWSMobileClient.getInstance()

        //「全てのファイルへのアクセス」許可の確認
        checkStoragePermission()

        binding.uploadLogButton.setOnClickListener {
            // アップロード中のダイアログ
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.upload_progress_dialog, null)

            val builder = AlertDialog.Builder(this)
            builder.setView(view)
            builder.setCancelable(false)

            dialog = builder.create()
            dialog.show()

            progressBar = dialog.findViewById<ProgressBar>(R.id.progressBarHorizontal)!!
            progressText = dialog.findViewById<TextView>(R.id.textViewProgress)!!

            archive(File("/storage/emulated/0/debuglogger/mobilelog"), File("/storage/emulated/0/debuglogger/mobilelog.zip"))
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun archive(source: File, target: File) {
        val job = GlobalScope.launch (Dispatchers.IO) {
            ZipOutputStream(target.outputStream()).use { zos ->
                if (source.isDirectory.not()) {
                    //ファイルならばそのままルートにデータを流し込む
                    zos.putNextEntry(ZipEntry(source.name))
                    source.inputStream().copyTo(zos, 256)
                    GlobalScope.launch(Dispatchers.Main) {
                        progressBar.progress += 20
                    }
                } else {
                    // ディレクトリの場合
                    var count = true
                    source.walk()
                        .filterNot { it.isHidden } // 隠しファイルは除外
                        .forEach {file->
                            if (file.isDirectory) {
                                // ディレクトリだった場合、Zipの中にディレクトリを切る
                                zos.putNextEntry(ZipEntry("${file.relativeTo(source)}/"))
                                zos.closeEntry()

                                if (count) {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        progressBar.progress += 1
                                    }
                                    count = false
                                } else {
                                    count = true
                                }

                            } else {
                                // ファイルだった場合、データを流し込む
                                zos.putNextEntry(ZipEntry(file.relativeTo(source).toString()))
                                file.inputStream().copyTo(zos, 256)

                                if (count) {
                                    GlobalScope.launch(Dispatchers.Main) {
                                        progressBar.progress += 1
                                    }
                                    count = false
                                } else {
                                    count = true
                                }
                            }
                        }
                }
            }
        }

        job.invokeOnCompletion {
            if (it == null) { // 処理が成功した場合
                Log.d("zipJob", "zip化が完了")

                // プログレスバーの進行状況を変更
                progressBar.progress += 5
                progressText.text = getString(R.string.upload_dialog)

                upload(File("/storage/emulated/0/debuglogger/mobilelog.zip"))
            } else { // 処理にエラー
                Log.d("zipJob", "何らかのエラーが発生")
            }
        }

    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun upload(file: File) {
        GlobalScope.launch (Dispatchers.IO) {
            Amplify.Storage.uploadFile(file.name, file,
                { Log.i("upload", "Successfully uploaded: ${it.key}")
                    Log.d("uploadJob", "アップロードが完了")
                    // プログレスバーの進行状況を変更
                    progressBar.progress = 100
                    // ダイアログを閉じる
                    dialog.dismiss()

                    createDialog("データを送信が完了しました。")
                },
                {
                    Log.e("upload", "Upload failed", it)
                    // ダイアログを閉じる
                    dialog.dismiss()
                    createDialog("データを送信できませんでした。\nもう一度お試しください")
                }
            )
        }
    }

    //「すべてのファイルへのアクセス」許可の確認
    private fun checkStoragePermission() {
        if (Environment.isExternalStorageManager()) {
            Log.d("checkPermission", "")
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            startActivity(intent)
        }
    }

    private fun createDialog(message: String?) {
        android.app.AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

}