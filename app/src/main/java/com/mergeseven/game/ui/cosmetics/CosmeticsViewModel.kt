package com.mergeseven.game.ui.cosmetics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mergeseven.game.core.flags.Feature
import com.mergeseven.game.core.flags.FeatureFlags
import com.mergeseven.game.core.flags.enabledState
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.meta.CosmeticCatalog
import com.mergeseven.game.meta.CosmeticDef
import com.mergeseven.game.meta.CosmeticKind
import com.mergeseven.game.meta.UnlockService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CosmeticRow(
    val def: CosmeticDef,
    val owned: Boolean,
    val equipped: Boolean
)

data class CosmeticsUiState(
    val tab: CosmeticKind = CosmeticKind.TILE,
    val rows: List<CosmeticRow> = emptyList(),
    val coins: Int = 0,
    val message: String? = null,
    val af5Enabled: Boolean = false
)

@HiltViewModel
class CosmeticsViewModel @Inject constructor(
    private val unlockService: UnlockService,
    private val userDataRepository: UserDataRepository,
    featureFlags: FeatureFlags
) : ViewModel() {

    private val tab = MutableStateFlow(CosmeticKind.TILE)
    private val message = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { unlockService.loadAndSeed() }
    }

    val uiState: StateFlow<CosmeticsUiState> = combine(
        tab,
        unlockService.owned,
        userDataRepository.userProfile,
        message,
        featureFlags.enabledState(Feature.AF5, viewModelScope)
    ) { kind, owned, profile, msg, af5 ->
        val defs = if (kind == CosmeticKind.TILE) CosmeticCatalog.tiles() else CosmeticCatalog.boards()
        CosmeticsUiState(
            tab = kind,
            rows = defs.map { def ->
                val isOwned = def.id in owned || def.startingOwned
                val equipped = if (kind == CosmeticKind.TILE) {
                    profile.equippedTileThemeId == def.id
                } else {
                    profile.equippedBoardThemeId == def.id
                }
                CosmeticRow(def, isOwned, equipped)
            },
            coins = profile.coins,
            message = msg,
            af5Enabled = af5
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CosmeticsUiState())

    fun selectTab(kind: CosmeticKind) {
        tab.value = kind
    }

    fun equip(id: String) {
        viewModelScope.launch {
            if (!unlockService.owns(id) && CosmeticCatalog.byId(id)?.startingOwned != true) {
                message.value = "Not owned"
                return@launch
            }
            val def = CosmeticCatalog.byId(id) ?: return@launch
            if (def.kind == CosmeticKind.TILE) {
                userDataRepository.equipTileTheme(id)
            } else {
                userDataRepository.equipBoardTheme(id)
            }
            message.value = "Equipped ${def.title}"
        }
    }

    fun buy(id: String) {
        viewModelScope.launch {
            val ok = unlockService.tryBuyCosmetic(id)
            message.value = if (ok) "Purchased" else "Not enough coins"
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
