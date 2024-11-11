package com.cmc.contactapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
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
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        contactAdapter = ContactAdapter(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = contactAdapter

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

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                contactAdapter.filter.filter(newText)
                return true
            }
        })
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
        val contactList = ArrayList<Contact>()
        val contentResolver = contentResolver
        val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneNumberIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                if (idIndex != -1 && nameIndex != -1 && hasPhoneNumberIndex != -1) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex)
                    if (cursor.getInt(hasPhoneNumberIndex) > 0) {
                        val pCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id), null
                        )

                        if (pCursor != null) {
                            val phoneNumberIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (phoneNumberIndex != -1) {
                                while (pCursor.moveToNext()) {
                                    val phoneNumber = pCursor.getString(phoneNumberIndex)
                                    contactList.add(Contact(id.toInt(), name, phoneNumber))
                                }
                            }
                            pCursor.close()
                        }
                    }
                }
            }
            cursor.close()
        }

        contactList.sortBy { it.name }

        contactAdapter.setContacts(contactList)
        updateEmptyState(contactList.isEmpty())
    }

    fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateImage.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyStateImage.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
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
