package com.example.finalproject

import android.app.Activity
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.view.View

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.stats_layout.*
import kotlinx.android.synthetic.main.weather_layout.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    companion object {
        val database : DatabaseReference = FirebaseDatabase.getInstance().reference
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }

    val walkPostList = ArrayList<WalkLogItem>()
    val postIds = ArrayList<String>()

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var locationPermissionGranted = false

    val NEW_WLOG = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nav_view.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        new_walk_fab.setOnClickListener{
            Toast.makeText(this, "Starting your walk...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, NewWalkActivity::class.java)
            startActivityForResult(intent, NEW_WLOG)
        }

        // creates a vertical linear layout manager
        viewManager = LinearLayoutManager(this)

        //adapter with click listener
        viewAdapter = WalkPostAdapter(this, walkPostList)

        walks.findViewById<RecyclerView>(R.id.walks_recycler_view).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val walkPostsListener =  object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("ERROR", error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    // for each data piece, we want to extract the profile
                    val post = it.getValue<WalkLogItem>(WalkLogItem::class.java)

                    if (!postIds.contains(post!!.uuid)) {
                        walkPostList.add(post)
                        postIds.add(post.uuid)
                        viewAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
        database.child("wlogs").addValueEventListener(walkPostsListener)

        // stat image
        Picasso.get()
            .load("https://media.npr.org/assets/img/2017/07/09/istock-514053020-a1852e49a73e8903c4fe44b65c5bb45bceb99430-s800-c85.jpg") // load the image
            .into(stats_image)

        // weather image
        Picasso.get()
            .load("https://www.thesouthafrican.com/wp-content/uploads/2018/08/cloud-346710_1280.png") // load the image
            .into(weather_image)
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_walks -> {
                walks.visibility = View.VISIBLE
                weather.visibility = View.INVISIBLE
                stats.visibility = View.INVISIBLE
                new_walk_fab.visibility = View.VISIBLE
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_weather -> {
                walks.visibility = View.INVISIBLE
                weather.visibility = View.VISIBLE
                stats.visibility = View.INVISIBLE
                new_walk_fab.visibility = View.INVISIBLE
                getAndUpdateWeather()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_stats -> {
                walks.visibility = View.INVISIBLE
                weather.visibility = View.INVISIBLE
                new_walk_fab.visibility = View.INVISIBLE
                stats.visibility = View.VISIBLE

                if (!walkPostList.isEmpty()){
                    calculateAndShowStats()
                }
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun calculateAndShowStats() {
        val distanceList = walkPostList.map{it -> it.jog_length}
        val timeList = walkPostList.map{it -> it.jog_time_seconds}

        val maxDist = String.format("%.2f", distanceList.max()!! * 0.000621371)
        val avgDist = String.format("%.2f", distanceList.average() * 0.000621371)
        val totalDist = String.format("%.2f", distanceList.sum() * 0.000621371)

        val maxTime = getTimeStringFromSeconds(timeList.max()!!)
        val avgTime = getTimeStringFromSeconds(timeList.average()!!.toInt())
        val totalTime = getTimeStringFromSeconds(timeList.sum()!!)

        max_dist.text = maxDist
        avg_dist.text = avgDist
        total_dist.text = totalDist

        max_time.text = maxTime
        avg_time.text = avgTime
        total_time.text = totalTime
    }

    private fun getAndUpdateWeather() {
        println("first checkpoint")
        locationPermissionGranted = ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!locationPermissionGranted) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }
        println("second checkpoint")
        var fusedLocationProviderClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        val locationResult = fusedLocationProviderClient.lastLocation
        locationResult.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                println("third check point")
                val newLocation = task.result
                val newLocationString = newLocation?.latitude.toString() + "," + newLocation?.longitude.toString()
                val queue = Volley.newRequestQueue(this)
                val earth_id_url = "https://www.metaweather.com/api/location/search/?lattlong=${newLocationString}"


                // Request a string response from the provided URL.
                val stringRequest = StringRequest(
                    Request.Method.GET, earth_id_url,
                    Response.Listener<String> { response ->
                        val jsonRes = JSONArray(response.toString())
                        val locationName = jsonRes.getJSONObject(0).getString("title")
                        val locationId = jsonRes.getJSONObject(0).getInt("woeid")

                        location_label.text = locationName

                        val weather_url = "https://www.metaweather.com/api/location/${locationId}/"

                        val stringRequest2 = StringRequest(
                            Request.Method.GET, weather_url,
                            Response.Listener<String> { response2 ->
                                val jsonRes2 = JSONObject(response2.toString()).getJSONArray("consolidated_weather")
                                val predictionToday = jsonRes2.getJSONObject(0).getString("weather_state_name")
                                val predictionTomorrow = jsonRes2.getJSONObject(1).getString("weather_state_name")

                                val minTempToday = jsonRes2.getJSONObject(0).getString("min_temp").slice(0..4)
                                val minTempTomorrow = jsonRes2.getJSONObject(1).getString("min_temp").slice(0..4)

                                val maxTempToday = jsonRes2.getJSONObject(0).getString("max_temp").slice(0..4)
                                val maxTempTomorrow = jsonRes2.getJSONObject(1).getString("max_temp").slice(0..4)

                                val avgTempToday = jsonRes2.getJSONObject(0).getString("the_temp").slice(0..4)
                                val avgTempTomorrow = jsonRes2.getJSONObject(1).getString("the_temp").slice(0..4)

                                today_expect.text = predictionToday
                                tomorrow_expect.text = predictionTomorrow

                                today_temp.text = avgTempToday
                                tomorrow_temp.text = avgTempTomorrow

                                today_high.text = maxTempToday
                                tomorrow_high.text = maxTempTomorrow

                                today_low.text = minTempToday
                                tomorrow_low.text = minTempTomorrow

                            },
                            Response.ErrorListener {
                                println( "That didn't work!")
                            })
                        queue.add(stringRequest2)
                    },
                    Response.ErrorListener {
                        println( "That didn't work!")
                    })

                // Add the request to the RequestQueue.
                queue.add(stringRequest)
            } else {
                Log.e("Map Location Issue", "Exception: %s", task.exception)
            }
        }


    }

    // once request is provided
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // permission was granted, yay! Do the thang
                locationPermissionGranted = true
                getAndUpdateWeather()
            } else {
                Toast.makeText(this, "Sorry, the weather feature requires location enabled", Toast.LENGTH_SHORT).show()
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
            }

        }
    }

    private fun getTimeStringFromSeconds(seconds: Int): String {
        val hours: Int = seconds / 3600
        val minutes: Int = seconds% 3600 / 60
        val secs: Int = seconds % 60
        val timeElapsed = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
        return timeElapsed
    }

    // this gets run after you return from the camera
    public override fun onActivityResult(requestCode:Int, resultCode:Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Walk Cancelled!", Toast.LENGTH_SHORT).show()
        } else if (resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Walk Saved!", Toast.LENGTH_SHORT).show()
        }
    }
}