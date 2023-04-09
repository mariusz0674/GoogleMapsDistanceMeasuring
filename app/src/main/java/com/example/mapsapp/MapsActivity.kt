package com.example.mapsapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy.request
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.DragAndDropPermissionsCompat.request

import com.example.mapsapp.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener {

    private lateinit var binding: ActivityMapsBinding

    private val MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    var gpsMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    val markerList: ArrayList<Marker> = ArrayList()
    var totalDistance = 0f

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }

        binding.distanceInfo.text =
            String.format(getString(R.string.total_distance_format), totalDistance)
        binding.clearButton.setOnClickListener {
            // remove any existing markers in the list
            markerList.clear()
            // remove any existing markers on the map
            mMap.clear()
            // Reset totalDistance
            totalDistance = 0f
            // Update the UI
            Snackbar.make(binding.root, "Map cleared", Snackbar.LENGTH_LONG).show()
            showLastLocationMarker()
            mMap.moveCamera(CameraUpdateFactory.zoomTo(5f))
            binding.distanceInfo.text =
                String.format(getString(R.string.total_distance_format), totalDistance)
        }

    }

    override fun onResume() {
        super.onResume()
        Log.i(localClassName, "onResume")
        createLocationRequest()
        createLocationCallback()
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // if location permission is not granted don't start location updates
            return
        }
        startLocationUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.normalMap -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.hybridMap -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.satelliteMap -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.terrainMap -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
        }
        return super.onOptionsItemSelected(item)
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
        // Add a marker in Sydney and move the camera

        mMap = googleMap
        mMap.getUiSettings().setMapToolbarEnabled(false)
        mMap.setOnMapLoadedCallback(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapLongClickListener(this)


    }

    fun zoomInClick(view: View) {
        // Zoom in the map by1
        mMap.moveCamera(CameraUpdateFactory.zoomIn())
    }

    fun zoomOutClick(view: View) {
        // Zoom out the map by1
        mMap.moveCamera(CameraUpdateFactory.zoomOut())
    }

    fun moveToMyLocation(view: View) {
        gpsMarker?.let { marker ->
            // if gpsMarker isn't null,move the camera to gpsMarker position and set the zoom to 12
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 12f))
        }
    }

    override fun onMapLoaded() {
        Log.i(localClassName, "Map Loaded")
        showLastLocationMarker()
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        // Zoom the map on the marker
        if (mMap.cameraPosition.zoom < 14f) {
            mMap.moveCamera(CameraUpdateFactory.zoomTo(14f))
        }
        return false
    }

    override fun onMapLongClick(latLng: LatLng) {
        var distance = 0f
        if (markerList.size > 0) {
            // If the markerList is not empty,calculate the distance between last marker
            // and the current long click position
            val lastMarker = markerList.get(markerList.size - 1)
            val result = FloatArray(3)
            // Calculate distance between two points
            Location.distanceBetween(
                lastMarker.position.latitude,
                lastMarker.position.longitude,
                latLng.latitude,
                latLng.longitude,
                result
            )
            distance = result[0]
            // Update totalDistance
            totalDistance += distance / 1000f
            // Createablue line between these points
            val rectOptions = with(PolylineOptions()) {
                add(lastMarker.position)
                add(latLng)
                width(10f)
                color(Color.BLUE)
            }
            mMap.addPolyline(rectOptions)
        }
// Addanew custom marker at the position of long click
        val marker = mMap.addMarker(with(MarkerOptions()) {
            position(LatLng(latLng.latitude, latLng.longitude))
            icon(BitmapDescriptorFactory.fromResource(R.drawable.marker2))
            alpha(0.8f)
            title(
                String.format(
                    getString(R.string.marker_info_format),
                    latLng.latitude,
                    latLng.longitude,
                    distance
                )
            )
        })
        markerList.add(marker!!)
// Update the total distance info box
        binding.distanceInfo.text =
            String.format(getString(R.string.total_distance_format), totalDistance)


    }



    private fun createLocationRequest(){
        mLocationRequest = LocationRequest.create().apply{
            interval=10000
            fastestInterval=5000
            priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }
    private fun createLocationCallback(){
        Log.i(localClassName, "createCallback")
        locationCallback = object: LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult){
            // remove the old marker
            gpsMarker?.remove()
            // add the new marker
            gpsMarker=mMap.addMarker(with(MarkerOptions()){
                position(LatLng(locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
                ))
                icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                alpha(0.8f)
                title(getString(R.string.current_loc_msg))
            })}
        }
    }
    @SuppressLint("Missing Permission", "MissingPermission")
    private fun startLocationUpdates(){
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, mainLooper)
    }

    override fun onRequestPermissionsResult(
    requestCode:Int,
    permissions:Array<out String>,
    grantResults:IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check if the request code matches the MY PERMISSION REQUEST_ACCESS_FINE_LOCATION variable
        if (requestCode == MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION) {
            // check whether there existsalocation ACCESS_FINE_LOCATION permission in the permissions list
            val indexOf = permissions.indexOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            if (indexOf != -1 && grantResults[indexOf] != PackageManager.PERMISSION_GRANTED) {
                // permission was denied notify the user that it is required
                Snackbar.make(
                    binding.root,
                    "Permission is required to continue",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("RETRY") {// Retry permission request
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION
                        )
                    }.show()


            }
        }
    }


    @SuppressLint("Missing Permission", "MissingPermission")
    private fun showLastLocationMarker(){
        fusedLocationClient.lastLocation.addOnSuccessListener{location: Location?->
            location?.let {
                // if the location isn't null addamarker to map.Marker parameters are defined
                // with the help of MarkerOptions
                mMap.addMarker(with(MarkerOptions()) {
                    // set the position of the marker-"it"corresponds to the location
                    position(LatLng(it.latitude, it.longitude))
                    // set the icon for the marker default marker with azure hue
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    // setatitle for the marker
                    title(getString(R.string.last_known_loc_msg))
                }
                )
            }
        }
    }

    override fun onPause(){
        Log.i(localClassName,"onPause")
        super.onPause()
        stopLocationUpdates()
    }
    private fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    }

