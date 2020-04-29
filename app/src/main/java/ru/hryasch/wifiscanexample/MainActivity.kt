package ru.hryasch.wifiscanexample

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions


class MainActivity : AppCompatActivity()
{
    private lateinit var myConnectButton: Button
    private lateinit var myButton: Button
    private lateinit var myList: ListView
    private lateinit var wifiManager: WifiManager
    private lateinit var results: List<ScanResult>
    private val arrayList = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private var isInitialized = false
    private lateinit var lm:LocationManager
    private lateinit var gpsReceiver: BroadcastReceiver
    private lateinit var wifiStateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myConnectButton = findViewById(R.id.myConnectButton)
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

        wifiStateReceiver = object: BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?)
            {
                Log.d("ActionOnAir:", "$p1")
                if (p1!!.action!! == WifiManager.SUPPLICANT_STATE_CHANGED_ACTION )
                {
                    if (checkWiFi())
                    {
                        Toast.makeText(this@MainActivity, "CONNECTED", Toast.LENGTH_SHORT).show()
                        try
                        {
                            unregisterReceiver(this)
                        }
                        catch (e: Exception)
                        {
                            Log.d("4345345", "already unregistered")
                        }
                    }
                    else
                    {
                        Toast.makeText(this@MainActivity, "DISCONNECTED", Toast.LENGTH_SHORT).show()
                    }
                }
                else
                {
                    Log.d("WTF", "MSG: ${intent.action!!}, EXPECTED: ${WifiManager.SUPPLICANT_STATE_CHANGED_ACTION}")
                }
            }
        }

        myConnectButton.setOnClickListener {

            //Old way
            val networkSampleSSID = "Hryasch's WLAN"
            val networkSamplePass = "SamplePass"
            val configuration = WifiConfiguration()
            configuration.SSID = "\"" + networkSampleSSID + "\""
            configuration.preSharedKey = "\"" + networkSamplePass + "\""
            wifiManager.addNetwork(configuration)

            val list = wifiManager.configuredNetworks
            for (i in list)
            {
                if (i.SSID != null && i.SSID == "\"" + networkSampleSSID + "\"")
                {
                    wifiManager.disconnect()

                    try
                    {
                        registerReceiver(wifiStateReceiver, IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
                    }
                    catch (e: Exception)
                    {
                        Log.d("4345345", "already registered")
                    }

                    wifiManager.enableNetwork(i.networkId, true)
                    wifiManager.reconnect()
                    Log.d("4345345", "try to connect....")
                    break
                }
            }
        }

        myConnectButton.isEnabled = false

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
                            myConnectButton.isEnabled = true
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

    private fun checkWiFi(): Boolean
    {
        val info = wifiManager.connectionInfo
        Log.d("785675", "Connected to: ${info.ssid} ${info.bssid}")
        return info.bssid != null
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
            Log.i("11111111", "unregister receiver")

            for (scanResult in results)
            {
                arrayList.add(scanResult.SSID + " " + scanResult.BSSID)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
