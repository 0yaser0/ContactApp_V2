package com.cmc.contactapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.cmc.contactapp.databinding.ActivityMainBinding
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val PERMISSIONS_REQUEST_CALL_PHONE = 124
    private lateinit var binding: ActivityMainBinding
    private lateinit var contactAdapter: ContactAdapter
    private var currentPhoneNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initializeRecyclerView()

        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_CONTACTS)) {
            loadContacts()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs access to your contacts to display them.",
                123,
                Manifest.permission.READ_CONTACTS
            )
        }
    }

    private fun setupUI() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeRecyclerView() {
        contactAdapter = ContactAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = contactAdapter
    }

    fun makeCall(phoneNumber: String) {
        currentPhoneNumber = phoneNumber
        if (EasyPermissions.hasPermissions(this, Manifest.permission.CALL_PHONE)) {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            startActivity(callIntent)
        } else {
            EasyPermissions.requestPermissions(
                this,
                "This app needs access to your phone to make calls.",
                PERMISSIONS_REQUEST_CALL_PHONE,
                Manifest.permission.CALL_PHONE
            )
        }
    }

    private fun loadContacts() {
        val contactList = mutableListOf<Contact>()
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

        cursor?.use {
            if (it.count > 0) {
                while (it.moveToNext()) {
                    val id = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                    if (it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        val pCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        pCursor?.use { phoneCursor ->
                            val phoneNumberIndex = phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            while (phoneCursor.moveToNext()) {
                                val phoneNumber = phoneCursor.getString(phoneNumberIndex)
                                contactList.add(Contact(id.toInt(), name, phoneNumber))
                            }
                        }
                    }
                }
            }
        }
        contactAdapter.setContacts(contactList)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        when (requestCode) {
            123 -> loadContacts()
            PERMISSIONS_REQUEST_CALL_PHONE -> currentPhoneNumber?.let { makeCall(it) }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }
}
