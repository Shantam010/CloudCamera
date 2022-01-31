package com.project.cloudcamera;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final Integer CAMIMAGE=123;
    FirebaseDatabase db;
    DatabaseReference myRef;
    Uri filePath;
    String mCurrentPhotoPath;
    private StorageReference mStorageRef;

    Button btnCamera;
    Button btnUpload;
    CircleImageView imageLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        initialize();
        setListeners();

    }

    public void initialize()
    {
        btnCamera=(Button) findViewById(R.id.btnCamera);
        btnUpload=(Button) findViewById(R.id.btnUpload);
        imageLayout=(CircleImageView) findViewById(R.id.imageLayout);
    }

    public void setListeners()
    {
        btnCamera.setOnClickListener(this);
        btnUpload.setOnClickListener(this);
    }
    private boolean hasCamera()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String storageDir = Environment.getExternalStorageDirectory() + "/picupload";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e("photo path = " , mCurrentPhotoPath);

        return image;
    }

    private void uploadToStorage()
    {
        if(filePath != null) {

            final ProgressDialog progressdialog=new ProgressDialog(this);
            progressdialog.setTitle("Uploading.....");
            progressdialog.show();

            final String _imgName=System.currentTimeMillis()/1000+"_img.jpg";

            StorageReference riversRef = mStorageRef.child("userImages/"+_imgName);

            Bitmap bitmap=null;
            try{
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), filePath);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            //compress image

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos);
            byte[] data = baos.toByteArray();




            riversRef.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            progressdialog.dismiss();
                            Toast.makeText(getApplicationContext(),"Successfully uploaded"
                                    ,Toast.LENGTH_SHORT).show();
                            //imageView.setImageBitmap(null);
                            Log.e("upload : ","Success");
                            @SuppressWarnings("VisibleForTests")Uri downloadUrl =
                                    taskSnapshot.getDownloadUrl();
                            Log.e("downloadUrl-->", "" + downloadUrl);




                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            progressdialog.dismiss();
                            Toast.makeText(getApplicationContext(),exception.getMessage()
                                    ,Toast.LENGTH_SHORT).show();

                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {


                            @SuppressWarnings("VisibleForTests")double progress
                                    = (100.0 * taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();

                            progressdialog.setMessage(((int) progress) + "% uploading....");

                        }
                    }) ;


        }



    }

    @Override
    public void onClick(View v) {

        switch(v.getId())
        {
            case R.id.btnCamera:
                if (hasCamera()) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File

                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                            filePath = Uri.fromFile(photoFile);
                            Log.e("Uri ", filePath + "");
                            startActivityForResult(takePictureIntent, CAMIMAGE);
                        }
                    }

                }
                break;

            case R.id.btnUpload:
                uploadToStorage();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAMIMAGE && resultCode == RESULT_OK)
        {
            CropImage.activity(filePath).setAspectRatio(1,1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri resultUri = result.getUri();
            imageLayout.setImageURI(resultUri);
        }

    }
}