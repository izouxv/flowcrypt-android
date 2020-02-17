/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.node.PgpApiRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseDecryptMsgRequest
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.node.ParseDecryptedMsgResult
import com.flowcrypt.email.model.PgpKeyInfo
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.cache.DiskLruCache
import com.sun.mail.util.ASCIIUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import org.spongycastle.bcpg.ArmoredInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.mail.BodyPart
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.internet.MimeMessage

/**
 * This [ViewModel] implementation can be used to parse and decrypt (if needed) an incoming message.
 *
 * @author Denis Bondarenko
 * Date: 3/21/19
 * Time: 11:47 AM
 * E-mail: DenBond7@gmail.com
 */
class DecryptMessageViewModel(application: Application) : BaseNodeApiViewModel(application),
    KeysStorageImpl.OnRefreshListener {
  val headersLiveData: MutableLiveData<String> = MutableLiveData()
  val decryptLiveData: MutableLiveData<Result<ParseDecryptedMsgResult?>> = MutableLiveData()

  private val keysStorage: KeysStorageImpl = KeysStorageImpl.getInstance(application)
  private val apiRepository: PgpApiRepository = NodeRepository()

  init {
    this.keysStorage.attachOnRefreshListener(this)
  }

  override fun onRefresh() {}

  fun decryptMessage(rawMimeBytes: ByteArray) {
    decryptLiveData.value = Result.loading()
    viewModelScope.launch {
      headersLiveData.value = getHeaders(ByteArrayInputStream(rawMimeBytes))
      val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
      val result = apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(data = rawMimeBytes, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
      decryptLiveData.value = result
    }
  }

  fun decryptMessage(context: Context, msgSnapshot: DiskLruCache.Snapshot) {
    decryptLiveData.value = Result.loading()
    viewModelScope.launch {
      val uri = msgSnapshot.getUri(0)
      if (uri != null) {
        headersLiveData.value = getHeaders(context.contentResolver.openInputStream(uri))
        val pgpKeyInfoList = keysStorage.getAllPgpPrivateKeys()
        val largerThan1Mb = msgSnapshot.getLength(0) > 1024 * 1000
        val result = if (largerThan1Mb) {
          parseMimeAndDecrypt(context, uri, pgpKeyInfoList)
        } else {
          apiRepository.parseDecryptMsg(
              request = ParseDecryptMsgRequest(context = context, uri = uri, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
        }
        decryptLiveData.value = result
      } else {
        val byteArray = msgSnapshot.getByteArray(0)
        decryptMessage(byteArray)
      }
    }
  }

  private suspend fun parseMimeAndDecrypt(context: Context, uri: Uri, pgpKeyInfoList: List<PgpKeyInfo>): Result<ParseDecryptedMsgResult?> {
    val uriOfEncryptedPart = getUriOfEncryptedPart(context, uri)
    return if (uriOfEncryptedPart != null) {
      apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(context = context, uri = uriOfEncryptedPart, pgpKeyInfos = pgpKeyInfoList, isEmail = false))
    } else {
      apiRepository.parseDecryptMsg(
          request = ParseDecryptMsgRequest(context = context, uri = uriOfEncryptedPart, pgpKeyInfos = pgpKeyInfoList, isEmail = true))
    }
  }

  private suspend fun getMimeMessageFromInputStream(context: Context, uri: Uri) =
      withContext(Dispatchers.IO) {
        MimeMessage(null, context.contentResolver.openInputStream(uri))
      }

  private suspend fun getUriOfEncryptedPart(context: Context, uri: Uri): Uri? {
    val mimeMessage: MimeMessage = getMimeMessageFromInputStream(context, uri)
    return findEncryptedPart(mimeMessage)
  }

  private suspend fun findEncryptedPart(part: Part): Uri? = withContext(Dispatchers.Default) {
    try {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        val multiPart = part.content as Multipart
        val partsNumber = multiPart.count
        for (partCount in 0 until partsNumber) {
          val bodyPart = multiPart.getBodyPart(partCount)
          if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            val encryptedPart = findEncryptedPart(bodyPart)
            if (encryptedPart != null) {
              return@withContext encryptedPart
            }
          } else if (bodyPart?.disposition?.toLowerCase(Locale.getDefault()) in listOf(Part.ATTACHMENT, Part.INLINE)) {
            val fileName = bodyPart.fileName?.toLowerCase(Locale.getDefault()) ?: ""
            if (fileName in listOf("message", "msg.asc", "message.asc", "encrypted.asc", "encrypted.eml.pgp", "Message.pgp", "")) {
              val file = prepareTempFile(bodyPart)
              return@withContext Uri.fromFile(file)
            }

            val contentType = bodyPart.contentType?.toLowerCase(Locale.getDefault()) ?: ""
            if (contentType in listOf("application/octet-stream", "application/pgp-encrypted")) {
              val file = prepareTempFile(bodyPart)
              return@withContext Uri.fromFile(file)
            }
          }
        }
        return@withContext null
      } else {
        return@withContext null
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      return@withContext null
    } catch (e: IOException) {
      e.printStackTrace()
      return@withContext null
    }
  }

  private suspend fun prepareTempFile(bodyPart: BodyPart): File = withContext(Dispatchers.IO) {
    val tempDir = CacheManager.getCurrentMsgTempDir()
    val file = File(tempDir, FILE_NAME_ENCRYPTED_MESSAGE)
    IOUtils.copy(ArmoredInputStream(bodyPart.inputStream), FileOutputStream(file))
    return@withContext file
  }

  /**
   * We fetch the first 50Kb from the given input stream and extract headers.
   */
  private suspend fun getHeaders(inputStream: InputStream?): String = withContext(Dispatchers.IO) {
    inputStream ?: return@withContext ""
    val d = ByteArray(50000)
    IOUtils.read(inputStream, d)
    EmailUtil.getHeadersFromRawMIME(ASCIIUtility.toString(d))
  }

  companion object {
    private const val FILE_NAME_ENCRYPTED_MESSAGE = "temp_encrypted_msg.asc"
  }

}
