package ro.ase.smsbridge.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object StatusSms : NavKey
@Serializable data class ListaMesaje(val tip: String) : NavKey
@Serializable data object TrimiteSms : NavKey
@Serializable data object Setari : NavKey
@Serializable data object Despre : NavKey
