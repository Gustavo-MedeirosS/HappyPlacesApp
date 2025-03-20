package com.example.happyplacesapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.happyplacesapp.R
import com.example.happyplacesapp.database.DatabaseHandler
import com.example.happyplacesapp.databinding.ActivityAddHappyPlaceBinding
import com.example.happyplacesapp.models.HappyPlaceModel
import com.example.happyplacesapp.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityAddHappyPlaceBinding? = null

    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mHappyPlaceDetails: HappyPlaceModel? = null

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()) {
            Places.initialize(
                this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key)
            )
        }

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails =
                intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        updateDateInView()

        if (mHappyPlaceDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            binding?.etTitle?.setText(mHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(mHappyPlaceDetails!!.description)
            binding?.etDate?.setText(mHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(mHappyPlaceDetails!!.location)
            mLongitude = mHappyPlaceDetails!!.longitude
            mLatitude = mHappyPlaceDetails!!.latitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)
            binding?.btnSave?.text = "UPDATE"
        }

        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        binding?.btnSave?.setOnClickListener(this)
        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf(
                    "Select photo from gallery",
                    "Capture photo from camera"
                )
                pictureDialog.setItems(pictureDialogItems) { _, which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> Toast.makeText(
                            this,
                            "Camera coming soon...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                pictureDialog.show()
            }

            R.id.btn_save -> {
                when {
                    binding?.etTitle?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }

                    binding?.etDescription?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
                    }

                    binding?.etLocation?.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
                    }

                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
                    }

                    else -> {
                        addHappyPlace()
                    }
                }
            }

            R.id.et_location -> {
                try {
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
                    )

                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    startActivity(intent)
                } else {
                    Dexter.withContext(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ).withListener(object : MultiplePermissionsListener {
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()) {
                                requestNewLocation()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            permissions: MutableList<PermissionRequest>?,
                            token: PermissionToken?
                        ) {
                            showRationaleDialogForPermissions()
                        }
                    }).onSameThread().check()
                }
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                )
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.numUpdates = 1

        mFusedLocationClient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val mLastLocation: Location = result.lastLocation!!
            mLatitude = mLastLocation.latitude
            mLongitude = mLastLocation.longitude

            val addressTask = GetAddressFromLatLng(
                context = this@AddHappyPlaceActivity,
                latitude = mLatitude,
                longitude = mLongitude
            )

            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String) {
                    binding?.etLocation?.setText(address)
                }

                override fun onError() {
                    Log.e("Get Address", "Something went wrong")
                }
            })

            addressTask.getAddresses()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun choosePhotoFromGallery() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
    }

    private fun takePhotoFromCamera() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.CAMERA,
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE)
                    startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {
                showRationaleDialogForPermissions()
            }
        }).onSameThread().check()
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                if (data != null) {
                    val contentUri = data.data
                    try {
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                            this.contentResolver, contentUri
                        )
                        saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.d("Saved Image", "Path: ")
                        binding?.ivPlaceImage?.setImageBitmap(selectedImageBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Failed to load images from gallery",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                val thumbnail = data!!.extras!!["data"] as Bitmap?
                binding?.ivPlaceImage?.setImageBitmap(thumbnail)
            } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
                val place: Place = Autocomplete.getPlaceFromIntent(data)
                binding?.etLocation?.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            }
        }
    }

    private fun showRationaleDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permission required for this feature. It can be enable under the App Settings")
            .setPositiveButton("Go to settings", { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView() {
        val dateFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(dateFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(calendar.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return Uri.parse(file.absolutePath)
    }

    private fun addHappyPlace() {
        val happyPlaceModel = HappyPlaceModel(
            id = if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,
            title = binding?.etTitle?.text.toString(),
            image = saveImageToInternalStorage.toString(),
            description = binding?.etDescription?.text.toString(),
            date = binding?.etDate?.text.toString(),
            location = binding?.etLocation?.text.toString(),
            latitude = mLatitude,
            longitude = mLongitude
        )

        val dbhandler = DatabaseHandler(context = this)

        if (mHappyPlaceDetails == null) {
            val addHappyPlaceResult = dbhandler.addHappyPlace(happyPlace = happyPlaceModel)

            if (addHappyPlaceResult > 0) {
                setResult(Activity.RESULT_OK)
                finish()
            }

        } else {
            val updateHappyPlaceResult = dbhandler.updateHappyPlace(happyPlace = happyPlaceModel)

            if (updateHappyPlaceResult > 0) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        private const val GALLERY_REQUEST_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }
}
