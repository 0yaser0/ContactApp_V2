package com.cmc.contactapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.cmc.contactapp.databinding.ContactItemBinding
import java.util.Locale
import kotlin.random.Random

class ContactAdapter(private val context: MainActivity) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), Filterable {
    private var contacts: List<Contact> = listOf()
    var filteredContacts: List<Contact> = listOf()

    init {
        filteredContacts = contacts
    }

    inner class ContactViewHolder(val binding: ContactItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]
        holder.binding.contactName.text = contact.name
        holder.binding.contactPhoneNumber.text = contact.phoneNumber

        val firstChar = contact.name.firstOrNull()
        if (firstChar != null && firstChar.isLetter()) {
            holder.binding.circleImageView.text = firstChar.toString()
            val drawable = context.getDrawable(R.drawable.circle) as GradientDrawable
            drawable.setColor(getRandomColor())
            holder.binding.circleImageView.background = drawable
        } else {
            holder.binding.circleImageView.background = context.getDrawable(R.drawable.user)
            holder.binding.circleImageView.text = ""
        }

        holder.binding.optionsMenu.setOnClickListener {
            showPopupMenu(holder.binding.optionsMenu, contact.phoneNumber)
        }
    }

    override fun getItemCount() = filteredContacts.size

    fun setContacts(contacts: List<Contact>) {
        val allContacts = contacts.distinctBy { it.name }
        this.contacts = allContacts
        this.filteredContacts = allContacts
        notifyDataSetChanged()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence?): FilterResults {
                val query = charSequence?.toString()?.toLowerCase(Locale.ROOT) ?: ""
                val filterResults = FilterResults()
                filterResults.values = if (query.isEmpty()) {
                    contacts
                } else {
                    contacts.filter {
                        it.name.toLowerCase(java.util.Locale.ROOT).contains(query) ||
                                it.phoneNumber.contains(query)
                    }
                }
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
                filteredContacts = filterResults?.values as List<Contact>
                notifyDataSetChanged()
                context.updateEmptyState(filteredContacts.isEmpty())
            }
        }
    }

    private fun showPopupMenu(view: View, phoneNumber: String) {
        val popup = PopupMenu(context, view)
        popup.inflate(R.menu.menu_item)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.call -> context.makeCall(phoneNumber)
                R.id.sms -> {
                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phoneNumber")
                    }
                    context.startActivity(smsIntent)
                }
            }
            true
        }
        popup.show()
    }

    private fun getRandomColor(): Int {
        val random = Random.Default
        val baseColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        return baseColor
    }
}
