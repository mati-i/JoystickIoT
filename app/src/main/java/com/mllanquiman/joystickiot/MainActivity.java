package com.mllanquiman.joystickiot;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joystickjhr.JoystickJhr;
import com.github.angads25.toggle.widget.LabeledSwitch;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    //-----------------------------------------------------------------------------------------
    //variables para medir los datos del joystick
    private TextView distanceX, distanceY, angle, distance, dirKey;
    private int dir_anterior = 0;

    //-----------------------------------------------------------------------------------------
    private ProgressBar progressBar,progressBar2;
    private int count = 0; //para aumentar la barra de progreso
    private int percentSpeed, levelSpeed = 0;
    private int velocity; //para almacenar la distancia del joystick
    private String connectedValue;
    private LabeledSwitch labeledSwitch; //switch de encendido de conexión con el ESP8266
    private boolean connected = false;
    private Toast toastNoConnected;
    private Toast toastConnected;

    //-----------------------------------------------------------------------------------------
    //Variables para cambiar el valor del progreso
    private TextView txtProgre1, txtProgre2;

    //-----------------------------------------------------------------------------------------
    //variables para tener acceso a la base de datos
    private FirebaseDatabase database;
    private DatabaseReference angleDB, switchDB,motorLvl,motorSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //--------------------------------------------------------------------------------------
        database = FirebaseDatabase.getInstance(); //obteniendo acceso a la base de datos

        //referencias a los datos de la base de datos
        angleDB = database.getReference("angle");
        switchDB = database.getReference("connected");
        motorLvl = database.getReference("motorLvl");
        motorSpeed = database.getReference("motorSpeed");
        //--------------------------------------------------------------------------------------
        //Datos para crear la conexión con la interfaz gráfica
        final JoystickJhr joystickJhr = findViewById(R.id.joystickJhr);
        distanceX = findViewById(R.id.distanciaX);
        distanceY = findViewById(R.id.distanciaY);
        angle = findViewById(R.id.angle);
        distance = findViewById(R.id.distancia);
        dirKey = findViewById(R.id.dirKey); //variable para indicar dirección
        progressBar = findViewById(R.id.progressBar);
        progressBar2 = findViewById(R.id.progressBar2);
        labeledSwitch = findViewById(R.id.btn_switch);
        txtProgre1 = findViewById(R.id.txt_progre_1);
        txtProgre2 = findViewById(R.id.txt_progre_2);

        //Mensajes para saber si el ESP8266 tiene internet
        toastNoConnected = Toast.makeText(this, "El ESP8266 no tiene acceso a internet", Toast.LENGTH_SHORT);
        toastConnected = Toast.makeText(this, "El ESP8266 se ha conectado a internet", Toast.LENGTH_SHORT);
        //--------------------------------------------------------------------------------------
        //Metodos para modificar el estado del switch
        switchDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //la variable snapshot se usa para capturar valores de la base de datos
                connectedValue = String.valueOf(snapshot.getValue());
                Log.d("TAG", "---->" + connectedValue); //para verificar valor por consola

                //si el valor es 1 el switch se encenderá sino se apaga
                if (connectedValue.equals("1")) {
                    labeledSwitch.setOn(true);
                    connected = true;
                    toastConnected.show();
                } else if (connectedValue.equals("0")) {
                    labeledSwitch.setOn(false);
                    connected = false;
                    toastNoConnected.show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        //--------------------------------------------------------------------------------------
        //Metodo para capturar los datos del joystick cuando este en movimiento
        joystickJhr.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //moviendo el joystick y estableciendo valores
                joystickJhr.move(motionEvent);
                distanceX.setText("Distancia X = " + joystickJhr.joyX());
                distanceY.setText("Distancia Y = " + joystickJhr.joyY());
                angle.setText("Angle = " + joystickJhr.angle());
                distance.setText("Distancia = " + joystickJhr.distancia());

                //------------------------------------------------------------------------------
                //Para aumentar la barra de progreso segun posicion del joystick
                velocity = (int) joystickJhr.distancia();
                speed();
                percentSpeed = (velocity/2)+2;
                if(velocity <2){
                    percentSpeed =0;
                }
                motorSpeed.setValue(percentSpeed);
                txtProgre1.setText(String.valueOf(percentSpeed) +"%");
                Log.d("TAG", "---->" + percentSpeed); //solo para revisar por consola el valor

                //------------------------------------------------------------------------------
                int angleInt = (int) joystickJhr.angle();

                if (angleInt == 0) {
                    angleInt = 90; //estableciendo angleInt como 90 por defecto
                }
                //-------------------------------------------------------------------------------
                //Este if es para controlar el servoMotor
                //El servo solo permite valores entre 0 y 180
                //Se resta 360 menos los valores mayores a 180 y menor o igual a 360
                if (angleInt <= 360 && angleInt > 180) {
                    angleInt = 360 - angleInt;
                }
                //y se envia el valor final a la variable de la base de datos
                angleDB.setValue(angleInt);

                //-------------------------------------------------------------------------------
                //Capturando la dirección
                int dir = joystickJhr.getDireccion();

                if (dir_anterior != dir) {
                    dir_anterior = dir;

                    if (dir == joystickJhr.STICK_UP) {
                        dirKey.setText("Direccion = Up");

                    } else if (dir == joystickJhr.STICK_UPRIGHT) {
                        dirKey.setText("Direccion = Up Right");
                    } else if (dir == joystickJhr.STICK_RIGHT) {
                        dirKey.setText("Direccion = Right");
                    } else if (dir == joystickJhr.STICK_DOWNRIGHT) {
                        dirKey.setText("Direccion = Down Right");
                    } else if (dir == joystickJhr.STICK_DOWN) {
                        dirKey.setText("Direccion = Down");
                    } else if (dir == joystickJhr.STICK_DOWNLEFT) {
                        dirKey.setText("Direccion = Down Left");
                    } else if (dir == joystickJhr.STICK_LEFT) {
                        dirKey.setText("Direccion = Left");
                    } else if (dir == joystickJhr.STICK_UPLEFT) {
                        dirKey.setText("Direccion = Up Left");
                    } else if (dir == joystickJhr.STICK_NONE) {
                        dirKey.setText("Direccion = Center");
                    }
                }

                return true;
            }
        });

    }

    //-------------------------------------------------------------------------------
    //Metodo para mostrar la velocidad con una barra de progreso principal
    public void speed() {
            //Esta barra de progreso solo llega hasta 50
            //distancia del joystick dividio 4 hasta la distancia actual -146 para no pasar de 49 en progreso
            setProgressBar(progressBar, (velocity/4)+1,velocity-146);
    }

    //-------------------------------------------------------------------------------
    //Metodo para mostar el nivel de velocidad con barra de progreso secundaria
    public void levelSpeedUp(View v) {
            count += 23;//para ir aumentado el contador
            //con un contador que aumenta de 10 en 10
            //se aumenta desde el valor actual del contador menos 10 hasta el valor actual del contador
            //esta barra de progreso solo llega hasta 70
            setProgressBar(progressBar2, count-23, count);
            //para que el contador no pase de 70
            levelSpeed +=1;
            if (count > 70) {
                count = 69;
                levelSpeed =3;
            }
            Log.d("TAG", "---->" + count);
            txtProgre2.setText(String.valueOf(levelSpeed));
            motorLvl.setValue(levelSpeed);

    }

    //-------------------------------------------------------------------------------
    //Metodo para disminuir la barra de progreso secundaria
    public void levelSpeedDown(View v){

        count -= 23; //para ir diminuyendo el contador
        setProgressBar(progressBar2,count+23,count ); //para disminuir la barra

        //para que el contador no pase de 0
        levelSpeed -=1;
        if (count < 10) {
            count = 0;
            levelSpeed =0;
        }
        Log.d("TAG", "---->" + count);
        txtProgre2.setText(String.valueOf(levelSpeed));
        motorLvl.setValue(levelSpeed);

    }

    //-------------------------------------------------------------------------------
    //Metodo para establecer el progreso de la barra de progreso
    public void setProgressBar(ProgressBar pB, int a, int b){
        ObjectAnimator animation = ObjectAnimator.ofInt(pB, "progress", a, b);
        animation.setDuration(250);
        animation.setAutoCancel(true);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    //-------------------------------------------------------------------------------
    //Al cerrar la aplicación también se desconecta del ESP8266
    @Override
    protected void onDestroy() {
        super.onDestroy();
        switchDB.setValue(0);
    }
}