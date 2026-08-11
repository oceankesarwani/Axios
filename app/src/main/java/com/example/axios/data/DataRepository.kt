package com.example.axios.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.axios.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL

object DataRepository {
    private const val PREFS_NAME = "axios_local_cache"
    private const val KEY_WINGS = "wings"
    private const val KEY_ANNOUNCEMENTS = "announcements"
    private const val KEY_MEMBERS = "members"
    private const val KEY_RESOURCES = "resources"

    private val db = FirebaseFirestore.getInstance()

    // ── Cloudinary config ──────────────────────────────────────────────────────
    // Values are injected at build time from local.properties (gitignored)
    private val CLOUDINARY_CLOUD_NAME get() = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val CLOUDINARY_UPLOAD_PRESET get() = BuildConfig.CLOUDINARY_UPLOAD_PRESET
    // ──────────────────────────────────────────────────────────────────────────

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
        val fileName: String = "",
        val downloadUrl: String = ""
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
                            fileName = obj.optString("fileName"),
                            downloadUrl = obj.optString("downloadUrl")
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error parsing local resources", e)
            }
        }
    }

    // Per-collection save helpers — each callback only persists its own data,
    // preventing a race condition where one collection's save overwrites another's
    // cached data before it has been fetched from Firestore.

    private fun saveWings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_WINGS, JSONArray(wings).toString()).apply()
    }

    private fun saveAnnouncements(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (a in announcements) {
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("wingName", a.wingName)
                put("message", a.message)
                put("info", a.info)
                put("timestamp", a.timestamp)
            })
        }
        prefs.edit().putString(KEY_ANNOUNCEMENTS, arr.toString()).apply()
    }

    private fun saveMembers(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (m in members) {
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("wingName", m.wingName)
                put("name", m.name)
                put("rollNo", m.rollNo)
                put("contactInfo", m.contactInfo)
                put("role", m.role)
            })
        }
        prefs.edit().putString(KEY_MEMBERS, arr.toString()).apply()
    }

    private fun saveResources(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (r in resources) {
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("wingName", r.wingName)
                put("fileName", r.fileName)
                put("downloadUrl", r.downloadUrl)
            })
        }
        prefs.edit().putString(KEY_RESOURCES, arr.toString()).apply()
    }

    private fun syncFromFirestore(context: Context, onComplete: () -> Unit) {
        // Fetch wings
        db.collection("wings").get().addOnSuccessListener { result ->
            val tempWings = mutableListOf<String>()
            for (doc in result) {
                doc.getString("name")?.let { tempWings.add(it) }
            }
            if (tempWings.isNotEmpty()) {
                wings = tempWings
                saveWings(context)  // only save wings — avoids overwriting other cached data
                onComplete()
            }
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
            // Always update — even an empty result clears stale deleted announcements
            announcements = tempAnn
            saveAnnouncements(context)  // only save announcements
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
            if (tempMem.isNotEmpty()) {
                members = tempMem
                saveMembers(context)  // only save members
                onComplete()
            }
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
                        fileName = doc.getString("fileName") ?: "",
                        downloadUrl = doc.getString("downloadUrl") ?: ""
                    )
                )
            }
            if (tempRes.isNotEmpty()) {
                resources = tempRes
                saveResources(context)  // only save resources
                onComplete()
            }
        }.addOnFailureListener { e ->
            Log.e("DataRepository", "Firestore resources fetch failed, using local fallback.", e)
        }
    }

    fun addWing(context: Context, wingName: String, onComplete: () -> Unit) {
        if (!wings.contains(wingName)) {
            wings.add(wingName)
            saveWings(context)
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
        saveAnnouncements(context)
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
        saveMembers(context)
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

    fun addResource(context: Context, wingName: String, fileName: String, downloadUrl: String, onComplete: () -> Unit) {
        val docRef = db.collection("resources").document()
        val res = Resource(
            id = docRef.id,
            wingName = wingName,
            fileName = fileName,
            downloadUrl = downloadUrl
        )
        resources.add(res)
        saveResources(context)
        onComplete()

        val data = mapOf(
            "wingName" to wingName,
            "fileName" to fileName,
            "downloadUrl" to downloadUrl
        )
        docRef.set(data)
            .addOnFailureListener { e -> Log.w("DataRepository", "Error writing resource to Firestore", e) }
    }

    fun uploadFile(
        context: Context,
        wingName: String,
        fileUri: Uri,
        fileName: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Thread {
            try {
                // Read file bytes from URI
                val inputStream = context.contentResolver.openInputStream(fileUri)
                    ?: throw Exception("Cannot open file")
                val bytes = inputStream.readBytes()
                inputStream.close()

                onProgress(10) // signal started

                val boundary = "----AxiosBoundary${System.currentTimeMillis()}"
                val uploadUrl = URL(
                    "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/auto/upload"
                )

                val conn = uploadUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.connectTimeout = 30_000
                conn.readTimeout = 60_000

                val os = conn.outputStream
                val writer = PrintWriter(os.writer(Charsets.UTF_8), true)

                // upload_preset field
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
                writer.append("$CLOUDINARY_UPLOAD_PRESET\r\n")
                writer.flush()

                // public_id field — organises files in Cloudinary by wing
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"public_id\"\r\n\r\n")
                writer.append("axios/resources/$wingName/$fileName\r\n")
                writer.flush()

                // file field
                writer.append("--$boundary\r\n")
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                writer.append("Content-Type: application/octet-stream\r\n\r\n")
                writer.flush()

                // Write file bytes in chunks and report progress
                val chunkSize = 8192
                var uploaded = 0
                var offset = 0
                while (offset < bytes.size) {
                    val end = minOf(offset + chunkSize, bytes.size)
                    os.write(bytes, offset, end - offset)
                    uploaded += end - offset
                    offset = end
                    val progress = 10 + (uploaded.toDouble() / bytes.size * 80).toInt()
                    onProgress(progress)
                }
                os.flush()

                writer.append("\r\n--$boundary--\r\n")
                writer.flush()

                onProgress(90)

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)
                    val downloadUrl = jsonResponse.getString("secure_url")

                    onProgress(100)
                    addResource(context, wingName, fileName, downloadUrl) {
                        onSuccess()
                    }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                    onFailure(Exception("Cloudinary upload failed: $error"))
                }

                conn.disconnect()
            } catch (e: Exception) {
                Log.e("DataRepository", "Cloudinary upload error", e)
                onFailure(e)
            }
        }.start()
    }

    fun deleteResource(context: Context, resource: Resource, onComplete: () -> Unit) {
        // Remove from local cache
        resources.remove(resource)
        saveResources(context)
        onComplete()

        // Delete Firestore document
        // Note: Cloudinary file deletion requires a signed request with API secret.
        // Do this from a backend/Cloud Function to avoid exposing credentials in the app.
        db.collection("resources").document(resource.id).delete()
            .addOnFailureListener { e -> Log.w("DataRepository", "Error deleting resource from Firestore", e) }
    }
}
