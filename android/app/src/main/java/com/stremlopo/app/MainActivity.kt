package com.stremlopo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFE0E0E0),onPrimary=Color.Black,background=Color(0xFF090909),surface=Color(0xFF191919),secondary=Color.Gray)) { Surface(modifier=Modifier.fillMaxSize()) { Stremlopo() } }
    } }
}
@Composable fun Stremlopo() {
    val ctx=LocalContext.current; val prefs=remember { ctx.getSharedPreferences("stremlopo",0) }; val scope=rememberCoroutineScope()
    var server by remember { mutableStateOf(prefs.getString("server","") ?: "") }
    var films by remember { mutableStateOf(emptyList<Film>()) }; var selected by remember { mutableStateOf<Film?>(null) }
    var search by remember { mutableStateOf("") }; var tab by remember { mutableStateOf("Início") }; var status by remember { mutableStateOf("Configure seu servidor para carregar o catálogo autorizado.") }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet()) }
    var history by remember { mutableStateOf(prefs.getStringSet("history",emptySet())!!.toSet()) }
    var busy by remember { mutableStateOf(false) }
    fun load() { scope.launch { busy=true; try { films=Api.catalog(server); prefs.edit().putString("server",server).apply(); status=if(films.isEmpty()) "Nenhum conteúdo aprovado disponível." else "${films.size} conteúdos autorizados" } catch(e: Exception) { status=e.message ?: "Falha ao carregar." } finally { busy=false } } }
    LaunchedEffect(Unit) { if(server.isNotBlank()) load() }
    val film=selected
    if(film!=null) {
        var fullscreen by remember(film.id) { mutableStateOf(false) }; var playing by remember(film.id) { mutableStateOf(false) }; var sourceIndex by remember(film.id) { mutableIntStateOf(0) }; var reason by remember { mutableStateOf("") }; var reportOpen by remember { mutableStateOf(false) }
        LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item { TextButton(onClick={selected=null}) { Text("← Catálogo") } }
            item { Text(film.title,style=MaterialTheme.typography.headlineMedium); Text("${film.year} • ${film.genre} • ${film.duration} min",color=Color.LightGray) }
            if(playing && !fullscreen) item { Video(film,film.sources[sourceIndex],Modifier.fillMaxWidth().height(240.dp)) { status=it }; TextButton(onClick={fullscreen=true}) { Text("Tela cheia") } }
            item { Text(film.synopsis); Text("Licença: ${film.license}\n${film.attribution}",color=Color.Gray) }
            item { Row { Button(enabled=film.sources.isNotEmpty(),onClick={playing=true;history=history+film.id;prefs.edit().putStringSet("history",history).apply()}) { Text("▶ Assistir") }; TextButton(onClick={favorites=if(film.id in favorites) favorites-film.id else favorites+film.id;prefs.edit().putStringSet("favorites",favorites).apply()}) { Text(if(film.id in favorites) "Remover da lista" else "+ Minha lista") } } }
            if(film.sources.size>1) item { Row { film.sources.forEachIndexed { index,_ -> TextButton(onClick={sourceIndex=index;playing=true}) { Text("Fonte ${index+1}") } } } }
            item { TextButton(onClick={reportOpen=true}) { Text("Denunciar direitos autorais") }; Text(status,color=Color.Gray) }
        }
        if(fullscreen) androidx.compose.ui.window.Dialog(onDismissRequest={fullscreen=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) { Column(Modifier.fillMaxSize().background(Color.Black)) { TextButton(onClick={fullscreen=false}) { Text("Sair da tela cheia") }; Video(film,film.sources[sourceIndex],Modifier.fillMaxWidth().weight(1f)) {status=it} } }
        if(reportOpen) AlertDialog(onDismissRequest={reportOpen=false},title={Text("Denunciar conteúdo")},text={OutlinedTextField(value=reason,onValueChange={reason=it},label={Text("Descreva o problema")})},confirmButton={TextButton(enabled=reason.isNotBlank(),onClick={scope.launch { try { Api.report(server,film.id,reason);status="Denúncia enviada.";reportOpen=false } catch(e:Exception){status=e.message ?: "Falha no envio"} }}){Text("Enviar")}},dismissButton={TextButton(onClick={reportOpen=false}){Text("Cancelar")}})
    } else {
        Column(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("STREMLOPO",style=MaterialTheme.typography.headlineLarge); Text("Cinema gratuito. Fontes autorizadas.",color=Color.Gray)
            Row { listOf("Início","Minha lista","Histórico","Servidor").forEach { label -> TextButton(onClick={tab=label}) { Text(label) } } }
            if(tab=="Servidor" || server.isBlank()) { OutlinedTextField(value=server,onValueChange={server=it},label={Text("Servidor HTTPS")},singleLine=true,modifier=Modifier.fillMaxWidth()); Button(enabled=!busy,onClick={load()}){Text("Conectar")} }
            OutlinedTextField(value=search,onValueChange={search=it},label={Text("Buscar título ou gênero")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(status,color=Color.Gray)
            val visible=films.filter { (tab!="Minha lista" || it.id in favorites) && (tab!="Histórico" || it.id in history) && (it.title.contains(search,true)||it.genre.contains(search,true)) }
            LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) { items(visible,key={it.id}) { f -> Card(onClick={selected=f},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp)) { Row(Modifier.padding(12.dp)) { AsyncImage(model=f.poster,contentDescription="Capa de ${f.title}",modifier=Modifier.width(88.dp).height(125.dp).background(Color.DarkGray)); Column(Modifier.padding(start=16.dp)) { Text(f.title,style=MaterialTheme.typography.titleLarge); Text("${f.genre} • ${f.year}",color=Color.Gray); Text(f.synopsis,maxLines=3); if(prefs.getLong("progress:${f.id}",0)>0) Text("Continuar assistindo",color=Color.LightGray) } } } } }
        }
    }
}
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable fun Video(film: Film,source: VideoSource,modifier: Modifier,onError:(String)->Unit) {
    val ctx=LocalContext.current; val prefs=remember {ctx.getSharedPreferences("stremlopo",0)}
    val player=remember(film.id,source.url) { ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(source.url)); seekTo(prefs.getLong("progress:${film.id}",0));prepare();playWhenReady=true } }
    DisposableEffect(player) {
        val listener=object:Player.Listener { override fun onPlayerError(error:androidx.media3.common.PlaybackException){onError("Fonte indisponível. Tente outra fonte ou avise a administração.")} }; player.addListener(listener)
        val lifecycle=(ctx as ComponentActivity).lifecycle
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event -> if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP){prefs.edit().putLong("progress:${film.id}",player.currentPosition).apply();player.pause()} }
        lifecycle.addObserver(observer)
        onDispose {prefs.edit().putLong("progress:${film.id}",player.currentPosition).apply();lifecycle.removeObserver(observer);player.removeListener(listener);player.release()}
    }
    AndroidView(factory={PlayerView(it).apply{this.player=player;useController=true}},update={it.player=player},modifier=modifier)
}
