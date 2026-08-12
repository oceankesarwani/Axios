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
    private val CLOUDINARY_CLOUD_NAME get() = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val CLOUDINARY_UPLOAD_PRESET get() = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    var wings = mutableListOf<String>()
    var announcements = mutableListOf<Announcement>()
    var members = mutableListOf<Member>()
    var resources = mutableListOf<Resource>()

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
        val role: String = ""
    )

    data class Resource(
        val id: String = "",
        val wingName: String = "",
        val fileName: String = "",
        val downloadUrl: String = ""
    )

    fun clearCache(context: Context) {
        wings.clear(); announcements.clear(); members.clear(); resources.clear()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun init(context: Context, onComplete: () -> Unit) {
        loadLocalData(context)
        onComplete()
        syncFromFirestore(context, onComplete)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadLocalData(context: Context) {
        val p = prefs(context)

        p.getString(KEY_WINGS, null)?.let { json ->
            runCatching {
                wings.clear()
                val arr = JSONArray(json)
                repeat(arr.length()) { wings.add(arr.getString(it)) }
            }.onFailure { Log.e("DataRepository", "Error parsing local wings", it) }
        }

        p.getString(KEY_ANNOUNCEMENTS, null)?.let { json ->
            runCatching {
                announcements.clear()
                val arr = JSONArray(json)
                repeat(arr.length()) {
                    val o = arr.getJSONObject(it)
                    announcements.add(Announcement(o.optString("id"), o.optString("wingName"),
                        o.optString("message"), o.optString("info"), o.optLong("timestamp")))
                }
            }.onFailure { Log.e("DataRepository", "Error parsing local announcements", it) }
        }

        p.getString(KEY_MEMBERS, null)?.let { json ->
            runCatching {
                members.clear()
                val arr = JSONArray(json)
                repeat(arr.length()) {
                    val o = arr.getJSONObject(it)
                    members.add(Member(o.optString("id"), o.optString("wingName"),
                        o.optString("name"), o.optString("rollNo"),
                        o.optString("contactInfo"), o.optString("role")))
                }
            }.onFailure { Log.e("DataRepository", "Error parsing local members", it) }
        }

        p.getString(KEY_RESOURCES, null)?.let { json ->
            runCatching {
                resources.clear()
                val arr = JSONArray(json)
                repeat(arr.length()) {
                    val o = arr.getJSONObject(it)
                    resources.add(Resource(o.optString("id"), o.optString("wingName"),
                        o.optString("fileName"), o.optString("downloadUrl")))
                }
            }.onFailure { Log.e("DataRepository", "Error parsing local resources", it) }
        }
    }

    private fun saveWings(context: Context) =
        prefs(context).edit().putString(KEY_WINGS, JSONArray(wings).toString()).apply()

    private fun saveAnnouncements(context: Context) {
        val arr = JSONArray()
        announcements.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id); put("wingName", a.wingName); put("message", a.message)
                put("info", a.info); put("timestamp", a.timestamp)
            })
        }
        prefs(context).edit().putString(KEY_ANNOUNCEMENTS, arr.toString()).apply()
    }

    private fun saveMembers(context: Context) {
        val arr = JSONArray()
        members.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("wingName", m.wingName); put("name", m.name)
                put("rollNo", m.rollNo); put("contactInfo", m.contactInfo); put("role", m.role)
            })
        }
        prefs(context).edit().putString(KEY_MEMBERS, arr.toString()).apply()
    }

    private fun saveResources(context: Context) {
        val arr = JSONArray()
        resources.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("wingName", r.wingName)
                put("fileName", r.fileName); put("downloadUrl", r.downloadUrl)
            })
        }
        prefs(context).edit().putString(KEY_RESOURCES, arr.toString()).apply()
    }

    private fun syncFromFirestore(context: Context, onComplete: () -> Unit) {
        db.collection("wings").get().addOnSuccessListener { result ->
            val temp = result.mapNotNull { it.getString("name") }.toMutableList()
            if (temp.isNotEmpty()) { wings = temp; saveWings(context); onComplete() }
        }.addOnFailureListener { Log.e("DataRepository", "Wings fetch failed", it) }

        db.collection("announcements").get().addOnSuccessListener { result ->
            announcements = result.map { doc ->
                Announcement(doc.id, doc.getString("wingName") ?: "",
                    doc.getString("message") ?: "", doc.getString("info") ?: "",
                    doc.getLong("timestamp") ?: System.currentTimeMillis())
            }.toMutableList()
            saveAnnouncements(context); onComplete()
        }.addOnFailureListener { Log.e("DataRepository", "Announcements fetch failed", it) }

        db.collection("members").get().addOnSuccessListener { result ->
            val temp = result.map { doc ->
                Member(doc.id, doc.getString("wingName") ?: "", doc.getString("name") ?: "",
                    doc.getString("rollNo") ?: "", doc.getString("contactInfo") ?: "",
                    doc.getString("role") ?: "Member")
            }.toMutableList()
            if (temp.isNotEmpty()) { members = temp; saveMembers(context); onComplete() }
        }.addOnFailureListener { Log.e("DataRepository", "Members fetch failed", it) }

        db.collection("resources").get().addOnSuccessListener { result ->
            val temp = result.map { doc ->
                Resource(doc.id, doc.getString("wingName") ?: "",
                    doc.getString("fileName") ?: "", doc.getString("downloadUrl") ?: "")
            }.toMutableList()
            if (temp.isNotEmpty()) { resources = temp; saveResources(context); onComplete() }
        }.addOnFailureListener { Log.e("DataRepository", "Resources fetch failed", it) }
    }

    fun addWing(context: Context, wingName: String, onComplete: () -> Unit) {
        if (wings.contains(wingName)) return
        wings.add(wingName); saveWings(context); onComplete()
        db.collection("wings").document(wingName).set(mapOf("name" to wingName))
            .addOnFailureListener { Log.w("DataRepository", "Error writing wing", it) }
    }

    fun addAnnouncement(context: Context, wingName: String, message: String, info: String, onComplete: () -> Unit) {
        val docRef = db.collection("announcements").document()
        val ann = Announcement(docRef.id, wingName, message, info, System.currentTimeMillis())
        announcements.add(ann); saveAnnouncements(context); onComplete()
        docRef.set(mapOf("wingName" to wingName, "message" to message, "info" to info, "timestamp" to ann.timestamp))
            .addOnFailureListener { Log.w("DataRepository", "Error writing announcement", it) }
    }

    fun addMember(context: Context, wingName: String, name: String, rollNo: String, contactInfo: String, role: String, onComplete: () -> Unit) {
        val docRef = db.collection("members").document()
        members.add(Member(docRef.id, wingName, name, rollNo, contactInfo, role))
        saveMembers(context); onComplete()
        docRef.set(mapOf("wingName" to wingName, "name" to name, "rollNo" to rollNo, "contactInfo" to contactInfo, "role" to role))
            .addOnFailureListener { Log.w("DataRepository", "Error writing member", it) }
    }

    fun addResource(context: Context, wingName: String, fileName: String, downloadUrl: String, onComplete: () -> Unit) {
        val docRef = db.collection("resources").document()
        resources.add(Resource(docRef.id, wingName, fileName, downloadUrl))
        saveResources(context); onComplete()
        docRef.set(mapOf("wingName" to wingName, "fileName" to fileName, "downloadUrl" to downloadUrl))
            .addOnFailureListener { Log.w("DataRepository", "Error writing resource", it) }
    }

    fun uploadFile(
        context: Context, wingName: String, fileUri: Uri, fileName: String,
        onProgress: (Int) -> Unit, onSuccess: () -> Unit, onFailure: (Exception) -> Unit
    ) {
        Thread {
            try {
                val bytes = context.contentResolver.openInputStream(fileUri)?.use { it.readBytes() }
                    ?: throw Exception("Cannot open file")

                val isImage = fileName.endsWith(".png", true) || fileName.endsWith(".jpg", true) ||
                              fileName.endsWith(".jpeg", true) || fileName.endsWith(".webp", true) ||
                              fileName.endsWith(".gif", true)
                val isPdf = fileName.endsWith(".pdf", true)
                val resourceType = if (isImage || isPdf) "image" else "raw"
                val publicId = if (isImage || isPdf) {
                    val nameNoExt = fileName.substringBeforeLast('.', fileName)
                    "axios/resources/$wingName/$nameNoExt"
                } else "axios/resources/$wingName/$fileName"

                onProgress(10)

                val boundary = "----AxiosBoundary${System.currentTimeMillis()}"
                val conn = URL("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/$resourceType/upload")
                    .openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"; doOutput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    connectTimeout = 30_000; readTimeout = 60_000
                }

                val os = conn.outputStream
                val writer = PrintWriter(os.writer(Charsets.UTF_8), true)

                fun field(name: String, value: String) {
                    writer.append("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n")
                    writer.flush()
                }
                field("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                field("public_id", publicId)

                writer.append("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                writer.flush()

                val chunkSize = 8192
                var offset = 0
                while (offset < bytes.size) {
                    val end = minOf(offset + chunkSize, bytes.size)
                    os.write(bytes, offset, end - offset)
                    offset = end
                    onProgress(10 + (offset.toDouble() / bytes.size * 80).toInt())
                }
                os.flush()
                writer.append("\r\n--$boundary--\r\n"); writer.flush()
                onProgress(90)

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val url = JSONObject(conn.inputStream.bufferedReader().readText()).getString("secure_url")
                    onProgress(100)
                    addResource(context, wingName, fileName, url) { onSuccess() }
                } else {
                    val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP ${conn.responseCode}"
                    onFailure(Exception("Cloudinary upload failed: $error"))
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("DataRepository", "Upload error", e)
                onFailure(e)
            }
        }.start()
    }

    fun deleteResource(context: Context, resource: Resource, onComplete: () -> Unit) {
        resources.remove(resource); saveResources(context); onComplete()
        db.collection("resources").document(resource.id).delete()
            .addOnFailureListener { Log.w("DataRepository", "Error deleting resource", it) }
    }

    fun deleteAnnouncement(context: Context, announcement: Announcement, onComplete: () -> Unit) {
        announcements.remove(announcement); saveAnnouncements(context); onComplete()
        db.collection("announcements").document(announcement.id).delete()
            .addOnFailureListener { Log.w("DataRepository", "Error deleting announcement", it) }
    }

    fun deleteMember(context: Context, member: Member, onComplete: () -> Unit) {
        members.remove(member); saveMembers(context); onComplete()
        db.collection("members").document(member.id).delete()
            .addOnFailureListener { Log.w("DataRepository", "Error deleting member", it) }
    }

    fun editMember(context: Context, member: Member, name: String, rollNo: String, contactInfo: String, role: String, onComplete: () -> Unit) {
        val index = members.indexOfFirst { it.id == member.id }
        if (index == -1) return
        val updated = member.copy(name = name, rollNo = rollNo, contactInfo = contactInfo, role = role)
        members[index] = updated
        saveMembers(context)
        onComplete()
        db.collection("members").document(member.id)
            .update("name", name, "rollNo", rollNo, "contactInfo", contactInfo, "role", role)
            .addOnFailureListener { Log.w("DataRepository", "Error updating member", it) }
    }

    fun deleteWing(context: Context, wingName: String, onComplete: () -> Unit) {
        wings.remove(wingName)
        announcements.removeAll { it.wingName.equals(wingName, ignoreCase = true) }
        members.removeAll { it.wingName.equals(wingName, ignoreCase = true) }
        resources.removeAll { it.wingName.equals(wingName, ignoreCase = true) }
        saveWings(context); saveAnnouncements(context); saveMembers(context); saveResources(context)
        onComplete()

        db.collection("wings").document(wingName).delete()
            .addOnFailureListener { Log.w("DataRepository", "Error deleting wing", it) }
        listOf("announcements", "members", "resources").forEach { col ->
            db.collection(col).whereEqualTo("wingName", wingName).get()
                .addOnSuccessListener { snap -> snap.documents.forEach { it.reference.delete() } }
        }
    }
}
