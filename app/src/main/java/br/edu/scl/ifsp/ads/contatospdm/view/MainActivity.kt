package br.edu.scl.ifsp.ads.contatospdm.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import br.edu.scl.ifsp.ads.contatospdm.R
import br.edu.scl.ifsp.ads.contatospdm.adapter.ContactAdapter
import br.edu.scl.ifsp.ads.contatospdm.controller.ContactController
import br.edu.scl.ifsp.ads.contatospdm.controller.ContactRoomController
import br.edu.scl.ifsp.ads.contatospdm.controller.ContactRtDbFBController
import br.edu.scl.ifsp.ads.contatospdm.databinding.ActivityMainBinding
import br.edu.scl.ifsp.ads.contatospdm.model.Constant.CONTACT_ARRAY
import br.edu.scl.ifsp.ads.contatospdm.model.Contact
import br.edu.scl.ifsp.ads.contatospdm.model.Constant.EXTRA_CONTACT
import br.edu.scl.ifsp.ads.contatospdm.model.Constant.VIEW_CONTACT

class MainActivity : AppCompatActivity() {
    private val amb: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    // Data source
    private val contactList: MutableList<Contact> = mutableListOf()

    // Controller
    private val contactController: ContactRtDbFBController by lazy {
        ContactRtDbFBController(this)
    }

    // Adapter
    private val contactAdapter: ContactAdapter by lazy {
        ContactAdapter(
            this,
            contactList
        )
    }

    companion object {
        const val GET_CONTACTS_MSG = 1
        const val GET_CONTACTS_INTERVAL = 2000L //milissegundos
    }

    //Handler
    val updateContactListHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message){
            super.handleMessage(msg)

            //Busca os contatos ou atualiza a lista de acordo com o tipo da mensagem
            if(msg.what == GET_CONTACTS_MSG){
                //Busca os contatos de acordo com o intervalo pre-definido e agenda nova busca
                contactController.getContacts()
                sendMessageDelayed(obtainMessage().apply { what = GET_CONTACTS_MSG }, GET_CONTACTS_INTERVAL)
            } else{
                msg.data.getParcelableArray(CONTACT_ARRAY)?.also { contactArray ->
                    contactList.clear()
                    contactArray.forEach {
                        contactList.add(it as Contact)
                    }
                    contactAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    // carl = ContactActivityResultLauncher
    private lateinit var carl: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(amb.root)

        setSupportActionBar(amb.toolbarIn.toolbar)

        //fillContacts()
        amb.contatosLv.adapter = contactAdapter

        carl = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val contact = result.data?.getParcelableExtra<Contact>(EXTRA_CONTACT)
                contact?.let { _contact ->
                    if (contactList.any { it.id == _contact.id }) {
                        // Edita
                        contactController.editContact(_contact)
                    }
                    else {
                        // Adiciona
                        contactController.insertContact(_contact)
                    }
                }
            }
        }

        amb.contatosLv.setOnItemClickListener{ parent, view, position, id ->
            val contact = contactList[position]
            val viewContactIntent = Intent(this, ContactActivity::class.java)
            viewContactIntent.putExtra(EXTRA_CONTACT, contact)
            viewContactIntent.putExtra(VIEW_CONTACT, true)
            startActivity(viewContactIntent)
        }

        registerForContextMenu(amb.contatosLv)
        updateContactListHandler.apply {
            sendMessageDelayed(
                obtainMessage().apply { what = GET_CONTACTS_MSG },
                GET_CONTACTS_INTERVAL
            )
        }

        contactController.getContacts()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.addContactMi -> {
                carl.launch(Intent(this, ContactActivity::class.java))
                true
            }
            else -> false
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menuInflater.inflate(R.menu.context_menu_main, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val position = (item.menuInfo as AdapterContextMenuInfo).position
        val contact = contactList[position]

        return when (item.itemId) {
            R.id.removeContactMi -> {
                contactController.removeContact(contact)
                Toast.makeText(this, "Contact removed.", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.editContactMi -> {
                val editContactIntent = Intent(this, ContactActivity::class.java)
                editContactIntent.putExtra(EXTRA_CONTACT, contact)
                carl.launch(editContactIntent)
                true
            }
            else -> { false }
        }
    }

    private fun fillContacts() {
        for (i in 1..50) {
            contactList.add(
                Contact(
                    i,
                    "Nome $i",
                    "Endereço $i",
                    "Telefone $i",
                    "Email $i"
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterForContextMenu(amb.contatosLv)
    }

    fun updateContactList(_contactList: MutableList<Contact>) {
        contactList.clear()
        contactList.addAll(_contactList)
        contactAdapter.notifyDataSetChanged()
    }
}