package com.example.finalproject

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_new_walk.*
import kotlinx.android.synthetic.main.walk_post_layout.view.*
import org.w3c.dom.Text
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil

class WalkPostAdapter(var activity: Activity, var postList: ArrayList<WalkLogItem>) : RecyclerView.Adapter<WalkPostViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkPostViewHolder =
        WalkPostViewHolder(
            LayoutInflater.from(activity).inflate(
                R.layout.walk_post_layout, parent, false))

    // binds the individual post info to the viewholder
    override fun onBindViewHolder(holder: WalkPostViewHolder, position: Int) =
        holder.bind(postList[position], position, this, postList)

    override fun getItemCount() = postList.size
}

class WalkPostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var description: TextView = view.walk_post_description
    var date: TextView = view.walk_post_date
    var distance: TextView = view.walk_post_distance
    var time: TextView = view.walk_post_time
    var image: ImageView = view.walk_post_image
    var walkPostDetailsbar = view.wak_post_details_bar
    private val STATIC_MAPS_API_KEY = "AIzaSyBWkbf76BjlJxTqvUL7bSmoZFBR2pC_2LA"

    // for binding leaderboard info to view
    fun bind(item: WalkLogItem, position: Int, adapter: WalkPostAdapter, walkPostList: ArrayList<WalkLogItem>) {

        // creating space in the end for scrolling if it is the last element being added
        if (position == (walkPostList.size - 1)) {
            var params = (walkPostDetailsbar.layoutParams as ViewGroup.MarginLayoutParams)
            params.updateMargins(bottom = 100)
        }

        description.text = item.description
        date.text = item.date.slice(0..3) + "-" + item.date.slice(4..5) + "-" +
                item.date.slice(6..7)
        distance.text = String.format("%.2f", (item.jog_length * 0.000621371)) + " miles"

        val hours: Int = item.jog_time_seconds / 3600
        val minutes: Int = item.jog_time_seconds % 3600 / 60
        val secs: Int = item.jog_time_seconds % 60
        val timeElapsed = String.format(Locale.getDefault(), "Time: %d:%02d:%02d", hours, minutes, secs)
        time.text = timeElapsed

        val zoomLevel = 15
        val centerLocation = item.trip_log[(item.trip_log.size / 2)]
        val size = "400x400"
        // what markers tempate looks like: "&markers=color:blue%7Clabel:S%7C62.107733,-145.541936"
        var markerStrings = ""
        var pointNumber = 0
        var pathString = "&path=color:0x0000ff%7Cweight:3"
        item.trip_log.forEach { location ->
            var markerLabel = pointNumber.toString()
            var color = "blue"
            if (pointNumber == 0) {
                markerLabel = "S"
                color = "red"
            }
            else if (pointNumber == item.trip_log.size - 1) {
                markerLabel = "F"
                color = "green"
            }
            markerStrings += "&markers=color:${color}%7Clabel:${markerLabel}%7C${location}"
            pathString += "%7C${location}"
            pointNumber++
        }

        val urlString = "https://maps.googleapis.com/maps/api/staticmap?center=${centerLocation}&zoom=${zoomLevel}&size=${size}&maptype=terrain${markerStrings}${pathString}&key=${STATIC_MAPS_API_KEY}"

        Picasso.get()
            .load(urlString) // load the image
            .into(image)
    }
}