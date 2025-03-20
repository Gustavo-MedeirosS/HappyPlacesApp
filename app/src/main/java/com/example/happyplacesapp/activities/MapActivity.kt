package com.example.happyplacesapp.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplacesapp.R
import com.example.happyplacesapp.databinding.ActivityMapBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var binding: ActivityMapBinding? = null
    private var mHappyPlaceDetail: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetail =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if (mHappyPlaceDetail != null) {
            setSupportActionBar(binding?.toolbarMap)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = mHappyPlaceDetail!!.title

            binding?.toolbarMap?.setNavigationOnClickListener {
                onBackPressed()
            }

            val supportMapFragment: SupportMapFragment =
                supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }

//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val position = LatLng(mHappyPlaceDetail!!.latitude, mHappyPlaceDetail!!.longitude)
        googleMap.addMarker(MarkerOptions().position(position).title(mHappyPlaceDetail!!.location))

        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 11f)
        googleMap.animateCamera(newLatLngZoom)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}