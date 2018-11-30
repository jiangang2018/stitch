package com.example.pinlan.pinlan_stitch;

import android.os.Bundle;

import com.example.stitch.OnBooleanListener;
import com.example.stitch.StitchActivity;
import com.donkingliang.imageselector.utils.ImageSelector;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.zxy.tiny.Tiny;
import com.zxy.tiny.callback.BitmapBatchCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends StitchActivity {

    private final int CLICK_PHOTO = 1;
    private final int SELECT_PHOTO_Mul = 2;
    private Uri fileUri;
    private ImageView ivImage;
    private Bitmap stitchResult;
    private List<Bitmap> gridImage = new ArrayList<>();
    private List<Bitmap> stitchImage = new ArrayList<>();
    private static final String FILE_LOCATION = Environment.getExternalStorageDirectory() + "/DCIM/Camera/";
    static int REQUEST_READ_EXTERNAL_STORAGE = 11;
    static boolean read_external_storage_granted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivImage = (ImageView) findViewById(R.id.ivImage);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i("permission", "request READ_EXTERNAL_STORAGE");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            Log.i("permission", "READ_EXTERNAL_STORAGE already granted");
            read_external_storage_granted = true;
        }

        Button bClickImage, bPhotoImage, bDone, bResult;

        bClickImage = (Button) findViewById(R.id.bClickImage);
        bPhotoImage = (Button) findViewById(R.id.bPhotoImage);
        bDone = (Button) findViewById(R.id.bDone);
        bResult = (Button) findViewById(R.id.bResult);

        bClickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPermissionRequests(Manifest.permission.CAMERA, new OnBooleanListener() {
                    @Override
                    public void onClick(boolean bln) {
                        if (bln) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            File imagesFolder = new File(FILE_LOCATION);
                            if (!imagesFolder.exists()) {                //如果不存在，那就建立这个文件夹
                                imagesFolder.mkdirs();
                            }
                            String fileName = System.currentTimeMillis() + ".jpg";
                            File image = new File(imagesFolder, fileName);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                fileUri = FileProvider.getUriForFile(MainActivity.this,
                                        "com.example.pinlan.pinlan_stitch.fileprovider", image);
                            } else {
                                fileUri = Uri.fromFile(image);
                            }
                            Log.d("MainActivity", "File URI = " + fileUri.toString());

                            intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//设置Action为拍照
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

                            startActivityForResult(intent, CLICK_PHOTO);
                        } else {
                            Toast.makeText(MainActivity.this, "拍照或无法正常使用", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        bPhotoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageSelector.builder()
                        .useCamera(false) // 设置是否使用拍照
                        .setSingle(false)  //设置是否单选
                        .setViewImage(true) //是否点击放大图片查看,，默认为true
                        .setMaxSelectCount(0) // 图片的最大选择数量，小于等于0时，不限数量。
                        .start(MainActivity.this, SELECT_PHOTO_Mul); // 打开相册
            }
        });

        bDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap[] bitmaps = stitchImage.toArray(new Bitmap[stitchImage.size()]);
                if (bitmaps.length == 0) {
                    Toast.makeText(getApplicationContext(), "No images clicked", Toast.LENGTH_SHORT).show();
                } else if (bitmaps.length == 1) {
                    Toast.makeText(getApplicationContext(), "Only one image clicked", Toast.LENGTH_SHORT).show();
                    ivImage.setImageBitmap(bitmaps[0]);

                } else {
                    Tiny.BitmapCompressOptions options = new Tiny.BitmapCompressOptions();
                    Tiny.getInstance().source(bitmaps).batchAsBitmap().withOptions(options).batchCompress(new BitmapBatchCallback() {
                        @Override
                        public void callback(boolean isSuccess, Bitmap[] bitmaps, Throwable t) {
                            if (!isSuccess) {
                                Toast.makeText(getApplicationContext(), "bitmaps compress bitmap failed!", Toast.LENGTH_SHORT).show();
                            }
                            stitchimages(bitmaps);
                            stitchImage.clear();
                        }
                    });
                }

            }
        });

        bResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stitchResult = stitchresult();
                if (stitchResult != null) {
                    ivImage.setImageBitmap(stitchResult);
                }
                else{
                    Toast.makeText(getApplicationContext(), "No stitch image result", Toast.LENGTH_SHORT).show();
                }
                gridImage.clear();
            }
        });
    }

    //创建ImageAdapter
    public class ImageAdapter extends BaseAdapter {
        private Context mContext;  //获取上下文

        public ImageAdapter(Context c) {
            mContext = c;
        }

        @Override
        public int getCount() {
            return gridImage.size();//图片数组的长度
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {        //判断传过来的值是否为空
                imageView = new ImageView(mContext);  //创建ImageView组件
                imageView.setLayoutParams(new GridView.LayoutParams(300, 300));   //为组件设置宽高
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);        //选择图片铺设方式

            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageBitmap(gridImage.get(position));    //将获取图片放到ImageView组件中
            return imageView; //返回ImageView
        }
    }

    public Bitmap scaleMatrixImage(Bitmap oldbitmap, float scaleWidth, float scaleHeight) {
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);// 放大缩小比例
        Bitmap ScaleBitmap = Bitmap.createBitmap(oldbitmap, 0, 0, oldbitmap.getWidth(), oldbitmap.getHeight(), matrix, true);
        return ScaleBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        Log.d("MainActivity", "request code " + requestCode + ", click photo " + CLICK_PHOTO + ", result code " + resultCode + ", result ok " + RESULT_OK);

        switch (requestCode) {
            case CLICK_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        scanIntent.setData(fileUri);
                        sendBroadcast(scanIntent);
                        Log.d("MainActivity", fileUri.toString());
                        final InputStream imageStream = getContentResolver().openInputStream(fileUri);
                        final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                        stitchImage.add(selectedImage);
                        Bitmap tempImg = scaleMatrixImage(selectedImage, 0.1f, 0.1f);
                        gridImage.add(tempImg);
                        GridView gridView = (GridView) findViewById(R.id.gridView);
                        gridView.setAdapter(new MainActivity.ImageAdapter(this));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case SELECT_PHOTO_Mul:
                if (resultCode == RESULT_OK && imageReturnedIntent != null) {
                    ArrayList<String> images = imageReturnedIntent.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                    images.size();
                    String[] images_file = images.toArray(new String[images.size()]);
                    for (int num = 0; num < images.size(); num = num + 1) {
                        Bitmap bm = BitmapFactory.decodeFile(images_file[num]);
                        stitchImage.add(bm);
                        Bitmap tempImg = scaleMatrixImage(bm, 0.1f, 0.1f);
                        gridImage.add(tempImg);
                    }
                    GridView gridView = (GridView) findViewById(R.id.gridView);
                    gridView.setAdapter(new MainActivity.ImageAdapter(this));

                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i("permission", "READ_EXTERNAL_STORAGE granted");
                read_external_storage_granted = true;
            } else {
                // permission denied
                Log.i("permission", "READ_EXTERNAL_STORAGE denied");
            }
        }
    }


    @Override
    protected void onResume() {
        onPermissionRequests(Manifest.permission.WRITE_EXTERNAL_STORAGE, new OnBooleanListener() {
            @Override
            public void onClick(boolean bln) {
                if (bln) {

                } else {
                    Toast.makeText(MainActivity.this, "文件读写或无法正常使用", Toast.LENGTH_SHORT).show();
                }
            }
        });
        super.onResume();
    }

}
