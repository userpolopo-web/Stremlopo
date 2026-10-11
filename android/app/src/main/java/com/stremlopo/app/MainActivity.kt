package com.stremlopo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val Ink=Color(0xFF090909)
private val Panel=Color(0xFF1C1C1E)
private val Muted=Color(0xFFA4A4AB)

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Color.White,onPrimary=Color.Black,background=Ink,surface=Panel,secondary=Muted)) { Stremlopo() } }
    }
}

@Composable fun Stremlopo() {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("stremlopo",0)}
    val scope=rememberCoroutineScope()
    val bundled=remember {Api.bundled(context)}
    var films by remember {mutableStateOf(bundled)}
    var tab by remember {mutableStateOf("Início")}
    var query by remember {mutableStateOf("")}
    var genre by remember {mutableStateOf("Todos")}
    var selected by remember {mutableStateOf<Film?>(null)}
    var playing by remember {mutableStateOf<Film?>(null)}
    var settings by remember {mutableStateOf(false)}
    var server by remember {mutableStateOf(prefs.getString("server","") ?: "")}
    var message by remember {mutableStateOf("")}
    var refreshing by remember {mutableStateOf(false)}
    var favorites by remember {mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet())}
    var history by remember {mutableStateOf(prefs.getStringSet("history",emptySet())!!.toSet())}
    fun favorite(film: Film) {favorites=if(film.id in favorites) favorites-film.id else favorites+film.id;prefs.edit().putStringSet("favorites",favorites).apply()}
    fun play(film: Film) {history=history+film.id;prefs.edit().putStringSet("history",history).apply();playing=film}
    fun refresh() {if(server.isNotBlank()) scope.launch {refreshing=true;try {films=mergeCatalog(bundled,Api.catalog(server));prefs.edit().putString("server",server).apply();message="Catálogo atualizado."} catch(e: Exception){message="O catálogo adicional está indisponível. Os filmes incluídos continuam disponíveis."} finally {refreshing=false}}}
    LaunchedEffect(Unit) {refresh()}
    val current=playing
    if(current!=null) {
        PlayerScreen(current,onClose={playing=null})
        return
    }
    val detail=selected
    if(detail!=null) {
        BackHandler {selected=null}
        DetailScreen(detail,detail.id in favorites,onBack={selected=null},onPlay={play(detail)},onFavorite={favorite(detail)},server=server)
    } else {
        Scaffold(containerColor=Ink,bottomBar={
            NavigationBar(containerColor=Color(0xFF111113),tonalElevation=0.dp) {
                listOf("Início" to Icons.Outlined.Home,"Filmes" to Icons.Outlined.Movie,"Buscar" to Icons.Outlined.Search,"Minha lista" to Icons.Outlined.BookmarkBorder).forEach { (name,icon) ->
                    NavigationBarItem(selected=tab==name,onClick={tab=name},icon={Icon(icon,contentDescription=null)},label={Text(name,fontSize=11.sp)},colors=NavigationBarItemDefaults.colors(indicatorColor=Color(0xFF29292C),selectedIconColor=Color.White,unselectedIconColor=Muted,selectedTextColor=Color.White,unselectedTextColor=Muted))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("S",fontWeight=FontWeight.Black,fontSize=30.sp,color=Color.White)
                    Text("STREMLOPO",fontWeight=FontWeight.Bold,letterSpacing=2.sp,fontSize=15.sp,modifier=Modifier.padding(start=10.dp).weight(1f))
                    IconButton(onClick={tab="Buscar"}) {Icon(Icons.Outlined.Search,contentDescription="Buscar filmes")}
                    IconButton(onClick={settings=true}) {Icon(Icons.Outlined.Settings,contentDescription="Configurações",tint=Muted)}
                }
                if(tab=="Início") {
                    val filtered=filterFilms(films,"",genre)
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(22.dp),contentPadding=PaddingValues(bottom=28.dp)) {
                        item {GenreChips(genre,onSelect={genre=it})}
                        val hero=filtered.firstOrNull()
                        if(hero!=null) item {Hero(hero,onPlay={play(hero)},onInfo={selected=hero},onFavorite={favorite(hero)},saved=hero.id in favorites)}
                        val continuing=films.filter {it.id in history && prefs.getLong("progress:${it.id}",0)>0}
                        if(continuing.isNotEmpty()) item {MovieRow("Continuar assistindo",continuing,onSelect={selected=it},progress={prefs.getLong("progress:${it.id}",0)})}
                        item {MovieRow("Escolha seu próximo filme",filtered,onSelect={selected=it})}
                        if(genre=="Todos") {
                            item {MovieRow("Animações & aventuras",films.filter {it.genre.contains("Animação")},onSelect={selected=it})}
                            item {MovieRow("Mundos de ficção",films.filter {it.genre.contains("Ficção") || it.genre.contains("Fantasia")},onSelect={selected=it})}
                        }
                        item {Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {Text("Todo o catálogo",fontWeight=FontWeight.Bold,fontSize=21.sp,modifier=Modifier.weight(1f));TextButton(onClick={tab="Filmes"}){Text("Ver todos (${films.size})")}}}
                        item {Text("Filmes independentes para descobrir e assistir.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(horizontal=20.dp))}
                    }
                } else {
                    val base=if(tab=="Minha lista") films.filter {it.id in favorites} else films
                    Column(Modifier.padding(horizontal=20.dp)) {
                        Text(if(tab=="Buscar") "O que você quer assistir?" else tab,fontSize=27.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if(tab=="Buscar") OutlinedTextField(value=query,onValueChange={query=it},placeholder={Text("Filme, aventura, animação…")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth())
                        if(tab=="Filmes") Text("${films.size} filmes disponíveis",color=Muted,fontSize=13.sp)
                        Spacer(Modifier.height(12.dp))
                    }
                    if(tab!="Minha lista") GenreChips(genre,onSelect={genre=it})
                    val result=filterFilms(base,if(tab=="Buscar") query else "",if(tab=="Minha lista") "Todos" else genre)
                    if(result.isEmpty()) Box(Modifier.fillMaxSize().padding(32.dp),contentAlignment=Alignment.Center) {Text(if(tab=="Minha lista") "Salve os filmes que você quer assistir. Eles aparecem aqui." else "Nenhum filme encontrado. Tente outro título ou categoria.",color=Muted)}
                    else LazyVerticalGrid(columns=GridCells.Adaptive(140.dp),contentPadding=PaddingValues(20.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {gridItems(result,key={it.id}) {FilmCard(it,Modifier.fillMaxWidth(),onSelect={selected=it})}}
                }
            }
        }
    }
    if(settings) AlertDialog(onDismissRequest={settings=false},title={Text("Configurações")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {Text("Os filmes incluídos já estão prontos para assistir. Você pode conectar um catálogo adicional, se quiser.",color=Muted);OutlinedTextField(value=server,onValueChange={server=it},label={Text("Catálogo adicional (opcional)")},singleLine=true);Text(message,fontSize=12.sp,color=Muted)}},confirmButton={TextButton(enabled=!refreshing,onClick={if(server.isBlank()){prefs.edit().remove("server").apply();films=bundled;settings=false}else refresh()}){Text(if(refreshing) "Atualizando…" else "Salvar")}},dismissButton={TextButton(onClick={settings=false}){Text("Fechar")}})
}

@Composable private fun GenreChips(selected: String,onSelect:(String)->Unit) {
    LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        items(listOf("Todos","Animação","Fantasia","Ficção científica")) {label ->
            FilterChip(selected=selected==label,onClick={onSelect(label)},label={Text(label,fontSize=12.sp)},shape=RoundedCornerShape(50.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Color.White,selectedLabelColor=Color.Black,containerColor=Ink,labelColor=Muted))
        }
    }
}

@Composable private fun Hero(film: Film,onPlay:()->Unit,onInfo:()->Unit,onFavorite:()->Unit,saved: Boolean) {
    Box(Modifier.fillMaxWidth().height(340.dp)) {
        AsyncImage(model=film.backdrop,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha=.05f),Ink.copy(alpha=.18f),Ink))))
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("EM DESTAQUE",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=3.sp,color=Color.LightGray)
            Text(film.title,fontWeight=FontWeight.Black,fontSize=42.sp,lineHeight=44.sp,color=Color.White)
            Text("${film.year}  •  ${film.genre}  •  ${film.duration} min",color=Color.LightGray,fontSize=12.sp)
            Text(film.synopsis,fontSize=13.sp,color=Color(0xFFD4D4D8),maxLines=2,overflow=TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Button(onClick=onPlay,shape=RoundedCornerShape(8.dp),modifier=Modifier.weight(1f).height(46.dp)) {Icon(Icons.Filled.PlayArrow,null);Spacer(Modifier.width(6.dp));Text("Assistir",fontWeight=FontWeight.Bold)}
                FilledTonalButton(onClick=onInfo,shape=RoundedCornerShape(8.dp),colors=ButtonDefaults.filledTonalButtonColors(containerColor=Color(0xFF333337),contentColor=Color.White),modifier=Modifier.weight(1f).height(46.dp)) {Icon(Icons.Outlined.Info,null,modifier=Modifier.size(19.dp));Spacer(Modifier.width(6.dp));Text("Detalhes")}
                IconButton(onClick=onFavorite) {Icon(if(saved) Icons.Outlined.Check else Icons.Outlined.Add,contentDescription="Minha lista")}
            }
        }
    }
}

@Composable private fun MovieRow(title: String,films: List<Film>,onSelect:(Film)->Unit,progress:((Film)->Long)?=null) {
    if(films.isEmpty()) return
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,fontWeight=FontWeight.Bold,fontSize=20.sp,modifier=Modifier.padding(horizontal=20.dp))
        LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            items(films,key={it.id}) {film ->FilmCard(film,Modifier.width(142.dp),onSelect={onSelect(film)},progress=progress?.invoke(film) ?: 0)}
        }
    }
}

@Composable private fun FilmCard(film: Film,modifier: Modifier,onSelect:()->Unit,progress: Long=0) {
    Column(modifier.clickable(onClick=onSelect),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f/3f).clip(RoundedCornerShape(10.dp)).background(Panel)) {
            AsyncImage(model=film.poster,contentDescription="Capa de ${film.title}",contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
            Surface(Modifier.align(Alignment.TopEnd).padding(7.dp),color=Color.Black.copy(alpha=.72f),shape=RoundedCornerShape(4.dp)) {Text("FILME",fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp,modifier=Modifier.padding(horizontal=5.dp,vertical=3.dp))}
            if(progress>0) LinearProgressIndicator(progress={ (progress/(film.duration.coerceAtLeast(1)*60000f)).coerceIn(0f,1f) },modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp),color=Color.White,trackColor=Color.DarkGray)
        }
        Text(film.title,fontWeight=FontWeight.SemiBold,fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        Text("${film.year} • ${film.duration} min",color=Muted,fontSize=11.sp)
    }
}

@Composable private fun DetailScreen(film: Film,saved: Boolean,onBack:()->Unit,onPlay:()->Unit,onFavorite:()->Unit,server: String) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var report by remember {mutableStateOf(false)};var reason by remember {mutableStateOf("")};var status by remember {mutableStateOf("")}
    Surface(color=Ink,modifier=Modifier.fillMaxSize()) {
        LazyColumn(contentPadding=PaddingValues(bottom=32.dp)) {
            item {Box(Modifier.fillMaxWidth().height(320.dp)) {
                AsyncImage(model=film.backdrop,contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Ink))))
                IconButton(onClick=onBack,modifier=Modifier.statusBarsPadding().padding(14.dp).background(Color.Black.copy(alpha=.5f),RoundedCornerShape(50))) {Icon(Icons.Outlined.ArrowBack,contentDescription="Voltar")}
            }}
            item {Column(Modifier.padding(horizontal=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Text(film.title,fontSize=34.sp,lineHeight=37.sp,fontWeight=FontWeight.Black)
                Text("${film.year}  •  ${film.duration} min  •  ${film.genre}",color=Muted,fontSize=13.sp)
                Button(onClick=onPlay,modifier=Modifier.fillMaxWidth().height(50.dp),shape=RoundedCornerShape(8.dp)) {Icon(Icons.Filled.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("Assistir agora",fontWeight=FontWeight.Bold)}
                FilledTonalButton(onClick=onFavorite,modifier=Modifier.fillMaxWidth().height(46.dp),shape=RoundedCornerShape(8.dp)) {Icon(if(saved) Icons.Outlined.Check else Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text(if(saved) "Na minha lista" else "Adicionar à minha lista")}
                Text("Sinopse",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Text(film.synopsis,color=Color(0xFFD4D4D8),fontSize=15.sp,lineHeight=23.sp)
                HorizontalDivider(color=Color(0xFF2A2A2D))
                Text("Créditos e licença",fontWeight=FontWeight.SemiBold,fontSize=13.sp)
                Text("${film.attribution}\n${film.license}",color=Muted,fontSize=12.sp,lineHeight=18.sp)
                if(film.evidence.isNotBlank()) TextButton(onClick={context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(film.evidence)))}) {Text("Ver origem e licença",fontSize=12.sp)}
                TextButton(onClick={report=true}) {Text("Relatar um problema",fontSize=12.sp,color=Muted)}
            }}
        }
    }
    if(report) AlertDialog(onDismissRequest={report=false},title={Text("Relatar um problema")},text={Column {OutlinedTextField(value=reason,onValueChange={reason=it},label={Text("O que aconteceu?")});Text(status,fontSize=12.sp)}},confirmButton={TextButton(enabled=reason.isNotBlank(),onClick={if(server.isNotBlank()) scope.launch {try{Api.report(server,film.id,reason);status="Enviado."}catch(e:Exception){status="Não foi possível enviar. Tente novamente."}} else {context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/userpolopo-web/Stremlopo/issues/new?title="+android.net.Uri.encode("Problema: ${film.title}")+"&body="+android.net.Uri.encode("$reason\nOrigem: ${film.evidence}"))));report=false}}){Text(if(server.isBlank()) "Abrir relato" else "Enviar")}},dismissButton={TextButton(onClick={report=false}){Text("Cancelar")}})
}
