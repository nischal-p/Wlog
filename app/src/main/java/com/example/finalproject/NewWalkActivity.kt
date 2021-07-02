package com.example.finalproject

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DatabaseReference
import kotlinx.android.synthetic.main.activity_new_walk.*
import java.text.SimpleDateFormat
import java.util.*


class NewWalkActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {

    lateinit var database : DatabaseReference

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var mMap: GoogleMap

    private lateinit var routeLine: Polyline

    private var gotNewLocation = false

    private var lastKnownLocation: Location? = null

    private var locationPermissionGranted = false

    private var seconds = 0

    private var tripLog: MutableList<LatLng> = mutableListOf<LatLng>()
    private var walkLength: Float = 0.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_walk)

        database = MainActivity.database

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val timer = Timer()
        val timehandler = Handler()
        val timetask = object : TimerTask() {
            override fun run() {
                timehandler.post(Runnable {
                    val hours: Int = seconds / 3600
                    val minutes: Int = seconds % 3600 / 60
                    val secs: Int = seconds % 60

                    // Format the seconds into hours, minutes,
                    // and seconds.

                    // Format the seconds into hours, minutes,
                    // and seconds.
                    val timeElapsed = String
                        .format(
                            Locale.getDefault(),
                            "%d:%02d:%02d", hours,
                            minutes, secs
                        )

                    time_label.text = timeElapsed
                    seconds++
                })
            }
        }
        timer.schedule(timetask, 0, 1000)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setUpMap()

        locationPermissionGranted = ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        mMap.isMyLocationEnabled = true

        addNewLocation()

        drop_pin_btn.setOnClickListener {
            addNewLocation()
        }

        cancel_walk_btn.setOnClickListener{
            val returnIntent = Intent()
            setResult(Activity.RESULT_CANCELED, returnIntent)
            finish()
        }

        end_walk_btn.setOnClickListener{
            // first verify the description and photo is not empty
            if (description_field.text.isNotEmpty()) {
                saveToFirebase()
            } else {
                description_field.hint = "Please enter a caption first"
                Toast.makeText(this, "Please enter a caption first", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun addNewLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // we were successful getting the new location
                        val potentialNewLocation = task.result
                        if (lastKnownLocation != null && potentialNewLocation != null && potentialNewLocation.distanceTo(lastKnownLocation) < 10.0) {
                            Toast.makeText(this, "Please walk at least 10 metres to drop a new marker!", Toast.LENGTH_SHORT).show()
                        }
                        else if (potentialNewLocation != null){
                            // new location is accepted
                            gotNewLocation = true
                            val myLatLng = LatLng(
                                potentialNewLocation.latitude,
                                potentialNewLocation.longitude
                            )
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, DEFAULT_ZOOM.toFloat()))
                            var markerTitle = "Point number: " + (tripLog.size + 1).toString()
                            if (tripLog.size == 0) {
                                markerTitle = "Starting Point"
                            } else {
                                walkLength = walkLength + potentialNewLocation.distanceTo(lastKnownLocation)
                            }

                            mMap.addMarker(MarkerOptions().position(myLatLng).title(markerTitle))
                            Toast.makeText(this, markerTitle + " marker has been dropped!", Toast.LENGTH_SHORT).show()
                            tripLog.add(myLatLng)
                            routeLine.remove()
                            routeLine = mMap.addPolyline(PolylineOptions().addAll(tripLog).width(10.toFloat()).color(Color.BLUE).geodesic(true))
                            gotNewLocation = false
                            distance_label.text = "Distance so far in miles: " + String.format("%.2f", (walkLength * 0.000621371))
                            lastKnownLocation = potentialNewLocation
                            println("new lat lng list: " + tripLog.toString())
                        }

                    } else {
                        Log.d("Map Location Issue", "Current location is null. Using defaults.")
                        Log.e("Map Locatio nIssue", "Exception: %s", task.exception)
                    }
                }
            }
            else {
                Toast.makeText(this, "Sorry, cannot log walks without location enabled.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        routeLine = mMap.addPolyline(PolylineOptions())
    }

    private val checkPermissionGrantStatus: (Int) -> Boolean = {
        it == PackageManager.PERMISSION_GRANTED
    }

    // once request is provided
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults.all(checkPermissionGrantStatus))) {
                // permission was granted, yay! Do the thang
                locationPermissionGranted = true
                setUpMap()
            } else {
                println(grantResults)
                Toast.makeText(this, "Sorry, cannot log walks without location enabled.", Toast.LENGTH_SHORT).show()
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

        }
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n" + location.latitude.toString() + location.longitude.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "This is your current location", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    private fun saveToFirebase() {

        // var tripLogArray: List<String> = tripLog.map { it -> it.latitude.toString() + "," + it.longitude.toString() }

        // create a walk log
        val wlog = WalkLogItem(
            description_field.text.toString(),
            SimpleDateFormat("yyyyMMdd").format(Date()).toString(),
            tripLog.map { it.latitude.toString() + "," + it.longitude.toString() },
            walkLength,
            seconds
        )

        // make a key for this profile
        val key = database.child("wlogs").push().key!!
        wlog.uuid = key
        database.child("wlogs").child(key).setValue(wlog)

        val returnIntent = Intent()
        setResult(Activity.RESULT_OK, returnIntent)
        finish()
    }


    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val DEFAULT_ZOOM = 18
    }



}