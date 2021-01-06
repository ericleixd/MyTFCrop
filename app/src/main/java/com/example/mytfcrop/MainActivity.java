package com.example.mytfcrop;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.mytfcrop.ml.ModelExportIcnTfliteCloseOpen2020122401062120210106t072429284937zModel;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.mytfcrop.ml.Model2;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private Bitmap bitmap;
    ImageView imageView;
    Button beautifyGallery;
    Button beautifyCapture;
    TextView classiness;
    TextView outputText;
    TextView output;
    private static ModelExportIcnTfliteCloseOpen2020122401062120210106t072429284937zModel model; // 声明 TFLite 模型
    private static final int pic_gallery = 2;//数字不重要
    private static final int pic_capture = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView=(ImageView)findViewById(R.id.image);
        beautifyGallery =(Button)findViewById(R.id.gallery);
        beautifyCapture =(Button)findViewById(R.id.capture);
        classiness =(TextView)findViewById(R.id.classifytext);
        outputText =(TextView)findViewById(R.id.output_text);
        output =(TextView)findViewById(R.id.output);
        try {
            model = ModelExportIcnTfliteCloseOpen2020122401062120210106t072429284937zModel.newInstance(MainActivity.this); // 预先加载模型
        } catch (IOException e) {
            System.out.println("Load Model Error");
            e.printStackTrace();
        }
        beautifyGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent();
                intent.setType("image/*");// 加载相片
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),pic_gallery);
            }
        });
        beautifyCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent camera_intent
                        = new Intent(MediaStore
                        .ACTION_IMAGE_CAPTURE);
                startActivityForResult(camera_intent, pic_capture);
            }
        });
    }
    public HashMap<String,String> MyNet(Bitmap bitmap){
        HashMap<String,String> result = new HashMap<String,String>(); // 可以理解为java的dictionary 用来存放模型输出结果
        TensorImage image = TensorImage.fromBitmap(bitmap);
        // Runs model inference and gets result.调用自己的模型
        ModelExportIcnTfliteCloseOpen2020122401062120210106t072429284937zModel.Outputs outputs = model.process(image);
        List<Category> scores = outputs.getScoresAsCategoryList();
        double temp = 0;
        String predict = null;
        for( int i = 0 ; i < scores.size() ; i++) {
            if (temp>scores.get(i).getScore()){
            }else{
                temp=scores.get(i).getScore();
                predict = scores.get(i).getLabel();
            }
        }
        System.out.println(scores);
        Log.e("Predict", predict);
        Log.e("Score", String.valueOf(temp));
        result.put("predict", predict);
        result.put("score", String.valueOf(temp));
        return result;
    }

    public Bitmap setBitmap(Bitmap bitmap) {
        Bitmap bitmap2 = null;
        // 调用Google的API
        FaceDetector faceDetector = new
                FaceDetector.Builder(getApplicationContext()).setTrackingEnabled(false)
                .build();
        if(!faceDetector.isOperational()){
            showMessage(getApplication(),"Could not set up the face detector!",1);
            return bitmap;
        }
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        if (faces.size() > 0 ){
            Face thisFace = faces.valueAt(0);// 返回第一张脸
            // 返回该脸左下角的坐标
            float x1 = thisFace.getPosition().x;
            float y1 = thisFace.getPosition().y;
            // 返回该脸的长宽
            float width1 = thisFace.getWidth();
            float height1 = thisFace.getHeight();
// data type 转换： float -> int
// 是 public static Bitmap createBitmap(Bitmap source, int x, int y, int width, int height)
// 的参数是int
            int x = (int)x1;
            int y = (int)y1;
            int width = (int)width1;
            int height = (int)height1;
// 按照face detection 来生成新的Bitmap图像
            bitmap2 = Bitmap.createBitmap(bitmap,x,y,width,height);
        }else {
            showMessage(getApplication(),"NO face detector!",1);
            return bitmap;
        }
        return bitmap2;
    }
    private void showMessage(Context context, String Message, int isLong){
        Toast.makeText(context,Message,isLong).show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            // 根据进来的 requestCode 来判断 data 是刚刚拍还是从gallery选的
            if (requestCode == pic_gallery) {
                assert data != null;
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                    Bitmap myBitmap = setBitmap(bitmap);//利用Google API 得到人像
                    imageView.setImageBitmap(myBitmap);// 把修剪好的人像输出到屏幕上
                    HashMap<String,String> result = MyNet(myBitmap);//利用自己的模型来判断
                    DecimalFormat df = new DecimalFormat("0.00");// 定义小数点后两位的输出
                    String score = String.valueOf(df.format(Double.parseDouble(result.get("score").toString()) * 100));// 得到百分比的分数
                    //输出结果到屏幕上
                    showMessage(getApplication(),"This " + score + "% may be "+result.get("predict").toString(),1);
                    output.setText(result.get("predict").toString());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == pic_capture){
                Bitmap myBitmapCap =(Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
                // 下面的和requestCode == pic_gallery的情况是一样的
                Bitmap myBitmapCC = setBitmap(myBitmapCap);//利用Google API 得到人像
                imageView.setImageBitmap(myBitmapCC);// 把修剪好的人像输出到屏幕上
                HashMap<String,String> result = MyNet(myBitmapCC);//利用自己的模型来判断
                DecimalFormat df = new DecimalFormat("0.00");// 定义小数点后两位的输出
                String score = String.valueOf(df.format(Double.parseDouble(result.get("score").toString()) * 100));// 得到百分比的分数
                //输出结果到屏幕上
                showMessage(getApplication(),"This " + score + "% may be "+result.get("predict").toString(),1);
                output.setText(result.get("predict").toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}