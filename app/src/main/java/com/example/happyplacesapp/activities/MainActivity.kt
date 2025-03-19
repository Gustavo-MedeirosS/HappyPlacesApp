package com.example.happyplacesapp.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.happyplacesapp.R
import com.example.happyplacesapp.adapters.HappyPlacesAdapter
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityMainBinding
import com.example.happyplacesapp.models.HappyPlaceModel

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddHappyPlace?.setOnClickListener {
            val intent = Intent(this, AddHappyPlaceActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }

        getHappyPlacesFromDatabase()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    private fun getHappyPlacesFromDatabase() {
        val dbHandler = DatabaseHandler(context = this)
        val getHappyPlaceList = dbHandler.getHappyPlacesList()

        if (getHappyPlaceList.isNotEmpty()) {
            Log.d("happy places list", "size: ${getHappyPlaceList.size}")
            binding?.tvNoRecordsAvailable?.visibility = View.GONE
            binding?.rvHappyPlacesList?.visibility = View.VISIBLE
            setupHappyPlacesRecyclerView(happyPlaces = getHappyPlaceList)
        } else {
            binding?.tvNoRecordsAvailable?.visibility = View.VISIBLE
            binding?.rvHappyPlacesList?.visibility = View.GONE
        }
    }

    private fun setupHappyPlacesRecyclerView(happyPlaces: ArrayList<HappyPlaceModel>) {
        binding?.rvHappyPlacesList?.layoutManager = LinearLayoutManager(this)
        binding?.rvHappyPlacesList?.setHasFixedSize(true)

        val adapter = HappyPlacesAdapter(this, happyPlaces)
        binding?.rvHappyPlacesList?.adapter = adapter

        adapter.setOnClickListener(onClickListener = object : HappyPlacesAdapter.OnClickListener {
            override fun onClick(position: Int, model: HappyPlaceModel) {
                val intent = Intent(
                    this@MainActivity,
                    HappyPlaceDetailActivity::class.java
                )

                intent.putExtra(EXTRA_PLACE_DETAILS, model)

                startActivity(intent)
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                getHappyPlacesFromDatabase()
            } else {
                Log.e("Activity", "Cancelled or back pressed")
            }
        }
    }

    companion object {
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        const val EXTRA_PLACE_DETAILS = "extra_place_details"
    }
}