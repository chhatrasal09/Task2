package com.app.chhatrasal.task2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.ContactsContract
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Button
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var contactList: ArrayList<ContactInfo>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val createButton: Button = findViewById(R.id.create_csv_button)
        createButton.setOnClickListener { getPermissions() }
        contactList = ArrayList()
    }

    private fun getPermissions() {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
        } else {
            Snackbar.make(findViewById(R.id.root_layout), "Permission already granted.", Toast.LENGTH_LONG).setDuration(3000).show()
            createCSVAndZip()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCSVAndZip()
        } else {
            quitApp()
        }
    }

    private fun start(){
        createCSV()
        val file = writeCSVtoStorage()
        createZipOfCSV(file)
    }
    private fun createCSVAndZip() {
        val createCSV = Observable.create(object : ObservableOnSubscribe<String>{
            override fun subscribe(e: ObservableEmitter<String>) {
                try{
                    start()
                }catch (exception : Exception){
                    e.onError(exception)
                }
                e.onComplete()
            }
        })

        createCSV
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Observer<String>  {
                    override fun onComplete() {
                        Log.d("######","File stored in /Task2 directory.")
                        Snackbar.make(findViewById(R.id.root_layout),"File stored in /Task2 directory.",Toast.LENGTH_LONG).setDuration(3000).show()
                    }

                    override fun onSubscribe(d: Disposable) {
                        Log.d("####","$d")
                    }

                    override fun onNext(t: String) {
                        Log.d("#####", " $t")
                    }

                    override fun onError(e: Throwable) {
                        Log.e("#####","${e.printStackTrace()}")
                    }
                })
    }


    private fun createCSV() {
        val cursor: Cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val id: String = cursor
                        .getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val displayName: String = cursor
                        .getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    val phoneCursor: Cursor = contentResolver
                            .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(id), null)
                    while (phoneCursor.moveToNext()) {
                        val phoneNumber: String = phoneCursor
                                .getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        Log.d("######   Name", "" + displayName)
                        Log.d("######   Number", "" + phoneNumber)
                        contactList.add(ContactInfo(displayName, id, phoneNumber))
                    }
                    phoneCursor.close()
                }
            }

        }
        if (cursor != null) {
            cursor.close()
        }
    }

    private fun writeCSVtoStorage(): File{
        var file : File? = null
        if (contactList.size > 0) {
            val stringBuilder = StringBuilder()
            var index = 0
            while (index < contactList.size) {
                stringBuilder.append(contactList.get(index)._id)
                stringBuilder.append(",")
                stringBuilder.append(contactList.get(index).name)
                stringBuilder.append(",")
                stringBuilder.appendln(contactList.get(index).number)
                index++;
            }

            Log.v("#####", "" + stringBuilder.toString())

            val fileDirectory = File(Environment.getExternalStorageDirectory(), "Task2/")
            if (!fileDirectory.exists()) {
                fileDirectory.mkdir()
            }
            file = File(fileDirectory.path, "/test.csv")
            file.writeText(stringBuilder.substring(0, stringBuilder.length - 1))
        }
        return file!!
    }

    private fun createZipOfCSV(file : File){
        Log.d("######","" + file.parentFile.path)
        val zipFile = ZipOutputStream(BufferedOutputStream(FileOutputStream(file.parentFile.path + "/test.zip")))
        val origin = BufferedInputStream(FileInputStream(file))
        val entry = ZipEntry(file.path.substring(file.path.lastIndexOf('/')))
        zipFile.putNextEntry(entry)
        origin.copyTo(zipFile, 1024)
        zipFile.closeEntry()
        zipFile.close()
    }
    private fun quitApp() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    data class ContactInfo(val name: String,
                           val _id: String,
                           val number: String)
}
