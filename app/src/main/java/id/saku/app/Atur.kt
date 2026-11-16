package id.saku.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun Atur(saku: Saku, t: Tok, onChange: (Saku) -> Unit) {
    var newName by remember { mutableStateOf("") }
    var glyphIdx by remember { mutableStateOf(0) }
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 24.dp)) {
        Text("Atur", Type.title, t.ink, Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(20.dp))

        // Tema
        Text("Tema", Type.label, t.muted2)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, t.hair, RoundedCornerShape(10.dp))
        ) {
            listOf("Terang" to false, "Gelap" to true).forEach { (label, dark) ->
                val on = saku.dark == dark
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) t.accentWash else Color.Transparent)
                        .tap { onChange(saku.copy(dark = dark)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        Type.label.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                        if (on) t.accent else t.muted
                    )
                }
            }
        }

        // Kategori
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Kategori", Type.label, t.muted2)
            Spacer(Modifier.weight(1f))
            Text("${saku.cats.size}", Type.micro, t.muted2)
        }
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)

        saku.cats.forEachIndexed { i, cat ->
            val used = saku.countFor(cat.id)
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Glyph(glyphOf(cat.glyph), t.ink, 16.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(cat.name, Type.body, t.ink)
                    if (used > 0) {
                        Text("$used transaksi", Type.micro, t.muted2, Modifier.padding(top = 1.dp))
                    }
                }
                if (i > 0) {
                    TouchTarget {
                        Box(Modifier.fillMaxSize().tap {
                            val list = saku.cats.toMutableList()
                            val item = list.removeAt(i)
                            list.add(i - 1, item)
                            onChange(saku.copy(cats = list))
                        }, contentAlignment = Alignment.Center) {
                            Glyph(Ic.Up, t.muted2, 16.dp)
                        }
                    }
                }
                if (i < saku.cats.lastIndex) {
                    TouchTarget {
                        Box(Modifier.fillMaxSize().tap {
                            val list = saku.cats.toMutableList()
                            val item = list.removeAt(i)
                            list.add(i + 1, item)
                            onChange(saku.copy(cats = list))
                        }, contentAlignment = Alignment.Center) {
                            Glyph(Ic.Down, t.muted2, 16.dp)
                        }
                    }
                }
                TouchTarget {
                    Box(
                        Modifier.fillMaxSize().tap {
                            if (used == 0) onChange(saku.copy(cats = saku.cats.filter { it.id != cat.id }))
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Ic.Trash, if (used == 0) t.muted2 else t.hair, 16.dp)
                    }
                }
            }
            Hair(t.hairSoft)
        }

        Spacer(Modifier.height(16.dp))
        Text("Kategori baru", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FieldShell(t) {
                    PlainField(
                        value = newName,
                        onValueChange = { newName = it.take(18) },
                        placeholder = "Nama kategori",
                        t = t,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    TouchTarget {
                        Box(
                            Modifier.fillMaxSize().tap { glyphIdx = (glyphIdx + 1) % glyphChoices.size },
                            contentAlignment = Alignment.Center
                        ) {
                            Glyph(glyphOf(glyphChoices[glyphIdx]), t.muted2, 16.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (newName.isBlank()) t.hair else t.accent)
                    .tap {
                        if (newName.isNotBlank()) {
                            onChange(
                                saku.copy(
                                    cats = saku.cats + Cat(
                                        id = "c" + System.nanoTime().toString(36),
                                        name = newName.trim(),
                                        glyph = glyphChoices[glyphIdx]
                                    )
                                )
                            )
                            newName = ""
                            focus.clearFocus()
                        }
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tambah",
                    Type.label.copy(fontWeight = FontWeight.SemiBold),
                    if (newName.isBlank()) t.faint else (if (t.dark) t.bg else Color.White)
                )
            }
        }

        // Data
        Spacer(Modifier.height(26.dp))
        Text("Data", Type.label, t.muted2)
        Spacer(Modifier.height(6.dp))
        Hair(t.hair)
        InfoRow(t, "Transaksi", saku.tx.size.toString())
        InfoRow(t, "Target nabung", saku.goals.size.toString())
        InfoRow(t, "Saldo", Fmt.rp(saku.balance()))

        Spacer(Modifier.height(24.dp))
        Text("Saku \u00b7 Compose", Type.label, t.ink)
        Text("Data tersimpan di perangkat ini. Tanpa akun, tanpa server.", Type.micro, t.muted2, Modifier.padding(top = 4.dp), maxLines = 2)
        Spacer(Modifier.height(110.dp))
    }
}

@Composable
private fun InfoRow(t: Tok, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Type.body, t.ink, Modifier.weight(1f))
        Text(value, Type.figure, t.muted2)
    }
    Hair(t.hairSoft)
}
