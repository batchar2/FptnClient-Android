package org.fptn.vpn.ui.updatetoken

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.elvishew.xlog.XLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.fptn.vpn.database.AppDatabase
import org.fptn.vpn.database.entity.TokenEntity
import org.fptn.vpn.utils.SharedPrefUtils
import org.fptn.vpn.utils.token.TokenUtils
import org.fptn.vpn.vpnclient.exception.PVNClientException

class UpdateTokenViewModel(application: Application) : AndroidViewModel(application) {

    val errorTextLiveData = MutableLiveData("")

    private val appDatabase = AppDatabase.getInstance(application)

    @Throws(PVNClientException::class)
    suspend fun parseAndSaveToken(token: String) {
        val parsed = TokenUtils.parseToken(token)
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val username = parsed.username
            val password = parsed.password
            val serviceName = parsed.serviceName ?: username

            val existingToken = appDatabase.tokenDAO().findByUsername(username)

            if (existingToken != null) {
                appDatabase.tokenDAO().updateTokenData(
                    existingToken.id, serviceName, token, username, password, now
                )

                appDatabase.serverDAO().deleteByTokenId(existingToken.id)
                parsed.servers.forEach { it.tokenId = existingToken.id }
                appDatabase.serverDAO().insertAll(parsed.servers)

                XLog.tag("UpdateTokenViewModel").i("Token updated [id=%d, serviceName=%s, servers=%d]",
                    existingToken.id, serviceName, parsed.servers.size)
            } else {
                val newToken = TokenEntity(
                    0,               // id
                    serviceName,     // serviceName
                    token,           // rawToken
                    username,        // username
                    password,        // password
                    true,            // isVisible
                    now,             // createdAt
                    now              // updatedAt
                )
                val newTokenId = appDatabase.tokenDAO().insert(newToken).toInt()

                parsed.servers.forEach { it.tokenId = newTokenId }
                appDatabase.serverDAO().insertAll(parsed.servers)

                XLog.tag("UpdateTokenViewModel").i("New token created [id=%d, serviceName=%s, servers=%d]",
                    newTokenId, serviceName, parsed.servers.size)
            }

            SharedPrefUtils.saveTokenUpdatedDate(getApplication(), now)
        }
    }
}