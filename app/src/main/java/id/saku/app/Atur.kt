package id.saku.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun Modifier.tap2(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = MutableInteractionSource(), indication = null, onClick = onClick
)

@Composable
fun Atur(saku: Saku, t: Tok, onChange: (Saku) -> Unit) {
    var newName by remember { mutableStateOf("") }
    val nameFocus = remember { FocusRequester() }
    val scroll = rememberScrollState()

    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("PENGATURAN", color = t.ink, fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))

        // ---- tema
        Text("TEMA", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(CircleShape).background(t.track).padding(3.dp)) {
            listOf(true to "Gelap", false to "Terang").forEach { (dark, name) ->
                val on = saku.dark == dark
                Box(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (on) t.panel else Color.Transparent)
                        .tap2 { onChange(saku.copy(dark = dark)) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = if (on) t.ink else t.muted, fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        // ---- kategori
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("KATEGORI", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
            Text("${saku.cats.size}", color = t.dim, fontSize = 10.sp)
        }
        Spacer(Modifier.height(10.dp))

        saku.cats.forEachIndexed { i, c ->
            val used = saku.tx.count { it.cat == c.id }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                        .background(Color(c.color).copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(catIcon(c.icon), null, tint = Color(c.color), modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = t.ink, fontSize = 14.sp)
                    if (used > 0) Text("$used transaksi", color = t.dim, fontSize = 10.sp)
                }
                // urutkan
                if (i > 0) Icon(Icons.Rounded.ArrowUpward, "Naik", tint = t.muted,
                    modifier = Modifier.size(18.dp).tap2 {
                        val l = saku.cats.toMutableList(); val a = l[i]; l[i] = l[i - 1]; l[i - 1] = a
                        onChange(saku.copy(cats = l))
                    })
                Spacer(Modifier.width(10.dp))
                if (i < saku.cats.size - 1) Icon(Icons.Rounded.ArrowDownward, "Turun", tint = t.muted,
                    modifier = Modifier.size(18.dp).tap2 {
                        val l = saku.cats.toMutableList(); val a = l[i]; l[i] = l[i + 1]; l[i + 1] = a
                        onChange(saku.copy(cats = l))
                    })
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Rounded.Close, "Hapus", tint = if (used > 0) t.dim.copy(alpha = 0.3f) else t.neg,
                    modifier = Modifier.size(17.dp).tap2 {
                        if (used == 0) onChange(saku.copy(cats = saku.cats.filterIndexed { j, _ -> j != i }))
                    })
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(t.track)
                .tap2 { nameFocus.requestFocus() }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (newName.isEmpty()) Text("Nama kategori baru", color = t.dim, fontSize = 13.sp)
                BasicTextField(
                    value = newName,
                    onValueChange = { newName = it.take(18) },
                    textStyle = TextStyle(color = t.ink, fontSize = 13.sp),
                    cursorBrush = SolidColor(t.lime),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(20.dp).focusRequester(nameFocus)
                )
            }
            Icon(Icons.Rounded.Add, "Tambah", tint = if (newName.isBlank()) t.dim else t.lime,
                modifier = Modifier.size(20.dp).tap2 {
                    if (newName.isNotBlank()) {
                        val palette = listOf(0xFFFF7A6B, 0xFFE0B341, 0xFF5FB3E0, 0xFFA98BF5, 0xFFE07BB0, 0xFF57D9C0, 0xFF7BE495)
                        val icons = listOf("restaurant", "directions_car", "home", "shopping_bag", "movie", "favorite", "more_horiz")
                        val k = saku.cats.size % palette.size
                        val cat = Cat(
                            id = "c" + System.nanoTime().toString(36),
                            name = newName.trim(),
                            color = palette[k].toLong(),
                            icon = icons[k]
                        )
                        onChange(saku.copy(cats = saku.cats + cat))
                        newName = ""
                    }
                })
        }
        Spacer(Modifier.height(24.dp))

        // ---- info
        Text("DATA", color = t.dim, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Spacer(Modifier.height(8.dp))
        InfoRow("Transaksi", "${saku.tx.size}", t)
        InfoRow("Target nabung", "${saku.goals.size}", t)
        InfoRow("Saldo tersimpan", Fmt.rp(saku.balance()), t)
        Spacer(Modifier.height(20.dp))
        Text("Saku \u00b7 Compose \u00b7 liquid glass AGSL", color = t.dim, fontSize = 11.sp)
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun InfoRow(label: String, value: String, t: Tok) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = t.ink, fontSize = 13.sp)
        Text(value, color = t.muted, fontSize = 12.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(t.line))
}
