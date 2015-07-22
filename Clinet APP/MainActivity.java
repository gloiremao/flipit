package josephtien.npgame;

import android.app.Service;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;


public class MainActivity extends ActionBarActivity {
    SensorManager gSensorManager;
    Sensor gyroscopeSensor;
    long timestamp;
    private static final float NS2S = 1.0f / 1000000000.0f;
    TextView playText;
    int state=0;
    float xPos=0;
    int curXPOS=0;
    int tarXPOS=0;
    String address = "140.114.79.67";
    int port = 6000;
    boolean connection = false;
    Thread tcp;
    Thread udp;
    Socket client;
    DatagramSocket uClient;
    int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hello);
        mHandler = new MHandler();
        myVibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
        //switchLayout(1);//test
        //sendMsg(SWITCH,2);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(state==1)regis();
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregis();
        if(state!=0){
            sendMsg(SWITCH, 0);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    boolean waiting=false;
    public void switchLayout(int tarLayout){
        sendMsg(VIB, 500);
        waiting = false;
        if(tarLayout == 0) {
            setContentView(R.layout.hello);
            stopClient();
            unregis();
        }else if(tarLayout == 1){
            setContentView(R.layout.control);
            playText = (TextView)findViewById(R.id.playText);
            xPos=0;
            regis();
            ImageButton ib = (ImageButton)findViewById(R.id.playBtn);
            if(id==1)
                ib.setBackgroundResource(R.drawable.jet1);
            else if(id==2)
                ib.setBackgroundResource(R.drawable.jet2);
            else if(id==3)
                ib.setBackgroundResource(R.drawable.jet3);
            else
                ib.setBackgroundResource(R.drawable.jet4);
        }else{
            setContentView(R.layout.wait);
            (new Waiting()).start();
        }
        state=tarLayout;
    }

    public void reGen(View view){
        port = 6000;
        ((EditText) findViewById(R.id.serverET)).setText("");
        ((EditText) findViewById(R.id.portET)).setText("");
    }

    public void genIP(View view){
        ((EditText) findViewById(R.id.serverET)).setText(address);
    }
    public void genPort(View view){
        port++;
        ((EditText) findViewById(R.id.portET)).setText(""+port);
    }

    public void onConnectBtn(View view){
        EditText et = (EditText)findViewById(R.id.serverET);
        String ipStr = et.getText().toString();
        address = ipStr;//.split(":")[0];
        //port = Integer.parseInt(ipStr.split(":")[1]);
        port = Integer.parseInt(((EditText) findViewById(R.id.portET)).getText().toString());
        //stopClient();
        tcp = new TCP();
        tcp.start();
    }
    public void onPlayBtn(View view){
        xPos = 0;
    }
    public void regis(){
        gSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor gSensor = gSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        gSensorManager.registerListener(gyroscopeListener, gSensor, gSensorManager.SENSOR_DELAY_GAME);
        xPos = tarXPOS = 0;
    }
    public void unregis(){
        if(gSensorManager!=null)
            gSensorManager.unregisterListener(gyroscopeListener);
    }
    private SensorEventListener gyroscopeListener = new SensorEventListener(){
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1){
            // TODO Auto-generated method stub
        }
        @Override
        public void onSensorChanged(SensorEvent event){
            // TODO Auto-generated method stub
            //Log.v("hihi",event.values[0]+"||"+event.values[1]+"||"+event.values[2]);
            if(timestamp!=0){
                final float dT = (event.timestamp - timestamp) * NS2S;
                xPos+=(event.values[2] * dT);
                playText.setText((int)(xPos*10) + "");
            }
            timestamp = event.timestamp;
        }
    };
    void stopClient(){
        try {
            sendMsg(VIB,500);
            //sendMsg(SWITCH,0);
            if(client != null)client.close();
            if(uClient != null)uClient.close();
            connection = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class TCP extends Thread {
        public void run() { // overwrite Thread's run()
            client = new Socket();
            InetSocketAddress isa = new InetSocketAddress(address, port);
            try {
                client.connect(isa, 10000);
                //DataInputStream input= new DataInputStream(client.getInputStream());
//                Scanner input = new Scanner(client.getInputStream());
//                id = Integer.parseInt(input.nextLine());
                DataInputStream input= new DataInputStream(client.getInputStream());
//                DataInputStream input = new DataInputStream(new
//                        BufferedInputStream(client.getInputStream()));
//                id = input.readInt();

                String str = input.readUTF();
                id = Integer.parseInt(str);
                sendMsg(SWITCH, 2);
                connection = true;
                while(connection){
                    //while(!input.hasNext());
                    //String recvStr = input.nextLine();
                    String recvStr = input.readUTF();
                    Log.v("hihi","recv!!! : "+recvStr);
                    if(recvStr.equals("dead")){
                        sendMsg(SWITCH, 0);
                        sendMsg(VIB, 0);
                    }else if(recvStr.equals("go")){
                        if(state==1)continue;
                        sendMsg(SWITCH, 1);
                        sendMsg(UDP,0);
                    }
                }
                client.close();
            } catch (java.io.IOException e) {
                sendMsg(SWITCH,0);
            }
        }
    }
    class Waiting extends Thread {
        public void run() { // overwrite Thread's run()
            waiting = true;
            int dotNum = 0;
            while (waiting) {
                String o = "Wait";
                for (int i = 0; i < dotNum; i++) {
                    o += ".";
                }
                sendMsg(WTV, o);
                dotNum = (dotNum + 1) % 5;
                pause(500);
            }
        }
    }
    class UDP extends Thread {
        public void run() { // overwrite Thread's run()
            try {
                int uPort = port +1000;
                uClient = new DatagramSocket(uPort);
                InetAddress IPAddress = InetAddress.getByName(address);
                byte[] sendData;
                String motion = "still";
                String msg = String.format("%d,%s", id,motion);
                sendData = msg.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, uPort);
                while(connection){
                    Thread.sleep(50);
                    curXPOS = (int)(xPos*50);
                    if(tarXPOS>curXPOS){
                        motion = "right";
                        tarXPOS--;
                    }else if(tarXPOS<curXPOS){
                        motion = "left";
                        tarXPOS++;
                    }else{
                        motion = "still";
                        continue;
                    }
                    msg = String.format("%d,%s", id,motion);
                    sendData = msg.getBytes();
                    sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, uPort);
                    uClient.send(sendPacket);
                }
                connection=false;
                uClient.close();
            } catch (java.io.IOException e) {
                sendMsg(SWITCH, 0);
            } catch (InterruptedException e) {
                sendMsg(SWITCH,0);
            }
        }
    }

    static void pause(int t){
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /******************************************/
    final int SWITCH=0;
    final int UDP=1;
    final int VIB = 2;
    final int WTV = 3;
    Handler mHandler;
    Vibrator myVibrator;
    class MHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case SWITCH:
                    switchLayout((int)msg.obj);
                    break;
                case UDP:
                    udp = new UDP();
                    udp.start();
                    break;
                case VIB:
                    myVibrator.vibrate((int) msg.obj);
                    break;
                case WTV:
                    TextView wtv = (TextView) findViewById(R.id.waitTV);
                    if(wtv!=null)
                        wtv.setText((String) msg.obj);
                    break;
            }
            super.handleMessage(msg);
        }
    }
    void sendMsg(int what,Object obj){
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }
}
