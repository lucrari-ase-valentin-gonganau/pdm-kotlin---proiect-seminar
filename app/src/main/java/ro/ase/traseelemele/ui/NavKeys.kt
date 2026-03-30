package ro.ase.traseelemele.ui
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable


@Serializable data object Despre: NavKey
@Serializable data class DetaliiTraseu(val numeTraseu: String): NavKey
@Serializable data object Setari: NavKey
@Serializable data object TraseulNou: NavKey
@Serializable data object TraseeleMele: NavKey

@Serializable data object StatusSms: NavKey
@Serializable data class ListaMesaje(val tip: String): NavKey

@Serializable data object TrimiteSms: NavKey
