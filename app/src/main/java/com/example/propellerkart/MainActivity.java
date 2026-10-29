package com.example.propellerkart;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // adresse MAC du Bluetooth du PC (trouvee avec ipconfig)
    private static final String PC_MAC = "20:17:02:23:82:06";

    // UUID standard RFCOMM (SPP)
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private OutputStream outputStream;

    // Accelerometre
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float currentX = 0, currentY = 0, currentZ = 0;

    // Etat
    private int boutonKlaxon = 0;       // 0 = inactif, 5 = actif
    private boolean sending = false;    // envoi auto en cours ou non

    // UI
    private TextView status, textX, textY, textZ, textTrame;
    private ProgressBar progressVertical, progressHorizontal;
    private Button btnKlaxon, btnSendToggle;

    // pour l'envoi periodique toutes les 50 ms
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sendTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.status);
        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        textTrame = findViewById(R.id.textTrame);
        progressVertical = findViewById(R.id.progressVertical);
        progressHorizontal = findViewById(R.id.progressHorizontal);
        btnKlaxon = findViewById(R.id.btnKlaxon);
        btnSendToggle = findViewById(R.id.btnSendToggle);
        Button btnConnect = findViewById(R.id.btnConnect);

        // Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
        }, 1);

        // Accelerometre
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { connectToPc(); }
        });

        // bouton klaxon : bascule entre 0 et 5
        btnKlaxon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (boutonKlaxon == 0) {
                    boutonKlaxon = 5;
                    btnKlaxon.setText("Klaxon : ACTIF (b=5)");
                } else {
                    boutonKlaxon = 0;
                    btnKlaxon.setText("Klaxon : INACTIF (b=0)");
                }
            }
        });

        // bouton démarrer/arrêter l'envoi auto
        btnSendToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!sending) {
                    startSending();
                    btnSendToggle.setText("Arrêter l'envoi");
                } else {
                    stopSending();
                    btnSendToggle.setText("Démarrer l'envoi auto");
                }
            }
        });
    }

    // ---- Accelerometre ----
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentX = event.values[0];
        currentY = event.values[1];
        currentZ = event.values[2];

        textX.setText(String.format(Locale.US, "x = %.2f", currentX));
        textY.setText(String.format(Locale.US, "y = %.2f", currentY));
        textZ.setText(String.format(Locale.US, "z = %.2f", currentZ));

        progressHorizontal.setProgress((int) (currentX + 10));
        progressVertical.setProgress((int) (currentY + 10));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    // ---- construction de la trame ----
    private String buildTrame() {
        // format : #b=5#x=-7.12#y=+3.45#z=+2.23#<CR>
        return "#b=" + boutonKlaxon
                + "#x=" + String.format(Locale.US, "%+.2f", currentX)
                + "#y=" + String.format(Locale.US, "%+.2f", currentY)
                + "#z=" + String.format(Locale.US, "%+.2f", currentZ)
                + "#" + (char) 13;   // (char)13 = CR (retour chariot)
    }

    // ---- envoi auto toutes les 50 ms ----
    private void startSending() {
        if (outputStream == null) {
            showToast("Connectez-vous au PC d'abord");
            return;
        }
        sending = true;
        sendTask = new Runnable() {
            public void run() {
                sendTrame(buildTrame());
                handler.postDelayed(this, 50);  // 50 ms
            }
        };
        handler.post(sendTask);
    }

    private void stopSending() {
        sending = false;
        if (sendTask != null) {
            handler.removeCallbacks(sendTask);
        }
    }

    // ---- envoi Bluetooth ----
    private void sendTrame(final String trame) {
        textTrame.setText(trame.replace((char) 13, ' ').trim());

        new Thread(new Runnable() {
            public void run() {
                try {
                    if (outputStream != null) {
                        outputStream.write(trame.getBytes());
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    showToast("Erreur envoi : " + e.getMessage());
                }
            }
        }).start();
    }

    // ---- connexion Bluetooth ----
    private void connectToPc() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            android.Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        showToast("Permission Bluetooth manquante");
                        return;
                    }

                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(PC_MAC);
                    bluetoothAdapter.cancelDiscovery();

                    try {
                        // méthode normale
                        socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                        socket.connect();
                    } catch (IOException e1) {
                        // méthode de secours (fallback) si la normale echoue
                        socket = (BluetoothSocket) device.getClass()
                                .getMethod("createRfcommSocket", int.class)
                                .invoke(device, 1);
                        socket.connect();
                    }

                    outputStream = socket.getOutputStream();

                    updateStatus("Connecté au PC", true);
                    showToast("Connexion réussie");

                } catch (SecurityException e) {
                    showToast("Permission refusée");
                } catch (Exception e) {
                    updateStatus("Échec connexion", false);
                    showToast("Erreur : " + e.getMessage());
                }
            }
        }).start();
    }

    private void updateStatus(final String text, final boolean ok) {
        handler.post(new Runnable() {
            public void run() {
                status.setText(text);
                status.setTextColor(ok ? 0xFF2E7D32 : 0xFFD32F2F);
            }
        });
    }

    private void showToast(final String text) {
        handler.post(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}