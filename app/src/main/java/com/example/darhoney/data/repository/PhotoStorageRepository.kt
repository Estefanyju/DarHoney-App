package com.example.darhoney.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PhotoStorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadPhoto(
        coupleId: String,
        uri: Uri,
        onProgress: (Int) -> Unit
    ): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("couples/$coupleId/photos/$fileName")
        val uploadTask = ref.putFile(uri)
        uploadTask.addOnProgressListener { snapshot ->
            if (snapshot.totalByteCount > 0) {
                val pct = ((100 * snapshot.bytesTransferred) / snapshot.totalByteCount).toInt()
                onProgress(pct)
            }
        }
        uploadTask.await()
        return ref.downloadUrl.await().toString()
    }
}
