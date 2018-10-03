package com.example.administrator.uwbbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

import static com.example.administrator.uwbbluetooth.R.id.singledata;
import static com.example.administrator.uwbbluetooth.R.id.text_state;
import static com.example.administrator.uwbbluetooth.R.id.uppicture;
import static java.lang.Boolean.TRUE;

/**
 * 2018年5月18日 23:26 更改情况：
 *   调整了单片机数据发送的频率，一秒发送一次
 *   程序修改了数据接收的方式，蓝牙连接时出现的数据暴增被处理为只在接收状态栏显示，不再写入到数据文件中
 *   减慢了将数据写向文件的速率
 *   写文件工具类中，将格式设置为不换行
 *   PictureThreadtwo线程中，设置了整型变量 len ,用来获取读取数据的长度
 *
 *   预期改动：
 *     在已接收的数据中找出所有的“z”标志位并获取索引，将索引保存数组中，通过得到的索引获取每一组数据
 *     一组数据处理完成后 将长度传回读数据的函数，通过len参数跳过之前已经读取过的数据，直接获取新数据
 *     若无新数据，则还是传回上次的数据
 *
 *
 * 2018年5月19日  更改情况：
 *   调整单片机发送的数据的速率为0.5秒一次
 */

public class MainActivity extends Activity {

     private BluetoothAdapter mBluetoothAdapter;  /* 创建本地蓝牙适配器 */
     private TextView textState;                  /* “连接”界面显示蓝牙连接状态的文本框 */
     private TextView single_text;                /* “连接”界面单行显示数据发送的文本框 */
     private TextView datatext;                   /* “数据”界面显示文件数据的文本框 */
     private ImageButton connectButton;           /* 主界面“连接”按钮 */
     private ImageButton dataButton;              /* 主界面“数据”按钮 */
     private ImageButton stateButton;             /* 主界面“设备状态”按钮 */
     private ImageButton pictureButton;           /* 主界面“图像生成”按钮 */
     private ImageButton positionButton;          /* 主界面“模拟定位”按钮 */
     private Button bt_ConnectionBack;            /* “连接”界面“返回”按钮 */
     private Button bt_DataBack;                  /* “数据”界面“返回”按钮 */
     private Button bt_DeviceStateBack;           /* “设备状态”界面“返回”按钮 */
     private Button bt_PictureBack;               /* “图像生成”界面“返回”按钮 */
     private Button bt_PositionBack;              /* “模拟定位”界面“返回”按钮 */
     private Button bt_CreatFile;                 /* 文件创建界面“创建”按钮 */
     private Button bt_topicture;
     private Button bt_update;
     private Button bt_uppicture;
     private Button bt_connecttodata;
     private Button bt_connecttopicture;
     private ImageButton bt_test;                 /* 图像生成界面的动态按钮 */
     private ListView blueToothList;              /* 蓝牙设备显示列表 */
     private BlueToothDeviceAdapter adapter;      /* 蓝牙连接工具类 */
     private Thread connectThread;                /* 连接蓝牙并接收数据的线程 */
     private EditText mPathName;                  /* 文件创建界面文件夹名输入框 */
     private EditText mFileName;                  /* 文件创建界面文件名输入框 */
     private EditText mContent;                   /* 文件创建界面内容输入框 */
     private Button mcreat;                       /* 文件创建界面完成创建按钮 */
     private TextView x_axis;                     /* x轴作坐标显示文本框 */
     private TextView y_axis;                     /* y轴组坐标显示文本框 */
     private String path;                         /* 新建文件夹名 */
     private String name;                         /* 新建文件名 */
     private String coordinate;
     private int xcoordinate;
     private int ycoordinate;

     private static final String NAME = "UWB";
     private final int BUFFER_SIZE = 1024;  /* 每次读取1024字符长度 */
     private int number1= 16;          /* 定义初始时x单次移动距离 */
     private int number2= 0;          /* 定义初始时y单次移动距离 */
     private int sign = 0;
     private int x;
     private int y;
     private int datanumber;
     private int x_distance;     /* x坐标值 */
     private int y_distance ;    /* y坐标值 */
     private int x_devicelocation;
     private int y_devicelocation;

      /* 蓝牙设备之间配对需要的唯一UUID码 */
     private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
      /* 获得BluetoothAdapter对象 */
     BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
      /* 实例化设置控件坐标的类 */
     WidgetController mWidgetController = new WidgetController();
      /* 实例创建数据文件工具类 */
     WriteFileTool writeFileTool = new WriteFileTool();

    /**
     * 主方法
     * 程序从这里开始执行
     */
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
             /* 取消初始标题栏 */
            requestWindowFeature(Window.FEATURE_NO_TITLE);
             /* 打开布局 */
            setContentView(R.layout.activity_main);
            deviceInit();    /* 初始化主界面控件 */
            openBlueTooth(); /* 检测蓝牙状态，若关闭，则请求打开蓝牙 */
    }

    /**
     * 初始化主界面控件，实例化活动对象
     */
    private void deviceInit() {

         /* 实例化“连接”按钮控件，创建点击事件内部类 */
        connectButton = (ImageButton) findViewById(R.id.bt_connect);
        connectButton.setOnClickListener(new Connect());

         /* 实例化“数据”按钮控件，创建点击事件内部类 */
        dataButton = (ImageButton) findViewById(R.id.bt_data);
        dataButton.setOnClickListener(new Data());

         /* 实例化“设备状态”按钮控件，创建点击事件内部类 */
        stateButton = (ImageButton) findViewById(R.id.bt_device_state);
        stateButton.setOnClickListener(new DeviceState());

         /* 实例化“图像生成”按钮控件，创建点击事件内部类 */
        pictureButton = (ImageButton) findViewById(R.id.bt_picture);
        pictureButton.setOnClickListener(new Picture());

         /* 实例化“模拟定位”按钮控件，创建点击事件内部类 */
        positionButton = (ImageButton) findViewById(R.id.bt_position);
        positionButton.setOnClickListener(new Position());

         /* 实例化“创建”按钮控件，创建点击事件内部类 */
        bt_CreatFile = (Button)findViewById(R.id.bt_theme);
        bt_CreatFile.setOnClickListener(new CreatFile());

         /* 注册广播 */
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(Receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(Receiver, filter);
    }

    /**
     * 打开蓝牙
     */
    private void openBlueTooth() {

         /* 判断设备是否具有蓝牙功能 */
        if (mAdapter == null) {
            /* 提示当前设备不支持蓝牙 */
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
        }

         /* 请求打开了蓝牙功能 */
        if (!mAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
        }

         /* 开启被其他蓝牙设备发现的功能 */
        if (mAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
             /* 蓝牙被发现持续时间设为零，则设置为始终可以被检测到 */
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(intent);
        }
    }

    /**
     * 程序在这里结束
     * 退出前注销广播
     */
    protected void onDestroy() {

        super.onDestroy();
         /* 取消搜索 */
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
         /* 注销BroadcastReceiver，防止资源泄露 */
        unregisterReceiver(Receiver);
    }

    /**
     * 创建广播函数
     * 显示附近搜索到的蓝牙设备
     */
    private final BroadcastReceiver Receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
             /* 判断是否搜索到蓝牙设备 */
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                 /* 获取搜索到的蓝牙设备 */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 /* 显示未配对的蓝牙设备 */
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    adapter.add(device);
                }
            }
        }
    };

    /**
     * 定义标题栏按钮“创建”
     */
    class CreatFile implements OnClickListener{

        public void onClick(View view) {
             /* 打开创建文件的界面 */
            setContentView(R.layout.activity_creatfile);
             /* 实例化创建文件界面中的控件 */
            findid();
             /* “创建”按钮点击事件，创建完成返回主界面 */
            mcreat.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    getcontent();  /* 获取用户输入的内容 */
                    setContentView(R.layout.activity_main);  /* 打开主界面 */
                    deviceInit();  /* 初始化主界面的所有控件 */
                }
            });
        }
    }

    /**
     * 数据格式转换
     */
    public String dataTransform(String coordinate){

        return null;
    }

    /**
     * 定义“连接”按钮响应函数
     */
    class Connect implements OnClickListener {

        public void onClick(View view) {

            openBlueTooth();                            /* 再次检测蓝牙是否打开 */

            setContentView(R.layout.activity_connect);  /* 打开“连接”界面 */

            connectInit();                              /* 初始化控件 */

            displayBlueToothDevice();                   /* 在列表中显示搜索到的蓝牙设备 */

            clickBlueToothList();                       /* 蓝牙点击事件 */
        }

    }


    /**
     * 实例化创建文件界面的控件
     */
    private void findid(){

             /* 实例化文件夹名输入框 */
            mPathName = (EditText)findViewById(R.id.et_pathName);

             /* 实例化文件名输入框 */
            mFileName = (EditText)findViewById(R.id.et_fileName);

             /* 实例化内容输入框 */
            mContent = (EditText)findViewById(R.id.et_content);

             /* 实例化“创建”按钮 */
            mcreat = (Button)findViewById(R.id.btn_test);

        }

    /**
     * 取得文件创建界面用户输入的内容
     */
    private void  getcontent(){

            /* 获取用户输入的文件夹名 */
           String pathName = mPathName.getText().toString();

            /* 获取用户输入的文件名 */
           String fileName = mFileName.getText().toString();

            /* 获取用户打算预先在文件中输入的内容 */
           String content  = mContent.getText().toString();

            /* 用户输入判断 */
           if(TextUtils.isEmpty(pathName)) {
               Toast.makeText(this,"文件夹名不能为空",Toast.LENGTH_SHORT).show();
           } else if(TextUtils.isEmpty(fileName)){
               Toast.makeText(this,"文件名不能为空",Toast.LENGTH_SHORT).show();
           }else{
                /* 将用户输入的内容写入到文件中 */
               PostContent(pathName,fileName,content);
           }
       }

    /**
     * 传递写入的内容
     * return 文件路径
     */
    private String[] PostContent(String pathName,String fileName,String content){

            /* 创建文件路径 */
           String filePath = Environment.getExternalStorageDirectory().getPath()
                   +"/"+pathName+"/";

            /* 创建文件名 */
           String fileName1 = fileName+".txt";

            /* 将字符串写入到文本文件中 */
           writeFileTool.writeTxtToFile(content,filePath,fileName1);
           Toast.makeText(this,"创建成功",Toast.LENGTH_SHORT).show();

            /* 存储文件路径并返回 */
           String address[]  = {filePath,fileName1};
           path = filePath;
           name = fileName1;

           return address;
       }

    /**
     * 各功能界面返回主界面按钮响应函数
     */
    class Back implements OnClickListener {

            public void onClick(View view) {

                 /* 打开主界面 */
                setContentView(R.layout.activity_main);

                 /* 初始化主界面控件 */
                deviceInit();
            }
        }

    /**
     * “数据”按钮点击事件
     */
    class Data implements OnClickListener {

            public void onClick(View view){

                     /* 打开“数据”界面 */
                    setContentView(R.layout.activity_data);

                     /* 实例化返回主界面按钮 */
                    bt_DataBack = (Button) findViewById(R.id.databack);
                    bt_DataBack.setOnClickListener(new Back());
                    bt_topicture = (Button)findViewById(R.id.bt_ToPicture);
                    bt_topicture.setOnClickListener(new Picture());
                    bt_update = (Button)findViewById(R.id.update);
                    bt_update.setOnClickListener(new Update());

                     /* 实例化数据显示的文本框 */
                    datatext= (TextView) findViewById(R.id.datatext);

                     /* 开启从文件中读取数据的线程 */
                    DataThread dataThread = new DataThread();
                    dataThread.start();
            }

        }

    class Update implements OnClickListener{
        public void onClick(View view){
            File file = new File(path+name);
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

             /* 打开“数据”界面 */
            setContentView(R.layout.activity_data);

             /* 实例化返回主界面按钮 */
            bt_DataBack = (Button) findViewById(R.id.databack);
            bt_DataBack.setOnClickListener(new Back());
            bt_topicture = (Button)findViewById(R.id.bt_ToPicture);
            bt_topicture.setOnClickListener(new Picture());
            bt_update = (Button)findViewById(R.id.update);
            bt_update.setOnClickListener(new Update());

             /* 实例化数据显示的文本框 */
            datatext= (TextView) findViewById(R.id.datatext);

             /* 开启从文件中读取数据的线程 */
            DataThread dataThread = new DataThread();
            dataThread.start();
        }
    }

    /**
     * 读取数据线程
     */
    private class DataThread  extends Thread {

      private DataThread() {}

      public void run () {

          try {
                /* 创建流文件读入类 */
               FileInputStream fin = new FileInputStream(path+name);

                /* 通过available方法取得流的最大字节数 */
               byte[] buffer = new byte[fin.available()];

               while (true) {
                    /* 获取接收到的数据长度 */
                   int len = fin.read(buffer);

                   if(len>0) {
                       final byte[] data = new byte[len];

                        /* 将数据转成字符串 */
                       System.arraycopy(buffer, 0, data, 0, len);
                       datatext.post(new Runnable() {
                              @Override
                              public void run() {

                            /* 将数据传递给数据显示文本框 */
                           datatext.setText(new String(data));

                            /* 设置文本框滚动显示 */
                           datatext.setMovementMethod(ScrollingMovementMethod.getInstance());
                              }
                          });
                      }
                  }
          }catch(Exception e){
                  e.printStackTrace();
          }
      }
    }

    /**
     * “设备状态”按钮点击事件
     */
     class DeviceState implements OnClickListener {

        public void onClick(View view) {

             /* 打开设备状态界面 */
            setContentView(R.layout.activity_devicestate);

             /* 实例化“设备状态”界面返回按钮 */
            bt_DeviceStateBack = (Button) findViewById(R.id.devicestateback);
            bt_DeviceStateBack.setOnClickListener(new Back());

        }
     }


     /**
      * "图像生成"按钮点击事件
      */
    class Picture implements OnClickListener {
        public void onClick(View view) {

             /* 打开图像生成界面 */
            setContentView(R.layout.activity_picture);

             /* 实例化“图像生成界面”返回按钮 */
            bt_PictureBack = (Button) findViewById(R.id.pictureback);
            bt_PictureBack.setOnClickListener(new Back());
            bt_uppicture = (Button)findViewById(uppicture);
            bt_uppicture.setOnClickListener(new updatapicture());

             /* 实例化显示XY坐标数据的文本框 */
            x_axis = (TextView)findViewById(R.id.x_text);
            y_axis = (TextView)findViewById(R.id.y_text);

             /* 实例化按钮目标 */
            bt_test = (ImageButton)findViewById(R.id.person);

            dynamticTarget();
        }
    }

    class updatapicture implements OnClickListener{

        public void onClick(View view){
                /* 打开图像生成界面 */
            setContentView(R.layout.activity_picture);

             /* 实例化“图像生成界面”返回按钮 */
            bt_PictureBack = (Button) findViewById(R.id.pictureback);
            bt_PictureBack.setOnClickListener(new Back());
            bt_uppicture = (Button)findViewById(uppicture);
            bt_uppicture.setOnClickListener(new updatapicture());

             /* 实例化显示XY坐标数据的文本框 */
            x_axis = (TextView)findViewById(R.id.x_text);
            y_axis = (TextView)findViewById(R.id.y_text);

             /* 实例化按钮目标 */
            bt_test = (ImageButton)findViewById(R.id.person);

            ReadFileData();
            mWidgetController.setLayout(bt_test,x_devicelocation,y_devicelocation);


        }
    }

    private void setLayout(){
        mWidgetController.setLayout(bt_test,20,420);
    }

    private void dynamticTarget() {

        /* 新建线程，通过接收动态数据来实现目标动态移动 */
        PictureThreadone pictureThreadone = new PictureThreadone();
        Thread pictureone = new Thread(pictureThreadone);
        pictureone.start();
        Log.d("PictureThreadone","start");

        /* 新建线程，接收动态数据 */
        PictureThreadtwo pictureThreadtwo = new PictureThreadtwo();
        Thread picturetwo = new Thread(pictureThreadtwo);
        picturetwo.start();
        Log.d("PictureThreadtwo","start");
    }

    private String readData(){

        String filedata = "";
        String filelastdata = "";

        try {

            FileReader fr = new FileReader(path+name);
            char temp[] = new char[15];
            StringBuilder data = new StringBuilder("");
            int len = 0;
            while ((len = fr.read(temp)) > 0){
                data.append(new String(temp, 0, len));
                sign=1;
            }
            Log.d("msg", "readSaveFile: \n" + data.toString());
            fr.close();
            filedata = data.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return filedata;
    }


public void ReadFileData(){


    String filedata = "";
    int len = 0;
    /* 调用读取数据方法 */
    filedata = readData();
    while (sign == 1) {
        len = filedata.length();
        Log.d("", "length" + "  " + len);
                     /* s设置开始检测数据的位置 */
        int signfirst = filedata.indexOf("z");

                     /* 截取开始标志位处的数据 */

        String coordinatestart = filedata.substring(signfirst + 1, signfirst + 2);
        Log.d("", "coordinatestart" + " " + coordinatestart);

                     /* 截取结束标志位处的数据 */
        String coordinateend = filedata.substring(signfirst + 6, signfirst + 7);
        Log.d("", "coordinateend" + " " + coordinateend);

        while ((coordinatestart.equals("h")) && (coordinateend.equals("z"))) {
                         /* 截取需要的数据位 */
            coordinate = filedata.substring(signfirst + 2, signfirst + 6);
            xcoordinate = Integer.parseInt(coordinate.substring(0, 2));
            ycoordinate = Integer.parseInt(coordinate.substring(2, 4));

            x_devicelocation = xcoordinate * 16;
            y_devicelocation = ycoordinate * 16;

            Log.d("", "X:" + xcoordinate);
            Log.d("", "Y:" + ycoordinate);
        }
    }

}

    /**
     * Picture界面动态获取数据线程
     */
    private class PictureThreadtwo implements Runnable{

        String filedata = "";
        int len = 0;
        private PictureThreadtwo(){}

        public void run(){

                /* 调用读取数据方法 */
                   filedata = readData();
                   while (sign == 1) {
                       len = filedata.length();
                       Log.d("", "length" + "  " + len);
                     /* s设置开始检测数据的位置 */
                       int signfirst = filedata.indexOf("z");

                     /* 截取开始标志位处的数据 */

                           String coordinatestart = filedata.substring(signfirst + 1, signfirst + 2);
                           Log.d("", "coordinatestart" + " " + coordinatestart);

                     /* 截取结束标志位处的数据 */
                           String coordinateend = filedata.substring(signfirst + 6, signfirst + 7);
                           Log.d("", "coordinateend" + " " + coordinateend);

                           while ((coordinatestart.equals("h")) && (coordinateend.equals("z"))) {
                         /* 截取需要的数据位 */
                               coordinate = filedata.substring(signfirst + 2, signfirst + 6);
                               xcoordinate = Integer.parseInt(coordinate.substring(0, 2));
                               ycoordinate = Integer.parseInt(coordinate.substring(2, 4));

                               x_devicelocation = xcoordinate * 16;
                               y_devicelocation = ycoordinate * 16;

                               Log.d("", "X:" + xcoordinate);
                               Log.d("", "Y:" + ycoordinate);
                           }
                           sign = 0;
                           break;
                       }

        }
    }


    /**
     *  动态图像生成线程
     */
    private class PictureThreadone  implements Runnable{
        boolean picturesign = TRUE;
        int[] xy = new int[2];   /* 储存XY坐标数据的整形数组 */
        private PictureThreadone() {}

        public void run() {

                while (picturesign){

                        bt_test.offsetLeftAndRight(number1);
                        bt_test.offsetTopAndBottom(number2);


                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                     /* 获取目标当前的XY坐标数据，并存储到xy数组 */
                    bt_test.getLocationOnScreen(xy);
                     x = xy[0];    /* 获取X的数据 */
                     y = xy[1];    /* 获取Y的数据 */
                     x_distance = x-60;
                     y_distance = y-459;
                     Log.d("",""+x);
                     Log.d("",""+y);

                    int xdistance = x_devicelocation - x_distance;
                    int ydistance = y_devicelocation - y_distance;
                    Log.d("PictureThreadone","得到XY值");
                     /* 判断位置 */
                    if ((x_distance>= 1100)||(y_distance>= 1100)) {
                        number1 = -10; number2 = -10;
                    }

                    if((x_distance <= 0)||(y_distance<=0)){
                        number1 = 1; number2 = 1;
                    }

                    if(xdistance>0&&ydistance>0){

                        float value = xdistance/ydistance;
                        number1 = (int)value*number2;

                    }

                    if((x_distance==x_devicelocation)&&(y_distance==y_devicelocation)){

                        number1=0; number2=0;
                        picturesign = false;
                    }

                     /* 将获取到的XY的坐标数据处理后转成字符串 */
                    final String xaxis = (x_distance/16) + "";
                    final String yaxis = (y_distance/16) + "";

                     /* 将XY坐标数据发送给文本框显示 */
                    x_axis.post(new Runnable() {
                        @Override
                        public void run() {
                            x_axis.setText(new String(xaxis));
                        }
                    });
                    y_axis.post(new Runnable() {
                        @Override
                        public void run() {
                            y_axis.setText(new String(yaxis));
                        }
                    });
                }

        }

    }

    /**
     * "模拟定位"按钮事件
     */
    class Position implements OnClickListener {

        public void onClick(View view) {

             /* 打开模拟定位界面 */
            setContentView(R.layout.activity_position);

             /* 实例化“模拟定位”界面返回主界面的按钮 */
            bt_PositionBack = (Button) findViewById(R.id.positionback);
            bt_PositionBack.setOnClickListener(new Back());
        }
    }


    /**
     * "断开"按钮点击事件
     */
    public void bt_Disconnect(View view) {

        mAdapter.disable(); /* 关闭蓝牙 */

        Toast.makeText(this, "已断开", Toast.LENGTH_SHORT).show();
    }

    /**
     * "退出"按钮点击事件
     */
    public void bt_Exit(View view) {

        finish(); /* 结束程序 */
    }

    /**
     * 连接蓝牙设备
     */
    private void connectDevice(BluetoothDevice device) {

         /* 连接状态栏提示信息 */
        textState.setText(getResources().getString(R.string.connecting));

        try {
             /* 创建Socke t*/
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BT_UUID);

            /* 启动连接线程 */
            connectThread = new ConnectThread(socket, true);
            connectThread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * “连接”按键线程
     */
    private class ConnectThread extends Thread {

        int conncetsign = 0;
        int datasign = 0;
        int datanumbersign=1;
        private BluetoothSocket socket;  /* 定义接收的蓝牙设备 */
        private boolean activeConnect;   /* 连接标志位 */
        InputStream inputStream;         /* 定义数据输入流 */

         /* 获取参数 */
        private ConnectThread(BluetoothSocket socket, boolean connect) throws FileNotFoundException {
            this.socket = socket;
            this.activeConnect = connect;
        }

        public void run() {

          while(true) {
              try {
                   /* 如果是自动连接,则调用连接方法*/
                  if (activeConnect) {
                      socket.connect();
                  }

                   /* 传递连接状态 */
                  textState.post(new Runnable() {
                      @Override
                      public void run() {
                          textState.setText(getResources().getString(R.string.connect_success));//显示连接成功
                      }
                  });

                   /* 获取接收到的数据 */
                  inputStream = socket.getInputStream();
                  byte[] buffer = new byte[BUFFER_SIZE];

                  int bytes;
                  while (true) {

                       /* 获取接收到的数据的位数 */
                      bytes = inputStream.read(buffer);
                      if (bytes > 0) {
                          final byte[] data = new byte[bytes];

                           /* 得到接收的数据 */
                          System.arraycopy(buffer, 0, data, 0, bytes);
                          Log.d("data","  "+data);
                          single_text.post(new Runnable() {

                              public void run() {

                                   /* 将数据发送给文本框显示 */
                                      single_text.setText(new String(data) + "\n\n");

                                      }

                          });
                          conncetsign++;
                          while (conncetsign >=1) {
                                   /*将数据写到文件中*/
                              writeFileTool.writeTxtToFile(new String(data), path, name);
                              conncetsign = 0;
                              datanumber++;

                          }
                      }
                  }
              } catch (IOException e) {
                  e.printStackTrace();
                  textState.post(new Runnable() {
                      @Override
                      public void run() {
                          textState.setText(getResources().getString(R.string.connect_error));
                      }
                  });
              }
          }
        }
    }

    /**
     * 系统自定义的返回按钮方法
     */
    public void onBackPressed() {
        super.onBackPressed();
    }
    /**
     * 初始化“连接”界面的控件
     */
    private void connectInit(){

        /* 实例化“连接”界面的“返回”按钮，并设置点击函数 */
        bt_ConnectionBack = (Button) findViewById(R.id.connectback);
        bt_ConnectionBack.setOnClickListener(new Back());

         /* 实例化“连接”界面显示单行数据的文本框 */
        single_text = (TextView)findViewById(singledata);
        textState = (TextView) findViewById(text_state);

        bt_connecttodata = (Button)findViewById(R.id.connecttodata);
        bt_connecttodata.setOnClickListener(new Data());

        bt_connecttopicture = (Button)findViewById(R.id.connecttopicture);
        bt_connecttopicture.setOnClickListener(new Picture());


         /* 实例化显示蓝牙设备的列表 */
        blueToothList = (ListView)findViewById(R.id.noconnectedlist);

    }

    /**
     * 显示搜索到的蓝牙
     */
    private void displayBlueToothDevice(){

        /* 将蓝牙设备的名字和地址显示到列表中 */
        adapter = new BlueToothDeviceAdapter(getApplication(), R.layout.activity_device);
        blueToothList.setAdapter(adapter);

             /* 如果在搜索蓝牙设备，取消搜索 */
        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
             /* 开始搜索蓝牙设备 */
        mAdapter.startDiscovery();

             /* 将搜索到的蓝牙设备放到集合中 */
        Set<BluetoothDevice> pariedDevices = mAdapter.getBondedDevices();
        if (pariedDevices.size() > 0) {

            for (BluetoothDevice bluetoothdevice : pariedDevices) {
                adapter.add(bluetoothdevice);
            }
        }
    }

    /**
     * 蓝牙列表点击事件
     */
    private void clickBlueToothList(){

        /** 蓝牙列表点击事件 */
        blueToothList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

                 /* 获取被点击的蓝牙设备 */
                BluetoothDevice device = adapter.getItem(position);

                 /* 连接设备 */
                    connectDevice(device);
                }

        });
    }
}
