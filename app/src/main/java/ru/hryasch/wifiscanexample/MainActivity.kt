package ru.hryasch.wifiscanexample

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import java.lang.Exception


class MainActivity : AppCompatActivity()
{
    private lateinit var myButton: Button
    private lateinit var myList: ListView
    private lateinit var wifiManager: WifiManager
    private lateinit var results: List<ScanResult>
    private val arrayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isInitialized = false
    private lateinit var lm:LocationManager
    private lateinit var gpsReceiver: BroadcastReceiver
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myButton = findViewById(R.id.mybutton)
        myList = findViewById(R.id.mylist)

        lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent)
            {
                if (intent.action!! == LocationManager.PROVIDERS_CHANGED_ACTION)
                {
                    tryScan()
                }
            }
        }

        myButton.setOnClickListener {
            Permissions
                .check(this,
                    arrayOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    "rationale",
                    Permissions.Options()
                        .setRationaleDialogTitle("Rationale dialog"),
                    object: PermissionHandler()
                    {
                        override fun onGranted()
                        {
                            isScanning = true
                            tryScan()
                        }
                    })
        }
    }

    private fun tryScan()
    {
        if (checkGps())
        {
            try
            {
                unregisterReceiver(gpsReceiver)
            }
            catch (e: IllegalArgumentException)
            {
                Log.d("12345", "receiver already unregistered")
            }

            startScan()
        }
        else
        {
            try
            {
                registerReceiver(gpsReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
            }
            catch (e: Exception)
            {
                Log.d("12345", "receiver already registered")
            }

            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun checkGps(): Boolean
    {
        val gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d("12345", "gps: $gpsEnabled, network: $networkEnabled")

        return gpsEnabled && networkEnabled
    }

    private fun startScan()
    {
        if (!wifiManager.isWifiEnabled)
        {
            Log.i("11111111", "WiFi was off, enabling...")
            wifiManager.isWifiEnabled = true
        }

        if (!isInitialized)
        {
            adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList)
            myList.adapter = adapter
            isInitialized = true
            Log.i("11111111", "initialized")
        }

        scanWifi()
    }

    private fun scanWifi()
    {
        Log.i("11111111", "register receiver")
        arrayList.clear()
        adapter.notifyDataSetChanged()
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        wifiManager.startScan()
        Log.i("11111111", "Scanning wifi...")
    }

    private val wifiReceiver = object: BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?)
        {
            results = wifiManager.scanResults
            unregisterReceiver(this)
            isScanning = false
            Log.i("11111111", "unregister receiver")

            for (scanResult in results)
            {
                arrayList.add(scanResult.SSID + " " + scanResult.BSSID)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
