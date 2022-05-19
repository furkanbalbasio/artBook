package com.balbasio.artbook

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.balbasio.artbook.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {
    private lateinit var binding:ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher:ActivityResultLauncher<String>
    private lateinit var database : SQLiteDatabase
    var selectedBitmap: Bitmap?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityArtBinding.inflate(layoutInflater)
        val view=binding.root
        setContentView(view)
        database=this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
registerLauncher()
        val info=intent.getStringExtra("info")
if (info.equals("new")){
    binding.editTextTextPersonName.setText("")
    binding.editTextTextPersonName2.setText("")
    binding.editTextTextPersonName3.setText("")
    binding.button.visibility=View.VISIBLE
    val selectedImageBackground=BitmapFactory.decodeResource(applicationContext.resources,R.drawable.ic_launcher_background)
    binding.imageView2.setImageBitmap(selectedImageBackground)
}
        else {
            binding.button.visibility=View.INVISIBLE
    val selectedId=intent.getIntExtra("id",1)
    val cursor=database.rawQuery("SELECT * FROM arts WHERE id=?", arrayOf(selectedId.toString()))
    val artNameIx=cursor.getColumnIndex("artname")
    val artistNameIx=cursor.getColumnIndex("artistname")
    val imageIx=cursor.getColumnIndex("image")
    while (cursor.moveToNext()){
        binding.editTextTextPersonName.setText(cursor.getString(artNameIx))
        binding.editTextTextPersonName2.setText(cursor.getString(artistNameIx))
        binding.editTextTextPersonName3.setText(cursor.getString(imageIx))
        val byteArray=cursor.getBlob(imageIx)
val bitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
        binding.imageView2.setImageBitmap(bitmap)
    }
    cursor.close()
        }
        fun makeSmallerBitmap(image: Bitmap,maximumSize:Int):Bitmap{
            var width=image.width
            var height=image.height
            val bitMapRatio:Double=width.toDouble()/height.toDouble()
            if (bitMapRatio>1){
                width=maximumSize
                val scaledHeight=width/bitMapRatio
                height=scaledHeight.toInt()
            }
            else{
                height=maximumSize
                val scaledWidth=height/bitMapRatio
                width=scaledWidth.toInt()
            }
            return Bitmap.createScaledBitmap(image,width,height,true)
        }


        fun button(view:android.view.View) {
            val artName=binding.editTextTextPersonName.text.toString()
            val artistName=binding.editTextTextPersonName2.text.toString()
            val year=binding.editTextTextPersonName3.text.toString()
            if (selectedBitmap!=null){
            val smallBitmap= makeSmallerBitmap(selectedBitmap!!,300)
                val outputStream=ByteArrayOutputStream()
                smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
                val byteArray=outputStream.toByteArray()
                try {
                    database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artisname VARCHAR,year VARCHAR,image BLOB)")
                    val sqlString="INSERT INTO arts (artname,artistname,year,image) VALUES (?,?,?,?)"
                    val statement=database.compileStatement(sqlString)
                    statement.bindString(1,artName)
                    statement.bindString(2,artistName)
                    statement.bindString(3,year)
                    statement.bindBlob(4,byteArray)
                    statement.execute()
                }
                catch (e:Exception){
                    e.printStackTrace()
                }
                val intent=Intent(this,MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }

        }
        fun selectImage(view:android.view.View){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for Gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                    {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
            }
                else{
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            }
        else{
            val intenToGallery=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intenToGallery)
        }}

    }
    private fun registerLauncher(){
        activityResultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult())
             {result ->
            if (result.resultCode== RESULT_OK){
                val intentFromResult=result.data
                if (intentFromResult != null){
                    val imageData=intentFromResult.data
                   // binding.imageView2.setImageURI(imageData)
                    try {
                        if (Build.VERSION.SDK_INT>= 28){
                    val source=ImageDecoder.createSource(this@ArtActivity.contentResolver, imageData!!)
                            selectedBitmap=ImageDecoder.decodeBitmap(source)
                            binding.imageView2.setImageBitmap(selectedBitmap)
                    }
                    else {
                    selectedBitmap=MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                            binding.imageView2.setImageBitmap(selectedBitmap)

                        }
                    }
                    catch (e:Exception){
                        e.printStackTrace()

                    }
                }
            }
            }
        permissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
            result-> if (result) {
            val intentToGallery =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
            else{
                Toast.makeText(this@ArtActivity,"Permission needed",Toast.LENGTH_LONG).show()
            }
        }
    }
}