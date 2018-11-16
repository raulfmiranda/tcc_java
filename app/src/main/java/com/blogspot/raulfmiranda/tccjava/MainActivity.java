package com.blogspot.raulfmiranda.tccjava;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.File;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ColorMatrix;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity {

    private int CAMERA_PERMISSION_CODE = 100;
    private int CAMERA_REQUEST = 1888;
    private int maxWidthHeight = 590;
    private File arquivoFoto;
    private ProgressBar progressBar;
    private Button btnUploadPicture;
    private ImageView imgFoto;
    private Button btnApplyFilter;
    private Button btnTakePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        btnUploadPicture = findViewById(R.id.btnUploadPicture);
        imgFoto = findViewById(R.id.imgFoto);
        btnApplyFilter = findViewById(R.id.btnApplyFilter);
        btnTakePicture = findViewById(R.id.btnTakePicture);

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, CAMERA_PERMISSION_CODE);

                } else {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    arquivoFoto = new File(geraCaminhoFoto());
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(arquivoFoto));
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        btnUploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFoto();
            }
        });

        btnApplyFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ApplyFilterAsync().execute();
            }
        });
    }

    private void uploadFoto() {
        if(arquivoFoto != null) {
            progressBar.setVisibility(View.VISIBLE);
            StorageReference storageReference = FirebaseStorage.getInstance().getReference().child(arquivoFoto.getName());
            UploadTask uploadTask = storageReference.putFile(Uri.fromFile(arquivoFoto));
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("firebaseerror", e.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    btnUploadPicture.setText("Picture sent");
                    progressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    private String geraCaminhoFoto() {
        return getExternalFilesDir(null).getPath() + "/" + System.currentTimeMillis() + ".jpg";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == CAMERA_PERMISSION_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                arquivoFoto = new File(geraCaminhoFoto());
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(arquivoFoto));
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(MainActivity.this, "Permissão da Câmera Consulta Negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            setVisibleAll();
            setBitmap();
        }
    }

    private void setBitmap() {
        if(arquivoFoto != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath());
            if(bitmap != null) {
                Bitmap bitmapConsultaReduzido = resizeBitmap(bitmap, maxWidthHeight, maxWidthHeight);
                imgFoto.setImageBitmap(bitmapConsultaReduzido);
                imgFoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imgFoto.setTag(arquivoFoto.getAbsolutePath());
            }
        }
    }

    private Bitmap applyFilter() {
        if(arquivoFoto != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(arquivoFoto.getAbsolutePath());
            if(bitmap != null) {
                Bitmap bmp = resizeBitmap(bitmap, maxWidthHeight, maxWidthHeight);
                return doGrayScale(bmp);
            }
        }
        return null;
    }

    class ApplyFilterAsync extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            return applyFilter();
        }

        @Override
        protected void onPostExecute(Bitmap bitmapGray) {
            super.onPostExecute(bitmapGray);
            imgFoto.setImageBitmap(bitmapGray);
            imgFoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imgFoto.setTag(arquivoFoto.getAbsolutePath());
            progressBar.setVisibility(View.GONE);
            bitmapToFile(bitmapGray);
        }
    }

    private Bitmap doGrayScale(Bitmap bmpOriginal) {
        int width;
        int height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0f);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixFilter);
        canvas.drawBitmap(bmpOriginal, 0f, 0f, paint);
        return bmpGrayscale;
    }

    private void bitmapToFile(Bitmap bmp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in file
        try {
            FileOutputStream fos = new FileOutputStream(arquivoFoto);
            fos.write(bitmapdata);
            fos.flush();
            fos.close() ;
        } catch (IOException e) {
            Log.e("fileerror", e.getMessage());
        }
    }

    private Bitmap resizeBitmap(Bitmap img, int maxWidth, int maxHeight) {

        // Verticalizar a foto
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        img = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);

        if (maxHeight > 0 && maxWidth > 0) {
            int width = img.getWidth();
            int height = img.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = Math.round((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = Math.round((float)maxWidth / ratioBitmap);
            }
            img = Bitmap.createScaledBitmap(img, finalWidth, finalHeight, true);
            return img;
        }
        return img;
    }

    private void setVisibleAll() {
        btnApplyFilter.setVisibility(View.VISIBLE);
        btnUploadPicture.setVisibility(View.VISIBLE);
        imgFoto.setVisibility(View.VISIBLE);
    }
}
