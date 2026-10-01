package com.mergeseven.game.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mergeseven.game.R
import com.mergeseven.game.ui.theme.GameColors

@Composable
fun WoodPage(title: String, coins: Int? = null, onBack: (() -> Unit)? = null,
             headerAction: @Composable () -> Unit = {}, footer: @Composable () -> Unit = {},
             content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.bg_wood_v2), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(GameColors.WoodDark.copy(alpha = 0.64f)))
        Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) IconButton(onBack) { GameIcon(GameIcons.Back, "Back", tint = GameColors.TextWhite) }
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (coins != null) Surface(shape = RoundedCornerShape(20.dp), color = GameColors.WoodMid) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(size = 20.dp); Spacer(Modifier.width(6.dp)); Text("$coins", color = GameColors.CoinGold)
                    }
                }
                headerAction()
            }
            Column(Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content)
            footer()
        }
    }
}
@Composable
fun WoodPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = GameColors.WoodMid.copy(alpha = 0.98f), border = BorderStroke(1.dp, GameColors.WoodLight.copy(alpha = 0.5f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
fun GoldButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(
            containerColor = GameColors.CoinGold, contentColor = GameColors.TextDark)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
@Composable
fun WoodLink(title: String, description: String, art: Int? = null, onClick: () -> Unit) {
    Surface(onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = GameColors.WoodMid, border = BorderStroke(1.dp, GameColors.WoodLight.copy(alpha = 0.4f))) {
        Row(Modifier.padding(16.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (art != null) Image(painterResource(art), null, Modifier.size(64.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = GameColors.TextWhite.copy(alpha = 0.8f))
            }
        }
    }
}
