package com.example.weather_app_ame

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weather_app_ame.databinding.ActivityMainBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException
import java.math.RoundingMode
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMarkerClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var map: GoogleMap
    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createMapFragment()
        binding.ivSearch.setOnClickListener { searchByName() }

    }

    /**
     * Search location by name and put the marker on the map
     */
    private fun searchByName() {
        var location = binding.svLocation.text.trim().toString()
        if (location != "") {
            var addressList: MutableList<Address>
            val geocoder = Geocoder(applicationContext)
            try {
                addressList = geocoder.getFromLocationName(location, 1)
                Log.d("CITY", addressList.toString())
                if(addressList.size == 0) {
                    location += ", Spain"
                    addressList = geocoder.getFromLocationName(location, 1)

                }
                val address = addressList[0]
                val coordinates = LatLng(address.latitude, address.longitude)
                map.addMarker(MarkerOptions().position(coordinates).title(location))
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(coordinates, 16f),
                    4000,
                    null
                )
            } catch (e: IOException) {
                Toast.makeText(this, "Geocoder is no available at this moment...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "City doesn't exist, try to specify the country.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Create Google Maps
     */
    private fun createMapFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Initialize Google Maps
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        createMarker()
        map.setOnMyLocationClickListener(this)
        map.setOnMarkerClickListener(this)
        enableMyLocation()
    }

    /**
     * Create the first marker on map. (location: URV Sescelades)
     */
    private fun createMarker() {
        val coordinates = LatLng(41.133149, 1.242923)
        val marker = MarkerOptions().position(coordinates).title("Universitat Rovira i Virgili")
        map.addMarker(marker)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinates, 16f),
            4000,
            null
        )
    }

    // Functions to check permissions
    private fun isLocationPermissionsGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (isLocationPermissionsGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "Accept Location permissions on Settings", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE_LOCATION -> if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                map.isMyLocationEnabled = true
            }else{
                Toast.makeText(this, "To activate location accept Location permissions on Settings", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if(!isLocationPermissionsGranted()){
            map.isMyLocationEnabled = false
            Toast.makeText(this, "To activate location accept Location permissions on Settings", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Member function of GoogleMap
     */
    override fun onMyLocationClick(location: Location) {
        val latitude = location.latitude.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
        val longitude = location.longitude.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
        Toast.makeText(this, "Lat: $latitude Lon: $longitude", Toast.LENGTH_SHORT).show()
        val intentAdd = Intent(this, ResultCurrentWeatherActivity::class.java)
        intentAdd.putExtra("INTENT_LATITUDE", latitude)
        intentAdd.putExtra("INTENT_LONGITUDE", longitude)
        startActivity(intentAdd)
    }

    /**
     * Member function of GoogleMap
     */
    override fun onMarkerClick(location: Marker): Boolean {
        val latitude = location.position.latitude.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
        val longitude = location.position.longitude.toBigDecimal().setScale(2, RoundingMode.UP).toDouble()
        Toast.makeText(this, "Lat: $latitude Lon: $longitude", Toast.LENGTH_SHORT).show()
        val intentAdd = Intent(this, ResultCurrentWeatherActivity::class.java)
        intentAdd.putExtra("INTENT_LATITUDE", latitude)
        intentAdd.putExtra("INTENT_LONGITUDE", longitude)
        startActivity(intentAdd)
        return false
    }

}