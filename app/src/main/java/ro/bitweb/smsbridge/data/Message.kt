package ro.bitweb.smsbridge.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val message: String,
    val status: String,
    // ID-ul mesajului asa cum vine de la server prin WebSocket (campul "id" din JSON).
    // Folosit ca sa putem actualiza statusul (Trimis/Livrat/Esuat) cand vine raspunsul
    // real de la SmsManager, care soseste asincron dupa ce randul a fost deja inserat.
    val externalId: String? = null,
    // Momentul (epoch millis) cand mesajul a fost inserat local. Folosit pentru
    // filtrarea dupa interval de date in lista de mesaje. Default = acum, evaluat la
    // construirea obiectului (adica chiar inainte de insert).
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: Message): Long

    @Update
    suspend fun update(message: Message)

    // Actualizeaza statusul unui mesaj trimis, pe baza ID-ului primit de la server.
    // Daca sunt mai multe randuri cu acelasi externalId (nu ar trebui, dar ne asiguram),
    // se actualizeaza cel mai recent inserat.
    @Query(
        """
        UPDATE messages SET status = :status
        WHERE id = (
            SELECT id FROM messages WHERE externalId = :externalId ORDER BY id DESC LIMIT 1
        )
        """
    )
    suspend fun updateStatusByExternalId(externalId: String, status: String)

    // Cate mesaje exista deja cu acest ID de la server. Folosit pentru deduplicare:
    // fiecare SMS primit prin WebSocket e unic, deci daca ID-ul exista deja in DB,
    // comanda e un duplicat (replay de flow, reconectare, sau retrimitere de la
    // server dupa un ACK pierdut) si NU trebuie sa mai trimitem inca un SMS.
    @Query("SELECT COUNT(*) FROM messages WHERE externalId = :externalId")
    suspend fun countByExternalId(externalId: String): Int

    @Query("SELECT * FROM messages")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM messages WHERE status = 'Received'")
    fun getReceivedCount(): Flow<Int>

    // Inainte cauta exact 'Sent via WebSocket' / 'Sent'. Acum SmsStatusReceiver poate
    // seta statusuri noi ('Trimis', 'Livrat', 'Trimitere in curs...', 'Esuat: ...'),
    // asa ca definim "trimis" ca fiind orice mesaj care nu e 'Received', in loc sa
    // hardcodam fiecare text de status posibil (fragil la orice schimbare viitoare).
    @Query("SELECT COUNT(*) FROM messages WHERE status != 'Received'")
    fun getSentCount(): Flow<Int>

    // Totalul de mesaje (trimise + in asteptare). Folosit pentru butonul de sumar
    // "SMS-uri (trimise/total)".
    @Query("SELECT COUNT(*) FROM messages")
    fun getTotalCount(): Flow<Int>

    // Lista paginata + filtrata pentru ecranul de mesaje.
    //  - tip: "toate" | "trimise" | "receptionate"
    //  - q: cautare in numar (sender) SAU in textul mesajului (message); "" = fara filtru
    //  - fromTs / toTs: interval de timp (epoch millis); null = capat deschis
    //  - limit: cate randuri se incarca (creste cu 20 la scroll = infinite scroll)
    // Ordonat mereu descrescator (cele mai noi primele) ca sa fie consistent cu
    // incarcarea progresiva.
    @Query(
        """
        SELECT * FROM messages
        WHERE (
            :tip = 'toate'
            OR (:tip = 'trimise' AND status != 'Received')
            OR (:tip = 'receptionate' AND status = 'Received')
        )
        AND (:q = '' OR sender LIKE '%' || :q || '%' OR message LIKE '%' || :q || '%')
        AND (:fromTs IS NULL OR timestamp >= :fromTs)
        AND (:toTs IS NULL OR timestamp <= :toTs)
        ORDER BY id DESC
        LIMIT :limit
        """
    )
    fun getMessagesFiltered(
        tip: String,
        q: String,
        fromTs: Long?,
        toTs: Long?,
        limit: Int
    ): Flow<List<Message>>

    // Aceleasi filtre ca mai sus, dar fara limita si ca lista simpla (nu Flow).
    // Folosit la export CSV: exportam exact ce corespunde filtrelor active.
    @Query(
        """
        SELECT * FROM messages
        WHERE (
            :tip = 'toate'
            OR (:tip = 'trimise' AND status != 'Received')
            OR (:tip = 'receptionate' AND status = 'Received')
        )
        AND (:q = '' OR sender LIKE '%' || :q || '%' OR message LIKE '%' || :q || '%')
        AND (:fromTs IS NULL OR timestamp >= :fromTs)
        AND (:toTs IS NULL OR timestamp <= :toTs)
        ORDER BY id DESC
        """
    )
    suspend fun getMessagesForExport(
        tip: String,
        q: String,
        fromTs: Long?,
        toTs: Long?
    ): List<Message>
}
