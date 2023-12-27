package jp.co.ods.sendlogforawsapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
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

    private val READ_REQUEST_CODE = 100

    private lateinit var binding: ActivityUploadBinding
    private lateinit var mobileClient: AWSMobileClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobileClient = AWSMobileClient.getInstance()

        //「全てのファイルへのアクセス」許可の確認
        checkStoragePermission()

        // 「upload to s3」ボタン
        binding.uploadButton.setOnClickListener {
            selectFile()
        }

        // 「zip folder」ボタン
        binding.zipButton.setOnClickListener {
            zipFolder()
        }


    }

    private fun zipFolder() {
        val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileToZip = File(downloadFolder, "ziptest")

        Log.d("zipFolder", "${fileToZip.path}, ${fileToZip.name}")

        if (fileToZip.exists() && fileToZip.isDirectory) {
            archive(File("/storage/emulated/0/debuglogger/mobilelog"), File("/storage/emulated/0/debuglogger/mobilelog.zip"))
        } else {
            Log.d("zipFolder", "fileToZipが存在しないもしくはフォルダーではありません。")
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
                } else {
                    // ディレクトリの場合
                    source.walk()
                        .filterNot { it.isHidden } // 隠しファイルは除外
                        .forEach {file->
                            if (file.isDirectory) {
                                // ディレクトリだった場合、Zipの中にディレクトリを切る
                                zos.putNextEntry(ZipEntry("${file.relativeTo(source)}/"))
                                zos.closeEntry()
                            } else {
                                // ファイルだった場合、データを流し込む
                                zos.putNextEntry(ZipEntry(file.relativeTo(source).toString()))
                                file.inputStream().copyTo(zos, 256)
                            }
                        }
                }
            }
        }

        job.invokeOnCompletion {
            if (it == null) { // 処理が成功した場合
                Log.d("job", "zip化が完了")
            } else { // 処理にエラー
                Log.d("job", "何らかのエラーが発生")
            }
        }

    }

    private fun selectFile() {
        //ファイルを選ぶ
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                Log.d("selectFile", "${uri.path}")

                val file = getFileFromUri(uri)

                if (file != null) {
                    upload(file)
                } else {
                    Log.d("selectFile", "file is null")
                }
            }
        }
    }

    private fun upload(file: File) {
        Amplify.Storage.uploadFile(file.name, file,
            { Log.i("upload", "Successfully uploaded: ${it.key}") },
            { Log.e("upload", "Upload failed", it) }
        )
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

    // ファイルをcashフォルダーにコピーしてそのFile型変数を返す
    private fun getFileFromUri(uri: Uri) :File? {
        val fileName = getFileNameFromUri(uri)
        Log.d("selectFile", fileName)

        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, fileName) //保存先のファイルパスを指定

        inputStream?.use {
            file.outputStream().use { output ->
                it.copyTo(output)
            }
        }
        return if (file.exists()) file else null
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result = ""
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val  displayName = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    result = it.getString(displayName)
                }
            }
        }
        if(result.isBlank()) {
            result = uri.path ?: ""
            val cut = result.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

}