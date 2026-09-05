(() => {
  "use strict";

  const DB_NAME = "aero-player-db";
  const STORE = "tracks";
  let dbPromise = null;

  function openDb() {
    if (dbPromise) return dbPromise;
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, 2);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE, { keyPath: "id", autoIncrement: true });
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    return dbPromise;
  }

  function dbTx(mode, fn) {
    return openDb().then(db => new Promise((resolve, reject) => {
      const tx = db.transaction(STORE, mode);
      const store = tx.objectStore(STORE);
      let result;
      try { result = fn(store); } catch (e) { reject(e); return; }
      tx.oncomplete = () => resolve(result);
      tx.onerror = () => reject(tx.error);
      tx.onabort = () => reject(tx.error || new Error("IndexedDB transaction aborted"));
    }));
  }

  function dbGetAllTracks() {
    return dbTx("readonly", store => new Promise((resolve, reject) => {
      const req = store.getAll();
      req.onsuccess = () => resolve(req.result || []);
      req.onerror = () => reject(req.error);
    }));
  }
  function dbGetTrack(id) {
    return dbTx("readonly", store => new Promise((resolve, reject) => {
      const req = store.get(id);
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    }));
  }
  function dbPutTrack(record) { return dbTx("readwrite", store => store.put(record)); }
  function dbDeleteTrack(id) { return dbTx("readwrite", store => store.delete(id)); }

  async function dbAddTracks(files) {
    const ids = [];
    const db = await openDb();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE, "readwrite");
      const store = tx.objectStore(STORE);
      files.forEach(file => {
        const req = store.add({ name: file.name, type: file.type || "audio/mpeg", blob: file, addedAt: Date.now(), favorite: false });
        req.onsuccess = () => ids.push(req.result);
      });
      tx.oncomplete = () => resolve(ids);
      tx.onerror = () => reject(tx.error);
    });
  }

  function readTags(blob) {
    return new Promise(resolve => {
      if (!window.jsmediatags) return resolve({});
      window.jsmediatags.read(blob, {
        onSuccess: tag => {
          const t = (tag && tag.tags) || {};
          let coverBlob = null;
          if (t.picture) {
            try { coverBlob = new Blob([new Uint8Array(t.picture.data)], { type: t.picture.format || "image/jpeg" }); } catch (_) {}
          }
          resolve({ title: t.title || null, artist: t.artist || null, album: t.album || null, year: t.year || null, genre: t.genre || null, track: t.track || null, coverBlob });
        },
        onError: () => resolve({})
      });
    });
  }

  async function extractAndStoreTags(id) {
    const record = await dbGetTrack(id);
    if (!record) return false;
    const info = await readTags(record.blob);
    if (!info.title && !info.artist && !info.album && !info.year && !info.genre && !info.track && !info.coverBlob) return false;
    Object.entries(info).forEach(([k, v]) => {
      if (k !== "coverBlob" && v) record[k] = v;
    });
    if (info.coverBlob) record.cover = info.coverBlob;
    await dbPutTrack(record);
    return true;
  }

  // ---------- State ----------
  let playlist = [];
  let currentIndex = -1;
  let isShuffle = JSON.parse(localStorage.getItem("aero.shuffle") || "false");
  let repeatMode = localStorage.getItem("aero.repeat") || "off"; // off | all | one
  let shuffleOrder = [];
  let filterText = "";
  let sortMode = localStorage.getItem("aero.sort") || "added";
  let topFlex = Number(localStorage.getItem("aero.split") || 58);
  let onlinePauseFn = null; // set once the online music player is initialized

  // ---------- DOM ----------
  const $ = id => document.getElementById(id);
  const audioEl = $("audioEl"), playBtn = $("playBtn"), playIcon = $("playIcon"), prevBtn = $("prevBtn"), nextBtn = $("nextBtn");
  const shuffleBtn = $("shuffleBtn"), repeatBtn = $("repeatBtn"), dockAddBtn = $("addBtnEmpty");
  const trackTitle = $("trackTitle"), trackArtist = $("trackArtist"), progressTrack = $("progressTrack"), progressFill = $("progressFill");
  const timeCurrent = $("timeCurrent"), timeDuration = $("timeDuration"), volumeSlider = $("volumeSlider"), orbWrap = $("orbWrap");
  const playlistScroll = $("playlistScroll"), emptyState = $("emptyState"), playlistCount = $("playlistCount"), fileInput = $("fileInput"), toast = $("toast");
  const searchInput = $("searchInput"), sortSelect = $("sortSelect"), coverArtEl = $("coverArt"), coverInput = $("coverInput");
  const onlineWindow = $("onlineMusicWindow");

  let toastTimer = null;
  function showToast(msg) {
    toast.textContent = msg; toast.classList.add("show"); clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove("show"), 2600);
  }
  function formatTime(sec) { if (!isFinite(sec) || sec < 0) sec = 0; return `${Math.floor(sec / 60)}:${Math.floor(sec % 60).toString().padStart(2, "0")}`; }
  function niceTitle(name) { return name.replace(/\.[^/.]+$/, ""); }

  // ---------- Startup chime ----------
  let chimePlayed = false;
  function playStartupChime() {
    if (chimePlayed) return;
    try {
      const Ctx = window.AudioContext || window.webkitAudioContext; if (!Ctx) return;
      const ctx = new Ctx(); ctx.resume?.();
      const master = ctx.createGain(); master.gain.value = .22; master.connect(ctx.destination);
      [523.25, 659.25, 783.99, 1046.5].forEach((freq, i) => {
        const t = ctx.currentTime + i * .16, o = ctx.createOscillator(), g = ctx.createGain(); o.type = "sine"; o.frequency.value = freq;
        g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(.4, t + .04); g.gain.exponentialRampToValueAtTime(.001, t + 1.2);
        o.connect(g); g.connect(master); o.start(t); o.stop(t + 1.3);
      });
      chimePlayed = true;
    } catch (_) {}
  }
  playStartupChime();
  document.addEventListener("pointerdown", () => { if (!chimePlayed) playStartupChime(); }, { once: true, capture: true });

  // ---------- Desktop/window manager ----------
  const desktop = $("desktop"), taskbarButtons = $("taskbarButtons"), taskbarClock = $("taskbarClock"), ambientEl = $("ambientEl");
  let openWindowCount = 0;
  const taskbarBtnByWindow = new Map();
  function updateTaskbarClock() { const d = new Date(); taskbarClock.textContent = `${String(d.getHours()).padStart(2,"0")}:${String(d.getMinutes()).padStart(2,"0")}`; }
  updateTaskbarClock(); setInterval(updateTaskbarClock, 15000);
  function playAmbient() { if (openWindowCount === 0) ambientEl.play().catch(() => {}); }
  function pauseAmbient() { ambientEl.pause(); }
  function makeTaskbarButton(win) {
    const b = document.createElement("button"); b.className = "taskbar-app-btn active"; b.textContent = win.dataset.title || "Ventana";
    b.addEventListener("click", () => win.classList.contains("open") ? minimizeWindow(win) : openWindow(win)); return b;
  }
  function openWindow(win) {
    playStartupChime();
    if (!win.classList.contains("open")) { openWindowCount++; pauseAmbient(); }
    win.classList.add("open");
    let b = taskbarBtnByWindow.get(win); if (!b) { b = makeTaskbarButton(win); taskbarBtnByWindow.set(win,b); taskbarButtons.appendChild(b); }
    b.classList.add("active");
  }
  function minimizeWindow(win) {
    if (win.classList.contains("open")) openWindowCount = Math.max(0, openWindowCount - 1);
    win.classList.remove("open"); const b = taskbarBtnByWindow.get(win); if (b) b.classList.remove("active"); if (!openWindowCount) playAmbient();
  }
  function toggleMaximize(win) { win.classList.toggle("restored"); }
  document.querySelectorAll(".window").forEach(win => {
    win.querySelector('[data-action="min"]').addEventListener("click", () => minimizeWindow(win));
    win.querySelector('[data-action="close"]').addEventListener("click", () => minimizeWindow(win));
    win.querySelector('[data-action="max"]').addEventListener("click", () => toggleMaximize(win));
  });
  $("appIcon").addEventListener("click", () => openWindow($("appWindow")));
  $("photosIcon").addEventListener("click", () => openWindow($("photoWindow")));
  $("onlineMusicIcon").addEventListener("click", () => openWindow(onlineWindow));
  $("settingsIcon").addEventListener("click", () => openWindow($("settingsWindow")));
  document.querySelectorAll(".desktop-icon.soon").forEach(btn => btn.addEventListener("click", () => showToast(`${btn.dataset.app}: próximamente`)));
  playAmbient(); document.addEventListener("pointerdown", () => playAmbient(), { once:true, capture:true });

  // ---------- Split ----------
  const splitContainer = $("splitContainer"), paneTop = $("paneTop"), paneBottom = $("paneBottom"), splitDivider = $("splitDivider");
  function applySplit() { topFlex = Math.max(25, Math.min(78, topFlex)); paneTop.style.flexBasis = `${topFlex}%`; paneBottom.style.flexBasis = `${100-topFlex}%`; splitDivider.setAttribute("aria-valuenow", String(Math.round(topFlex))); localStorage.setItem("aero.split", topFlex); }
  applySplit();
  let dragging = false;
  splitDivider.addEventListener("pointerdown", e => { dragging=true; splitDivider.setPointerCapture(e.pointerId); });
  splitDivider.addEventListener("pointermove", e => { if(!dragging) return; const r=splitContainer.getBoundingClientRect(); topFlex=(e.clientY-r.top)/r.height*100; applySplit(); });
  ["pointerup","pointercancel"].forEach(ev => splitDivider.addEventListener(ev, () => dragging=false));
  splitDivider.addEventListener("keydown", e => { if(e.key === "ArrowUp") { topFlex-=3; applySplit(); } if(e.key === "ArrowDown") { topFlex+=3; applySplit(); } });

  // ---------- Clock ----------
  const lockDate=$("lockDate"), lockTime=$("lockTime");
  const DIAS=["domingo","lunes","martes","miércoles","jueves","viernes","sábado"], MESES=["enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"];
  function updateClock(){ const d=new Date(), day=DIAS[d.getDay()]; lockDate.textContent=`${day[0].toUpperCase()+day.slice(1)} ${d.getDate()} de ${MESES[d.getMonth()]}`; lockTime.textContent=`${String(d.getHours()).padStart(2,"0")}:${String(d.getMinutes()).padStart(2,"0")}`; }
  updateClock(); setInterval(updateClock,15000);

  // ---------- Visualizer (decorative — deliberately does NOT hijack audioEl's
  // output via createMediaElementSource: that permanently reroutes native
  // playback through Web Audio, and if that context ever stays suspended —
  // common on mobile without extra interaction — the track goes silent while
  // still looking like it's playing. Not worth that risk for a bonus visual.) ----------
  const visualizer=$("visualizer"); let vizTimer=null;
  function startVisualizer(){
    if (vizTimer) return;
    const bars = visualizer.querySelectorAll("span");
    visualizer.classList.add("live");
    vizTimer = setInterval(() => { bars.forEach(bar => bar.style.height = `${18 + Math.random()*82}%`); }, 260);
  }
  function stopVisualizer(){
    clearInterval(vizTimer); vizTimer=null;
    visualizer.classList.remove("live");
    visualizer.querySelectorAll("span").forEach(b=>b.style.height="10%");
  }

  // ---------- Playlist/library ----------
  function filteredTracks(){
    const q=filterText.trim().toLocaleLowerCase();
    let arr=playlist.filter(t=>!q || [t.title,t.artist,t.album,t.name].filter(Boolean).join(" ").toLocaleLowerCase().includes(q));
    if(sortMode==="title") arr.sort((a,b)=>(a.title||niceTitle(a.name)).localeCompare(b.title||niceTitle(b.name),"es"));
    if(sortMode==="artist") arr.sort((a,b)=>(a.artist||"").localeCompare(b.artist||"","es"));
    if(sortMode==="album") arr.sort((a,b)=>(a.album||"").localeCompare(b.album||"","es"));
    if(sortMode==="favorite") arr.sort((a,b)=>Number(b.favorite)-Number(a.favorite));
    if(sortMode==="added") arr.sort((a,b)=>(a.addedAt||0)-(b.addedAt||0));
    return arr;
  }
  function renderPlaylist(){
    playlistCount.textContent=`${playlist.length} canción${playlist.length===1?"":"es"}`; playlistScroll.innerHTML="";
    const tracks=filteredTracks();
    if(!tracks.length){ const e=emptyState.cloneNode(true); e.style.display="flex"; e.querySelector("div")?.replaceChildren(document.createTextNode(playlist.length?"No hay coincidencias.":"Aún no tienes canciones aquí. Agrega archivos de música de tu dispositivo.")); playlistScroll.appendChild(e); return; }
    tracks.forEach((track, visualIndex)=>{
      const realIndex=playlist.indexOf(track), row=document.createElement("div"); row.className="track-row"+(realIndex===currentIndex?" playing":""); row.tabIndex=0;
      const num=document.createElement("div"); num.className="track-num"; num.textContent=realIndex===currentIndex?"♪":String(visualIndex+1); row.appendChild(num);
      const thumb=document.createElement("div"); thumb.className="track-thumb"; if(track.coverUrl) thumb.style.backgroundImage=`url("${track.coverUrl}")`; else thumb.classList.add(`buddy-${visualIndex%5}`,"no-cover"); row.appendChild(thumb);
      const info=document.createElement("div"); info.className="track-info"; const name=document.createElement("div"), sub=document.createElement("div"); name.className="name"; sub.className="sub"; name.textContent=track.title||niceTitle(track.name); sub.textContent=track.artist||(track.album?track.album:"Archivo local"); info.append(name,sub); row.appendChild(info);
      const actions=document.createElement("div"); actions.className="track-actions";
      const fav=document.createElement("button"); fav.className="track-action"; fav.title=track.favorite?"Quitar de favoritos":"Añadir a favoritos"; fav.textContent=track.favorite?"★":"☆"; fav.setAttribute("aria-label",fav.title); fav.addEventListener("click",e=>{e.stopPropagation(); toggleFavorite(track);});
      const cover=document.createElement("button"); cover.className="track-action"; cover.title="Cambiar portada"; cover.textContent="▣"; cover.setAttribute("aria-label",cover.title); cover.addEventListener("click",e=>{e.stopPropagation(); pendingCoverId=track.id; coverInput.click();});
      const del=document.createElement("button"); del.className="track-action danger"; del.title="Eliminar canción"; del.textContent="×"; del.setAttribute("aria-label",del.title); del.addEventListener("click",e=>{e.stopPropagation(); deleteTrack(track.id);});
      actions.append(fav,cover,del); row.appendChild(actions);
      row.addEventListener("click",()=>playTrackAt(realIndex)); row.addEventListener("keydown",e=>{if(e.key==="Enter"||e.key===" "){e.preventDefault();playTrackAt(realIndex);}}); playlistScroll.appendChild(row);
    });
  }
  async function toggleFavorite(track){ track.favorite=!track.favorite; const r=await dbGetTrack(track.id); if(r){r.favorite=track.favorite;await dbPutTrack(r);} renderPlaylist(); }
  let pendingCoverId=null;
  coverInput?.addEventListener("change",async e=>{ const f=e.target.files?.[0]; if(!f||!pendingCoverId)return; const r=await dbGetTrack(pendingCoverId); if(r){r.cover=f;await dbPutTrack(r);showToast("Portada actualizada");await loadPlaylistFromDb();} pendingCoverId=null; coverInput.value=""; });

  function updateNowPlayingUI(){
    if(currentIndex<0||!playlist[currentIndex]){trackTitle.textContent="Sin música cargada";trackArtist.textContent="Agrega canciones para empezar";orbWrap.classList.remove("has-cover");coverArtEl.removeAttribute("src");return;}
    const t=playlist[currentIndex]; trackTitle.textContent=t.title||niceTitle(t.name); trackArtist.textContent=t.artist?(t.album?`${t.artist} — ${t.album}`:t.artist):(t.album||"Reproduciendo desde tu dispositivo");
    if(t.coverUrl){coverArtEl.src=t.coverUrl;orbWrap.classList.add("has-cover");}else{coverArtEl.removeAttribute("src");orbWrap.classList.remove("has-cover");}
  }
  function setPlayingIconState(playing){orbWrap.classList.toggle("spinning",playing);playIcon.innerHTML=playing?'<path d="M7 5h4v14H7zM13 5h4v14h-4z"/>':'<path d="M8 5v14l12-7z"/>';}

  // ---------- Playback ----------
  function playTrackAt(idx){ if(idx<0||idx>=playlist.length)return; if(onlinePauseFn)onlinePauseFn(); currentIndex=idx; const t=playlist[idx]; audioEl.src=t.url; audioEl.play().catch(err=>{showToast("No se pudo reproducir este archivo");console.warn(err);}); updateNowPlayingUI();renderPlaylist(); updateMediaSession(); }
  function togglePlayPause(){ if(currentIndex<0){if(playlist.length)playTrackAt(0);return;} if(audioEl.paused)audioEl.play().catch(()=>showToast("El navegador bloqueó la reproducción"));else audioEl.pause(); }
  function getNextIndex(){if(!playlist.length)return-1;if(isShuffle){if(!shuffleOrder.length)rebuildShuffleOrder();const p=shuffleOrder.indexOf(currentIndex);return shuffleOrder[(p+1)%shuffleOrder.length];}return currentIndex<playlist.length-1?currentIndex+1:0;}
  function getPrevIndex(){if(!playlist.length)return-1;if(isShuffle){if(!shuffleOrder.length)rebuildShuffleOrder();const p=shuffleOrder.indexOf(currentIndex);return shuffleOrder[(p-1+shuffleOrder.length)%shuffleOrder.length];}return(currentIndex-1+playlist.length)%playlist.length;}
  function rebuildShuffleOrder(){shuffleOrder=playlist.map((_,i)=>i);for(let i=shuffleOrder.length-1;i>0;i--){const j=Math.floor(Math.random()*(i+1));[shuffleOrder[i],shuffleOrder[j]]=[shuffleOrder[j],shuffleOrder[i]];}}
  audioEl.addEventListener("play",()=>{if(onlinePauseFn)onlinePauseFn();setPlayingIconState(true);startVisualizer();updateMediaSession();});
  audioEl.addEventListener("pause",()=>{setPlayingIconState(false);stopVisualizer();updateMediaSession();});
  audioEl.addEventListener("timeupdate",()=>{if(!audioEl.duration)return;const pct=audioEl.currentTime/audioEl.duration*100;progressFill.style.width=`${pct}%`;timeCurrent.textContent=formatTime(audioEl.currentTime);});
  audioEl.addEventListener("loadedmetadata",()=>timeDuration.textContent=formatTime(audioEl.duration));
  audioEl.addEventListener("error",()=>{showToast("No se pudo leer el archivo de audio");});
  audioEl.addEventListener("ended",()=>{if(repeatMode==="one"){audioEl.currentTime=0;audioEl.play().catch(()=>{});return;}if(currentIndex===playlist.length-1&&repeatMode==="off"){setPlayingIconState(false);return;}const n=getNextIndex();if(n>=0)playTrackAt(n);});
  playBtn.addEventListener("click",togglePlayPause); nextBtn.addEventListener("click",()=>{const n=getNextIndex();if(n>=0)playTrackAt(n);}); prevBtn.addEventListener("click",()=>{if(audioEl.currentTime>3){audioEl.currentTime=0;return;}const p=getPrevIndex();if(p>=0)playTrackAt(p);});
  function setShuffle(v){isShuffle=v;localStorage.setItem("aero.shuffle",JSON.stringify(v));shuffleBtn.classList.toggle("active",v);if(v)rebuildShuffleOrder();shuffleBtn.setAttribute("aria-pressed",String(v));}
  shuffleBtn.addEventListener("click",()=>setShuffle(!isShuffle));
  function setRepeat(mode){repeatMode=mode;localStorage.setItem("aero.repeat",mode);repeatBtn.classList.toggle("active",mode!=="off");repeatBtn.dataset.mode=mode;repeatBtn.textContent="";repeatBtn.innerHTML=mode==="one"?"1":"↻";repeatBtn.setAttribute("aria-label",mode==="off"?"Repetición desactivada":mode==="all"?"Repetir lista":"Repetir canción");}
  repeatBtn.addEventListener("click",()=>setRepeat(repeatMode==="off"?"all":repeatMode==="all"?"one":"off"));
  setShuffle(isShuffle);setRepeat(repeatMode);
  function seek(e){if(!audioEl.duration)return;const r=progressTrack.getBoundingClientRect();audioEl.currentTime=Math.max(0,Math.min(1,(e.clientX-r.left)/r.width))*audioEl.duration;}
  progressTrack.addEventListener("click",seek); progressTrack.addEventListener("keydown",e=>{if(e.key==="ArrowLeft"){audioEl.currentTime=Math.max(0,audioEl.currentTime-5);}if(e.key==="ArrowRight"){audioEl.currentTime=Math.min(audioEl.duration||0,audioEl.currentTime+5);}});
  let savedVolume=Number(localStorage.getItem("aero.volume")); if(!Number.isFinite(savedVolume))savedVolume=80; volumeSlider.value=savedVolume;audioEl.volume=savedVolume/100;volumeSlider.addEventListener("input",()=>{audioEl.volume=Number(volumeSlider.value)/100;localStorage.setItem("aero.volume",volumeSlider.value);});

  // ---------- Add music / duplicate detection / drag & drop ----------
  async function addFiles(files){ const audioFiles=Array.from(files).filter(f=>f.type.startsWith("audio/")||/\.(mp3|wav|ogg|m4a|aac|flac|opus)$/i.test(f.name)); if(!audioFiles.length){showToast("No encontré archivos de audio");return;} const existing=await dbGetAllTracks();const signatures=new Set(existing.map(r=>`${r.name}|${r.blob?.size||0}`));const fresh=audioFiles.filter(f=>!signatures.has(`${f.name}|${f.size}`)); if(!fresh.length){showToast("Esas canciones ya están en tu biblioteca");return;}const ids=await dbAddTracks(fresh);showToast(`${fresh.length} canción${fresh.length===1?"":"es"} agregada${fresh.length===1?"":"s"}`);await loadPlaylistFromDb();if(ids.length){showToast("Leyendo metadatos y portadas…");Promise.all(ids.map(extractAndStoreTags)).then(async()=>loadPlaylistFromDb());}}
  dockAddBtn.addEventListener("click",()=>fileInput.click()); fileInput.addEventListener("change",e=>{addFiles(e.target.files);fileInput.value="";});
  [playlistScroll,$("paneBottom")].forEach(el=>{el.addEventListener("dragover",e=>{e.preventDefault();el.classList.add("drop-target")});el.addEventListener("dragleave",()=>el.classList.remove("drop-target"));el.addEventListener("drop",e=>{e.preventDefault();el.classList.remove("drop-target");addFiles(e.dataTransfer.files);});});
  $("folderBtn")?.addEventListener("click",()=>{if("showDirectoryPicker" in window) window.showDirectoryPicker().then(async dir=>{const files=[];for await(const h of dir.values())if(h.kind==="file"){const f=await h.getFile();if(f.type.startsWith("audio/"))files.push(f);}addFiles(files);}).catch(()=>{});else showToast("Tu navegador no admite selección de carpetas");});

  async function deleteTrack(id){ const idx=playlist.findIndex(t=>t.id===id); if(idx<0)return; if(!confirm(`¿Eliminar “${playlist[idx].title||niceTitle(playlist[idx].name)}” de Aero Player?`))return; const wasCurrent=idx===currentIndex;await dbDeleteTrack(id);if(wasCurrent){audioEl.pause();audioEl.removeAttribute("src");currentIndex=-1;}else if(idx<currentIndex)currentIndex--;await loadPlaylistFromDb();showToast("Canción eliminada"); }
  $("clearLibraryBtn")?.addEventListener("click",async()=>{if(!playlist.length)return;if(!confirm("¿Eliminar toda la biblioteca local de Aero Player?"))return;for(const t of playlist)await dbDeleteTrack(t.id);audioEl.pause();audioEl.removeAttribute("src");currentIndex=-1;await loadPlaylistFromDb();showToast("Biblioteca vaciada");});

  // ---------- Search/sort ----------
  searchInput?.addEventListener("input",()=>{filterText=searchInput.value;renderPlaylist();}); sortSelect?.addEventListener("change",()=>{sortMode=sortSelect.value;localStorage.setItem("aero.sort",sortMode);renderPlaylist();}); if(sortSelect)sortSelect.value=sortMode;

  // ---------- Metadata backup (metadata only, audio stays local) ----------
  $("exportBtn")?.addEventListener("click",async()=>{const data=await dbGetAllTracks();const clean=data.map(({blob,cover,...r})=>({...r}));const a=document.createElement("a");a.href=URL.createObjectURL(new Blob([JSON.stringify({app:"Aero Player",version:2,tracks:clean},null,2)],{type:"application/json"}));a.download="aero-player-library.json";a.click();setTimeout(()=>URL.revokeObjectURL(a.href),1000);showToast("Biblioteca exportada (metadatos)");});
  $("importBtn")?.addEventListener("click",()=>$("libraryInput")?.click());
  $("libraryInput")?.addEventListener("change",async e=>{const f=e.target.files?.[0];if(!f)return;try{const data=JSON.parse(await f.text());showToast(`${Array.isArray(data.tracks)?data.tracks.length:0} registros encontrados. Los audios deben volver a agregarse desde el dispositivo.`);}catch(_){showToast("Archivo de biblioteca inválido");}e.target.value="";});

  // ---------- Música Online (búsqueda inspirada en Cantio + reproducción oficial de YouTube) ----------
  const DEFAULT_PIPED_INSTANCES = [
    "https://pipedapi.kavin.rocks",
    "https://piped-api.garudalinux.org",
    "https://pipedapi.leptons.xyz",
    "https://piped-api.lunar.icu",
    "https://pipedapi.colinslegacy.com"
  ];
  const onlineSearchInput = $("onlineSearchInput"), onlineSearchBtn = $("onlineSearchBtn");
  const onlineList = $("onlineList"), onlineEmptyState = $("onlineEmptyState"), onlineListLabel = $("onlineListLabel");
  const onlineShowLikesBtn = $("onlineShowLikesBtn");
  const onlineNowTitle = $("onlineNowTitle"), onlineNowArtist = $("onlineNowArtist");
  const onlineProgressTrack = $("onlineProgressTrack"), onlineProgressFill = $("onlineProgressFill");
  const onlineTimeCurrent = $("onlineTimeCurrent"), onlineTimeDuration = $("onlineTimeDuration");
  const onlinePrevBtn = $("onlinePrevBtn"), onlinePlayBtn = $("onlinePlayBtn"), onlineNextBtn = $("onlineNextBtn");
  const onlineRepeatBtn = $("onlineRepeatBtn"), onlineLikeBtn = $("onlineLikeBtn");
  const onlineSettingsToggle = $("onlineSettingsToggle"), onlineSettingsPanel = $("onlineSettingsPanel");
  const onlineInstanceInput = $("onlineInstanceInput"), onlineInstanceSaveBtn = $("onlineInstanceSaveBtn");

  let onlineResults = [];
  let onlineQueue = [];
  let onlineIndex = -1;
  let onlineRepeatMode = localStorage.getItem("aero.onlineRepeat") || "off"; // off | all | one
  let onlineShowingLikes = false;
  let onlinePlayer = null;
  let onlinePlayerReadyPromise = null;
  let onlineProgressTimer = null;
  let onlineLikes = [];
  try { onlineLikes = JSON.parse(localStorage.getItem("aero.onlineLikes") || "[]"); } catch(_) { onlineLikes = []; }

  function decodeHtml(str){ if(!str)return ""; const ta=document.createElement("textarea"); ta.innerHTML=str; return ta.value; }
  function getPipedBase(){ return localStorage.getItem("aero.pipedInstance") || null; }
  if (onlineInstanceInput) onlineInstanceInput.value = getPipedBase() || "";

  function pipedCandidates(){ const custom = getPipedBase(); return custom ? [custom, ...DEFAULT_PIPED_INSTANCES.filter(i=>i!==custom)] : DEFAULT_PIPED_INSTANCES; }

  async function fetchWithTimeout(url, ms){
    const ctrl = new AbortController(); const t = setTimeout(()=>ctrl.abort(), ms);
    try { const res = await fetch(url, { signal: ctrl.signal }); clearTimeout(t); if(!res.ok) throw new Error(`HTTP ${res.status}`); return await res.json(); }
    finally { clearTimeout(t); }
  }

  async function searchOnline(query){
    const q = query.trim(); if(!q) return;
    onlineListLabel.textContent = "Buscando…"; onlineShowingLikes = false;
    onlineList.innerHTML = ""; onlineEmptyState.style.display = "none";
    for (const base of pipedCandidates()){
      try {
        let data = await fetchWithTimeout(`${base}/search?q=${encodeURIComponent(q)}&filter=music_songs`, 7000);
        let items = Array.isArray(data.items) ? data.items.filter(it=>it.type==="stream" || it.url) : [];
        if (!items.length) { data = await fetchWithTimeout(`${base}/search?q=${encodeURIComponent(q)}&filter=videos`, 7000); items = Array.isArray(data.items) ? data.items.filter(it=>it.type==="stream" || it.url) : []; }
        onlineResults = items.map(it=>{
          const m = (it.url||"").match(/[?&]v=([\w-]{6,})/); const videoId = m ? m[1] : (it.url||"").split("/").pop();
          return { videoId, title: decodeHtml(it.title||"Sin título"), uploader: decodeHtml(it.uploaderName||it.uploader||""), duration: it.duration||0, thumbnail: it.thumbnail||"" };
        }).filter(t=>t.videoId);
        if (onlineResults.length){ onlineListLabel.textContent = "Resultados"; renderOnlineList(onlineResults); return; }
      } catch(_) { /* try next instance */ }
    }
    onlineListLabel.textContent = "Resultados";
    onlineList.innerHTML = ""; onlineEmptyState.textContent = "No encontré resultados (o el servidor de búsqueda no respondió). Puedes cambiarlo en ⚙."; onlineEmptyState.style.display = "block"; onlineList.appendChild(onlineEmptyState);
    showToast("No se pudo buscar. Prueba con otro servidor en ⚙");
  }

  function isLiked(videoId){ return onlineLikes.some(t=>t.videoId===videoId); }
  function saveLikes(){ localStorage.setItem("aero.onlineLikes", JSON.stringify(onlineLikes)); }
  function toggleLike(track){
    const i = onlineLikes.findIndex(t=>t.videoId===track.videoId);
    if (i>=0) onlineLikes.splice(i,1); else onlineLikes.unshift({videoId:track.videoId,title:track.title,uploader:track.uploader,duration:track.duration,thumbnail:track.thumbnail});
    saveLikes();
    if (onlineIndex>=0 && onlineQueue[onlineIndex] && onlineQueue[onlineIndex].videoId===track.videoId) onlineLikeBtn.textContent = isLiked(track.videoId) ? "★" : "☆";
    if (onlineShowingLikes) renderOnlineList(onlineLikes);
  }

  function renderOnlineList(items){
    onlineList.innerHTML = "";
    if (!items.length){ onlineEmptyState.textContent = onlineShowingLikes ? "Aún no tienes canciones favoritas online." : "Busca una canción o artista para empezar a escuchar, sin anuncios de por medio."; onlineEmptyState.style.display = "block"; onlineList.appendChild(onlineEmptyState); return; }
    items.forEach((track, idx)=>{
      const row = document.createElement("div"); row.className = "onlinemusic-item" + (onlineIndex>=0 && onlineQueue[onlineIndex] && onlineQueue[onlineIndex].videoId===track.videoId ? " playing" : ""); row.tabIndex = 0;
      const thumb = document.createElement("div"); thumb.className = "onlinemusic-item-thumb"; if (track.thumbnail) thumb.style.backgroundImage = `url("${track.thumbnail}")`; row.appendChild(thumb);
      const info = document.createElement("div"); info.className = "onlinemusic-item-info";
      const name = document.createElement("div"); name.className = "name"; name.textContent = track.title;
      const sub = document.createElement("div"); sub.className = "sub"; sub.textContent = [track.uploader, track.duration ? formatTime(track.duration) : ""].filter(Boolean).join(" · ");
      info.append(name, sub); row.appendChild(info);
      const actions = document.createElement("div"); actions.className = "onlinemusic-item-actions";
      const like = document.createElement("button"); like.className = "track-action"; like.textContent = isLiked(track.videoId) ? "★" : "☆"; like.title = "Me gusta"; like.setAttribute("aria-label","Añadir o quitar de favoritos"); like.addEventListener("click", e=>{ e.stopPropagation(); toggleLike(track); like.textContent = isLiked(track.videoId) ? "★" : "☆"; });
      actions.appendChild(like); row.appendChild(actions);
      const play = ()=>playOnlineAt(items, idx);
      row.addEventListener("click", play); row.addEventListener("keydown", e=>{ if(e.key==="Enter"||e.key===" "){ e.preventDefault(); play(); } });
      onlineList.appendChild(row);
    });
  }

  onlineSearchBtn?.addEventListener("click", ()=>searchOnline(onlineSearchInput.value));
  onlineSearchInput?.addEventListener("keydown", e=>{ if(e.key==="Enter") searchOnline(onlineSearchInput.value); });
  onlineShowLikesBtn?.addEventListener("click", ()=>{ onlineShowingLikes = !onlineShowingLikes; onlineListLabel.textContent = onlineShowingLikes ? "Favoritos" : "Resultados"; renderOnlineList(onlineShowingLikes ? onlineLikes : onlineResults); });
  onlineSettingsToggle?.addEventListener("click", ()=>{ onlineSettingsPanel.hidden = !onlineSettingsPanel.hidden; });
  onlineInstanceSaveBtn?.addEventListener("click", ()=>{ const v = onlineInstanceInput.value.trim().replace(/\/$/,""); if (v) localStorage.setItem("aero.pipedInstance", v); else localStorage.removeItem("aero.pipedInstance"); showToast("Servidor de búsqueda guardado"); });

  // ---------- Reproductor oficial de YouTube (IFrame Player API) ----------
  function loadYtApiScript(){
    if (window.YT && window.YT.Player) return Promise.resolve();
    return new Promise(resolve=>{
      const prev = window.onYouTubeIframeAPIReady;
      window.onYouTubeIframeAPIReady = ()=>{ if (typeof prev === "function") prev(); resolve(); };
      const s = document.createElement("script"); s.src = "https://www.youtube.com/iframe_api"; document.head.appendChild(s);
    });
  }
  function ensureOnlinePlayer(){
    if (onlinePlayerReadyPromise) return onlinePlayerReadyPromise;
    onlinePlayerReadyPromise = loadYtApiScript().then(()=>new Promise(resolve=>{
      onlinePlayer = new YT.Player("onlinePlayerHost", {
        host: "https://www.youtube.com",
        playerVars: { controls: 0, disablekb: 1, fs: 0, modestbranding: 1, rel: 0, iv_load_policy: 3, playsinline: 1 },
        events: {
          onReady: ()=>resolve(onlinePlayer),
          onStateChange: onOnlinePlayerStateChange
        }
      });
    }));
    return onlinePlayerReadyPromise;
  }
  function setOnlinePlayIcon(playing){ onlinePlayBtn.textContent = playing ? "❚❚" : "▶"; }
  function startOnlineProgressTimer(){
    stopOnlineProgressTimer();
    onlineProgressTimer = setInterval(()=>{
      if (!onlinePlayer || typeof onlinePlayer.getDuration !== "function") return;
      const dur = onlinePlayer.getDuration() || 0, cur = onlinePlayer.getCurrentTime() || 0;
      onlineProgressFill.style.width = dur ? `${(cur/dur)*100}%` : "0%";
      onlineTimeCurrent.textContent = formatTime(cur); onlineTimeDuration.textContent = formatTime(dur);
    }, 500);
  }
  function stopOnlineProgressTimer(){ clearInterval(onlineProgressTimer); onlineProgressTimer = null; }
  function onOnlinePlayerStateChange(e){
    if (e.data === YT.PlayerState.PLAYING){ setOnlinePlayIcon(true); startOnlineProgressTimer(); updateOnlineMediaSession(); }
    else if (e.data === YT.PlayerState.PAUSED){ setOnlinePlayIcon(false); stopOnlineProgressTimer(); }
    else if (e.data === YT.PlayerState.ENDED){
      stopOnlineProgressTimer(); setOnlinePlayIcon(false);
      if (onlineRepeatMode === "one"){ onlinePlayer.seekTo(0, true); onlinePlayer.playVideo(); return; }
      if (onlineIndex === onlineQueue.length - 1 && onlineRepeatMode === "off") return;
      const n = onlineIndex < onlineQueue.length - 1 ? onlineIndex + 1 : 0;
      playOnlineAt(onlineQueue, n);
    }
  }
  async function playOnlineAt(list, idx){
    if (idx < 0 || idx >= list.length) return;
    audioEl.pause();
    onlineQueue = list; onlineIndex = idx; const track = list[idx];
    onlineNowTitle.textContent = track.title; onlineNowArtist.textContent = track.uploader || "Música online";
    onlineLikeBtn.textContent = isLiked(track.videoId) ? "★" : "☆";
    renderOnlineList(onlineShowingLikes ? onlineLikes : onlineResults);
    await ensureOnlinePlayer();
    onlinePlayer.loadVideoById(track.videoId);
    updateOnlineMediaSession();
  }
  function toggleOnlinePlayPause(){
    if (!onlinePlayer){ if (onlineIndex < 0 && onlineResults.length) playOnlineAt(onlineResults, 0); return; }
    const state = onlinePlayer.getPlayerState ? onlinePlayer.getPlayerState() : -1;
    if (state === YT.PlayerState.PLAYING) onlinePlayer.pauseVideo(); else onlinePlayer.playVideo();
  }
  function onlineNext(){ if (!onlineQueue.length) return; const n = onlineIndex < onlineQueue.length - 1 ? onlineIndex + 1 : 0; playOnlineAt(onlineQueue, n); }
  function onlinePrev(){ if (!onlineQueue.length) return; const p = onlineIndex > 0 ? onlineIndex - 1 : onlineQueue.length - 1; playOnlineAt(onlineQueue, p); }
  onlinePlayBtn?.addEventListener("click", toggleOnlinePlayPause);
  onlineNextBtn?.addEventListener("click", onlineNext);
  onlinePrevBtn?.addEventListener("click", onlinePrev);
  onlineRepeatBtn?.addEventListener("click", ()=>{
    onlineRepeatMode = onlineRepeatMode === "off" ? "all" : onlineRepeatMode === "all" ? "one" : "off";
    localStorage.setItem("aero.onlineRepeat", onlineRepeatMode);
    onlineRepeatBtn.classList.toggle("active", onlineRepeatMode !== "off");
    onlineRepeatBtn.textContent = onlineRepeatMode === "one" ? "1" : "↻";
  });
  if (onlineRepeatBtn){ onlineRepeatBtn.textContent = onlineRepeatMode === "one" ? "1" : "↻"; onlineRepeatBtn.classList.toggle("active", onlineRepeatMode !== "off"); }
  onlineLikeBtn?.addEventListener("click", ()=>{ if (onlineIndex>=0 && onlineQueue[onlineIndex]) toggleLike(onlineQueue[onlineIndex]); });
  onlineProgressTrack?.addEventListener("click", e=>{
    if (!onlinePlayer || !onlinePlayer.getDuration) return;
    const dur = onlinePlayer.getDuration(); if (!dur) return;
    const r = onlineProgressTrack.getBoundingClientRect();
    onlinePlayer.seekTo(Math.max(0, Math.min(1, (e.clientX - r.left) / r.width)) * dur, true);
  });
  onlinePauseFn = ()=>{ if (onlinePlayer && onlinePlayer.pauseVideo) onlinePlayer.pauseVideo(); };
  // Los controles del sistema (auriculares, pantalla de bloqueo) se enlazan en updateOnlineMediaSession(),
  // que se llama cada vez que cambia o arranca la canción online.
  function updateOnlineMediaSession(){
    if (!("mediaSession" in navigator) || onlineIndex < 0 || !onlineQueue[onlineIndex]) return;
    const t = onlineQueue[onlineIndex];
    navigator.mediaSession.metadata = new MediaMetadata({ title: t.title, artist: t.uploader || "Música Online", album: "Aero Player", artwork: t.thumbnail ? [{src:t.thumbnail, sizes:"512x512", type:"image/jpeg"}] : [] });
    [["play",toggleOnlinePlayPause],["pause",toggleOnlinePlayPause],["nexttrack",onlineNext],["previoustrack",onlinePrev]].forEach(([a,f])=>{ try{ navigator.mediaSession.setActionHandler(a,f); }catch(_){} });
  }

  // ---------- Personalización (fondo de escritorio + música ambiental) ----------
  // Guarda los archivos elegidos por el usuario en su propia base IndexedDB
  // (separada de la biblioteca de música) para que sobrevivan entre sesiones
  // sin tener que volver a subir el proyecto.
  const SETTINGS_DB = "aero-player-settings";
  let settingsDbPromise = null;
  function openSettingsDb() {
    if (settingsDbPromise) return settingsDbPromise;
    settingsDbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(SETTINGS_DB, 1);
      req.onupgradeneeded = () => {
        if (!req.result.objectStoreNames.contains("settings")) {
          req.result.createObjectStore("settings");
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    return settingsDbPromise;
  }
  function getSetting(key) {
    return openSettingsDb().then(db => new Promise((resolve, reject) => {
      const tx = db.transaction("settings", "readonly");
      const req = tx.objectStore("settings").get(key);
      req.onsuccess = () => resolve(req.result || null);
      req.onerror = () => reject(req.error);
    }));
  }
  function setSetting(key, value) {
    return openSettingsDb().then(db => new Promise((resolve, reject) => {
      const tx = db.transaction("settings", "readwrite");
      tx.objectStore("settings").put(value, key);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    }));
  }
  function deleteSetting(key) {
    return openSettingsDb().then(db => new Promise((resolve, reject) => {
      const tx = db.transaction("settings", "readwrite");
      tx.objectStore("settings").delete(key);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    }));
  }

  const DEFAULT_WALLPAPER = "assets/desktop-wallpaper.jpg";
  const DEFAULT_AMBIENT = "assets/desktop-ambient.mp3";
  let customWallpaperUrl = null;
  let customAmbientUrl = null;

  function applyWallpaper(blob) {
    if (customWallpaperUrl) URL.revokeObjectURL(customWallpaperUrl);
    if (blob) {
      customWallpaperUrl = URL.createObjectURL(blob);
      desktop.style.backgroundImage =
        `linear-gradient(180deg, rgba(4,25,45,0.28) 0%, rgba(4,25,45,0.02) 22%, rgba(4,25,45,0.05) 70%, rgba(4,25,45,0.35) 100%), url("${customWallpaperUrl}")`;
    } else {
      customWallpaperUrl = null;
      desktop.style.backgroundImage = "";
    }
  }
  function applyAmbient(blob) {
    const wasPlaying = !ambientEl.paused;
    if (customAmbientUrl) URL.revokeObjectURL(customAmbientUrl);
    if (blob) {
      customAmbientUrl = URL.createObjectURL(blob);
      ambientEl.src = customAmbientUrl;
    } else {
      customAmbientUrl = null;
      ambientEl.src = DEFAULT_AMBIENT;
    }
    if (wasPlaying) playAmbient();
  }

  (async function loadCustomSettings() {
    try {
      const [wallpaper, ambient] = await Promise.all([getSetting("wallpaper"), getSetting("ambient")]);
      if (wallpaper) applyWallpaper(wallpaper);
      if (ambient) applyAmbient(ambient);
    } catch (e) {
      console.warn("No se pudieron cargar las preferencias de personalización", e);
    }
  })();

  const wallpaperInput = $("wallpaperInput"), ambientInput = $("ambientInput");
  $("wallpaperPickBtn").addEventListener("click", () => wallpaperInput.click());
  $("ambientPickBtn").addEventListener("click", () => ambientInput.click());
  wallpaperInput.addEventListener("change", async e => {
    const f = e.target.files?.[0];
    if (!f) return;
    await setSetting("wallpaper", f);
    applyWallpaper(f);
    showToast("Fondo de escritorio actualizado");
    wallpaperInput.value = "";
  });
  ambientInput.addEventListener("change", async e => {
    const f = e.target.files?.[0];
    if (!f) return;
    await setSetting("ambient", f);
    applyAmbient(f);
    showToast("Música de fondo actualizada");
    ambientInput.value = "";
  });
  $("settingsResetBtn").addEventListener("click", async () => {
    await Promise.all([deleteSetting("wallpaper"), deleteSetting("ambient")]);
    applyWallpaper(null);
    applyAmbient(null);
    showToast("Se restablecieron el fondo y la música originales");
  });

  // ---------- Media Session ----------
  function updateMediaSession(){if(!("mediaSession" in navigator)||currentIndex<0||!playlist[currentIndex])return;const t=playlist[currentIndex];navigator.mediaSession.metadata=new MediaMetadata({title:t.title||niceTitle(t.name),artist:t.artist||"Aero Player",album:t.album||"Biblioteca local",artwork:t.coverUrl?[{src:t.coverUrl,sizes:"512x512",type:"image/jpeg"}]:[]});}
  if("mediaSession" in navigator){[["play",togglePlayPause],["pause",togglePlayPause],["nexttrack",()=>{const n=getNextIndex();if(n>=0)playTrackAt(n);}],["previoustrack",()=>{const p=getPrevIndex();if(p>=0)playTrackAt(p);}]].forEach(([a,f])=>{try{navigator.mediaSession.setActionHandler(a,f);}catch(_) {}});}

  // ---------- Keyboard shortcuts ----------
  document.addEventListener("keydown",e=>{if(e.target instanceof Element && e.target.matches("input,textarea,select"))return;if(e.code==="Space"){e.preventDefault();togglePlayPause();}else if(e.key==="ArrowRight"){if(audioEl.duration)audioEl.currentTime=Math.min(audioEl.duration,audioEl.currentTime+5);}else if(e.key==="ArrowLeft"){audioEl.currentTime=Math.max(0,audioEl.currentTime-5);}else if(e.key==="ArrowUp"){volumeSlider.value=Math.min(100,Number(volumeSlider.value)+5);volumeSlider.dispatchEvent(new Event("input"));}else if(e.key==="ArrowDown"){volumeSlider.value=Math.max(0,Number(volumeSlider.value)-5);volumeSlider.dispatchEvent(new Event("input"));}});

  // ---------- Load library ----------
  async function loadPlaylistFromDb(){
    try{
      const records=await dbGetAllTracks();
      const currentId = currentIndex>=0 && playlist[currentIndex] ? playlist[currentIndex].id : null;
      const prevById = new Map(playlist.map(t=>[t.id,t]));
      // Revoke every cover URL (cheap, no continuity to break) and every audio URL EXCEPT the
      // one currently loaded in <audio>. Revoking that one out from under active playback is
      // what caused the "progress bar keeps moving but there's no sound" bug: the element
      // is still reporting a playing state from its internal clock/buffer while the resource
      // behind its src has already been invalidated.
      playlist.forEach(t=>{ if(t.id!==currentId && t.url) URL.revokeObjectURL(t.url); if(t.coverUrl) URL.revokeObjectURL(t.coverUrl); });
      playlist=records.map(r=>{
        const prev = prevById.get(r.id);
        const url = (r.id===currentId && prev) ? prev.url : URL.createObjectURL(r.blob);
        return {id:r.id,name:r.name,type:r.type,blob:r.blob,addedAt:r.addedAt,title:r.title||null,artist:r.artist||null,album:r.album||null,year:r.year||null,genre:r.genre||null,track:r.track||null,favorite:!!r.favorite,url,coverUrl:r.cover?URL.createObjectURL(r.cover):null};
      });
      if(isShuffle)rebuildShuffleOrder();
      currentIndex = currentId!=null ? playlist.findIndex(t=>t.id===currentId) : -1;
      renderPlaylist();updateNowPlayingUI();
    }catch(e){console.error(e);showToast("No se pudo cargar la biblioteca local");}
  }

  if("serviceWorker" in navigator)window.addEventListener("load",()=>navigator.serviceWorker.register("sw.js").catch(()=>{}));
  loadPlaylistFromDb();
})();
