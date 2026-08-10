package com.example.axios

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject

object DataRepository {
    private const val PREFS_NAME = "axios_local_cache"
    private const val KEY_WINGS = "wings"
    private const val KEY_ANNOUNCEMENTS = "announcements"
    private const val KEY_MEMBERS = "members"
    private const val KEY_RESOURCES = "resources"

    private val db = FirebaseFirestore.getInstance()

    var wings = mutableListOf<String>()
    var announcements = mutableListOf<Announcement>()
    var members = mutableListOf<Member>()
    var resources = mutableListOf<Resource>()

    fun clearCache(context: Context) {
        wings.clear()
        announcements.clear()
        members.clear()
        resources.clear()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    data class Announcement(
        val id: String = "",
        val wingName: String = "",
        val message: String = "",
        val info: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    data class Member(
        val id: String = "",
        val wingName: String = "",
        val name: String = "",
        val rollNo: String = "",
        val contactInfo: String = "",
        val role: String = "" // "Coordinator", "Senior Member", "Member"
    )

    data class Resource(
        val id: String = "",
        val wingName: String = "",
        val fileName: String = ""
    )

    fun init(context: Context, onComplete: () -> Unit) {
        // Load local cache first
        loadLocalData(context)

        onComplete()

        // Sync with Firestore in background
        syncFromFirestore(context, onComplete)
    }

    private fun loadLocalData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Wings
        prefs.getString(KEY_WINGS, null)?.let { json ->
            try {
                wings.clear()
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    wings.add(arr.getString(i))
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error parsing local wings", e)
            }
        }

        // Announcements
        prefs.getString(KEY_ANNOUNCEMENTS, null)?.let { json ->
            try {
                announcements.clear()
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    announcements.add(
                        Announcement(
                            id = obj.optString("id"),
                            wingName = obj.optString("wingName"),
                            message = obj.optString("message"),
                            info = obj.optString("info"),
                            timestamp = obj.optLong("timestamp")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error parsing local announcements", e)
            }
        }

        // Members
        prefs.getString(KEY_MEMBERS, null)?.let { json ->
            try {
                members.clear()
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    members.add(
                        Member(
                            id = obj.optString("id"),
                            wingName = obj.optString("wingName"),
                            name = obj.optString("name"),
                            rollNo = obj.optString("rollNo"),
                            contactInfo = obj.optString("contactInfo"),
                            role = obj.optString("role")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error parsing local members", e)
            }
        }

        // Resources
        prefs.getString(KEY_RESOURCES, null)?.let { json ->
            try {
                resources.clear()
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    resources.add(
                        Resource(
                            id = obj.optString("id"),
                            wingName = obj.optString("wingName"),
                            fileName = obj.optString("fileName")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error parsing local resources", e)
            }
        }
    }

    private fun saveLocalData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Wings
        val wingsArr = JSONArray(wings)
        editor.putString(KEY_WINGS, wingsArr.toString())

        // Announcements
        val annArr = JSONArray()
        for (a in announcements) {
            val obj = JSONObject().apply {
                put("id", a.id)
                put("wingName", a.wingName)
                put("message", a.message)
                put("info", a.info)
                put("timestamp", a.timestamp)
            }
            annArr.put(obj)
        }
        editor.putString(KEY_ANNOUNCEMENTS, annArr.toString())

        // Members
        val memArr = JSONArray()
        for (m in members) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("wingName", m.wingName)
                put("name", m.name)
                put("rollNo", m.rollNo)
                put("contactInfo", m.contactInfo)
                put("role", m.role)
            }
            memArr.put(obj)
        }
        editor.putString(KEY_MEMBERS, memArr.toString())

        // Resources
        val resArr = JSONArray()
        for (r in resources) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("wingName", r.wingName)
                put("fileName", r.fileName)
            }
            resArr.put(obj)
        }
        editor.putString(KEY_RESOURCES, resArr.toString())

        editor.apply()
    }

    private fun syncFromFirestore(context: Context, onComplete: () -> Unit) {
        // Fetch wings
        db.collection("wings").get().addOnSuccessListener { result ->
            val tempWings = mutableListOf<String>()
            for (doc in result) {
                doc.getString("name")?.let { tempWings.add(it) }
            }
            wings = tempWings
            saveLocalData(context)
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("DataRepository", "Firestore wings fetch failed, using local fallback.", e)
        }

        // Fetch announcements
        db.collection("announcements").get().addOnSuccessListener { result ->
            val tempAnn = mutableListOf<Announcement>()
            for (doc in result) {
                tempAnn.add(
                    Announcement(
                        id = doc.id,
                        wingName = doc.getString("wingName") ?: "",
                        message = doc.getString("message") ?: "",
                        info = doc.getString("info") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                )
            }
            announcements = tempAnn
            saveLocalData(context)
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("DataRepository", "Firestore announcements fetch failed, using local fallback.", e)
        }

        // Fetch members
        db.collection("members").get().addOnSuccessListener { result ->
            val tempMem = mutableListOf<Member>()
            for (doc in result) {
                tempMem.add(
                    Member(
                        id = doc.id,
                        wingName = doc.getString("wingName") ?: "",
                        name = doc.getString("name") ?: "",
                        rollNo = doc.getString("rollNo") ?: "",
                        contactInfo = doc.getString("contactInfo") ?: "",
                        role = doc.getString("role") ?: "Member"
                    )
                )
            }
            members = tempMem
            saveLocalData(context)
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("DataRepository", "Firestore members fetch failed, using local fallback.", e)
        }

        // Fetch resources
        db.collection("resources").get().addOnSuccessListener { result ->
            val tempRes = mutableListOf<Resource>()
            for (doc in result) {
                tempRes.add(
                    Resource(
                        id = doc.id,
                        wingName = doc.getString("wingName") ?: "",
                        fileName = doc.getString("fileName") ?: ""
                    )
                )
            }
            resources = tempRes
            saveLocalData(context)
            onComplete()
        }.addOnFailureListener { e ->
            Log.e("DataRepository", "Firestore resources fetch failed, using local fallback.", e)
        }
    }

    fun addWing(context: Context, wingName: String, onComplete: () -> Unit) {
        if (!wings.contains(wingName)) {
            wings.add(wingName)
            saveLocalData(context)
            onComplete()

            // Save to Firestore
            val data = mapOf("name" to wingName)
            db.collection("wings").document(wingName).set(data)
                .addOnFailureListener { e -> Log.w("DataRepository", "Error writing wing to Firestore", e) }
        }
    }

    fun addAnnouncement(context: Context, wingName: String, message: String, info: String, onComplete: () -> Unit) {
        val docRef = db.collection("announcements").document()
        val ann = Announcement(
            id = docRef.id,
            wingName = wingName,
            message = message,
            info = info,
            timestamp = System.currentTimeMillis()
        )
        announcements.add(ann)
        saveLocalData(context)
        onComplete()

        val data = mapOf(
            "wingName" to wingName,
            "message" to message,
            "info" to info,
            "timestamp" to ann.timestamp
        )
        docRef.set(data)
            .addOnFailureListener { e -> Log.w("DataRepository", "Error writing announcement to Firestore", e) }
    }

    fun addMember(context: Context, wingName: String, name: String, rollNo: String, contactInfo: String, role: String, onComplete: () -> Unit) {
        val docRef = db.collection("members").document()
        val mem = Member(
            id = docRef.id,
            wingName = wingName,
            name = name,
            rollNo = rollNo,
            contactInfo = contactInfo,
            role = role
        )
        members.add(mem)
        saveLocalData(context)
        onComplete()

        val data = mapOf(
            "wingName" to wingName,
            "name" to name,
            "rollNo" to rollNo,
            "contactInfo" to contactInfo,
            "role" to role
        )
        docRef.set(data)
            .addOnFailureListener { e -> Log.w("DataRepository", "Error writing member to Firestore", e) }
    }

    fun addResource(context: Context, wingName: String, fileName: String, onComplete: () -> Unit) {
        val docRef = db.collection("resources").document()
        val res = Resource(
            id = docRef.id,
            wingName = wingName,
            fileName = fileName
        )
        resources.add(res)
        saveLocalData(context)
        onComplete()

        val data = mapOf(
            "wingName" to wingName,
            "fileName" to fileName
        )
        docRef.set(data)
            .addOnFailureListener { e -> Log.w("DataRepository", "Error writing resource to Firestore", e) }
    }
}
