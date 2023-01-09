package com.example.firestoretut

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.firestoretut.databinding.ActivityMainBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val personCollectionRef = Firebase.firestore.collection("Persons")

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnUploadData.setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
        }

//        subscribeToRealtimeUpdates()

        binding.btnRetrieveData.setOnClickListener {
            retrievePersons()
        }

        binding.btnDeletePerson.setOnClickListener {
            val person = getOldPerson()
            deletePerson(person)
        }

        binding.btnUpdatePerson.setOnClickListener {
            val oldPerson = getOldPerson()
            val newPerson = getNewPersonMap()
            updatePerson(oldPerson, newPerson)
        }

    }

    private fun deletePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()

        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
//                    personCollectionRef.document(document.id).delete().await()
                    personCollectionRef.document(document.id).update(mapOf(
                        "firstname" to FieldValue.delete()
                    ))
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getOldPerson(): Person{
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val age = binding.etAge.text.toString().toInt()
        return Person(firstName, lastName, age)
    }

    private fun getNewPersonMap(): Map<String, Any>{
        val firstName = binding.etNewFirstName.text.toString()
        val lastName = binding.etNewLastName.text.toString()
        val age = binding.etNewAge.text.toString()
        val map = mutableMapOf<String, Any>()
        if(firstName.isNotEmpty()) {
            map["firstName"] = firstName
        }
        if(lastName.isNotEmpty()) {
            map["lastName"] = lastName
        }
        if(age.isNotEmpty()) {
            map["age"] = age.toInt()
        }
        return map
    }

    private fun updatePerson(person: Person, newPersonMap: Map<String, Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef
            .whereEqualTo("firstName", person.firstName)
            .whereEqualTo("lastName", person.lastName)
            .whereEqualTo("age", person.age)
            .get()
            .await()
        if(personQuery.documents.isNotEmpty()) {
            for(document in personQuery) {
                try {
                    //personCollectionRef.document(document.id).update("age", newAge).await()
                    personCollectionRef.document(document.id).set(
                        newPersonMap,
                        SetOptions.merge()
                    ).await()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "No persons matched the query.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun subscribeToRealtimeUpdates(){
        personCollectionRef.addSnapshotListener { querySnapshot, firestoreException ->
            firestoreException?.let {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            querySnapshot?.let {
                val sb = StringBuilder()
                for (document in it){
                    val person = document.toObject<Person>()
                    sb.append("$person\n")
                    binding.tvPersons.text = sb.toString()
                }
            }
        }
    }

    private fun retrievePersons() = CoroutineScope(Dispatchers.IO).launch{

        val fromAge = binding.etFrom.text.toString().toInt()
        val toAge = binding.etTo.text.toString().toInt()

        try {
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()

            val sb = StringBuilder()
            for (document in querySnapshot.documents){
                val person = document.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main){
                binding.tvPersons.text = sb.toString()

            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error ${e.message} occurred", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun savePerson(person: Person) = CoroutineScope(Dispatchers.IO).launch {
        try {
            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "successfully saved", Toast.LENGTH_SHORT).show()
            }
        }catch (e: Exception){
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}