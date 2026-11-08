package ir.heydar.nexfolder.core.network.clients

import android.content.Context
import ir.heydar.nexfolder.domain.model.network.NetworkProtocol
import ir.heydar.nexfolder.domain.model.network.RemoteConnection
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Persists saved remote connections in private shared preferences (JSON). */
@Singleton
class ConnectionStore @Inject constructor(
    private val context: Context
) {

    private val prefs = context.getSharedPreferences("network_connections", Context.MODE_PRIVATE)

    fun load(): List<RemoteConnection> {
        return try {
            val json = prefs.getString(KEY, "[]") ?: "[]"
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                RemoteConnection(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    protocol = NetworkProtocol.entries.firstOrNull {
                        it.name == obj.optString("protocol")
                    } ?: NetworkProtocol.FTP,
                    host = obj.optString("host"),
                    port = obj.optInt("port"),
                    username = obj.optString("username"),
                    password = obj.optString("password"),
                    anonymous = obj.optBoolean("anonymous"),
                    domain = obj.optString("domain")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(connections: List<RemoteConnection>) {
        val array = JSONArray()
        connections.forEach { connection ->
            array.put(
                JSONObject().apply {
                    put("id", connection.id)
                    put("name", connection.name)
                    put("protocol", connection.protocol.name)
                    put("host", connection.host)
                    put("port", connection.port)
                    put("username", connection.username)
                    put("password", connection.password)
                    put("anonymous", connection.anonymous)
                    put("domain", connection.domain)
                }
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun findById(id: String): RemoteConnection? = load().firstOrNull { it.id == id }

    fun add(connection: RemoteConnection) {
        save(load().filterNot { it.id == connection.id } + connection)
    }

    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    private companion object {
        const val KEY = "connections"
    }
}
