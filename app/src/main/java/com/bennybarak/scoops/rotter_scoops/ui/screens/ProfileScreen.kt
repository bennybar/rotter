package com.bennybarak.scoops.rotter_scoops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bennybarak.scoops.rotter_scoops.ui.LocalLanguage
import com.bennybarak.scoops.rotter_scoops.ui.ProfileRoute
import com.bennybarak.scoops.rotter_scoops.ui.palette
import com.bennybarak.scoops.rotter_scoops.ui.strings
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppBar
import com.bennybarak.scoops.rotter_scoops.ui.widgets.AppScaffold
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BackButton
import com.bennybarak.scoops.rotter_scoops.ui.widgets.BarTitle
import com.bennybarak.scoops.rotter_scoops.ui.widgets.PostBody
import com.bennybarak.scoops.rotter_scoops.ui.widgets.relTime

/**
 * A fully native member profile: name, reputation points + stats, and the
 * member's posts in the current thread. No webview, no rotter.net page.
 */
@Composable
fun ProfileScreen(r: ProfileRoute) {
    val l = strings
    val p = palette
    val lang = LocalLanguage.current
    val accent = MaterialTheme.colorScheme.primary
    AppScaffold(bar = { AppBar(leading = { BackButton() }, title = { BarTitle(l.userDetails) }) }) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            LazyColumn(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(52.dp).background(accent.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Person, null, tint = accent, modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Text(r.name, style = TextStyle(fontWeight = FontWeight.W800, fontSize = 18.sp, color = p.ink), modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    // Real rotter member stats (parsed from the thread HTML).
                    if (r.points != null || r.raters != null || r.messages != null || r.joinDate != null) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(p.surface, RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                        ) {
                            r.points?.let { Stat("$it", l.memberPoints, accent) }
                            r.raters?.let { Stat("$it", l.memberRaters, accent) }
                            r.messages?.let { Stat("$it", l.memberPosts, accent) }
                        }
                    }
                    if (r.joinDate != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("${l.memberSince} ${r.joinDate}", style = TextStyle(fontSize = 12.5.sp, color = p.muted))
                    }
                    Spacer(Modifier.height(22.dp))
                    Text(
                        l.userPostsInThread(r.posts.size),
                        style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.W800, letterSpacing = 0.6.sp, color = p.muted),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                items(r.posts) { m ->
                    Column(
                        Modifier
                            .padding(bottom = 10.dp)
                            .fillMaxWidth()
                            .background(p.surface, RoundedCornerShape(16.dp))
                            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        val hasTitle = !m.title.isNullOrEmpty()
                        if (hasTitle) {
                            Text(m.title!!, style = TextStyle(fontWeight = FontWeight.W700, color = p.ink, fontSize = 14.5.sp))
                        }
                        if (!m.bodyHtml.isNullOrEmpty()) {
                            if (hasTitle) Spacer(Modifier.height(6.dp))
                            PostBody(m.bodyHtml, r.baseUrl, fontSize = 14f)
                        }
                        if (m.timestamp != null || m.time != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (m.timestamp != null) relTime(m.timestamp, l, lang) else (m.time ?: ""),
                                style = TextStyle(fontSize = 11.5.sp, color = p.muted),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String, accent: Color) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = TextStyle(fontWeight = FontWeight.W800, fontSize = 18.sp, color = accent))
        Spacer(Modifier.height(2.dp))
        Text(label, style = TextStyle(fontSize = 11.5.sp, color = palette.muted))
    }
}
