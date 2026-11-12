import React, { useMemo, useRef, useState } from "react";

const iconPaths = {
  home: "M3 10.5 12 3l9 7.5M5.5 9v11h13V9M9 20v-6h6v6",
  photos:
    "M4 5.5A1.5 1.5 0 0 1 5.5 4h13A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5zM4 16l5-5 4 4 2-2 5 5M15.5 8.5h.01",
  albums:
    "M3 7.5h7l2 2H21v9.25A1.25 1.25 0 0 1 19.75 20H4.25A1.25 1.25 0 0 1 3 18.75zM3 7.5V5.25A1.25 1.25 0 0 1 4.25 4h4.5l2 2H19",
  shared:
    "M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M20 8v6M23 11h-6",
  heart:
    "M20.8 8.6c0 5.3-8.8 10.4-8.8 10.4S3.2 13.9 3.2 8.6A4.6 4.6 0 0 1 12 6.4a4.6 4.6 0 0 1 8.8 2.2Z",
  search: "m20 20-4.5-4.5M18 10.5a7.5 7.5 0 1 1-15 0 7.5 7.5 0 0 1 15 0Z",
  upload:
    "M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M4 15v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4",
  plus: "M12 5v14M5 12h14",
  chevron: "m9 18 6-6-6-6",
  settings:
    "M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7ZM12 3v2M12 19v2M3 12h2M19 12h2M5.64 5.64l1.42 1.42M16.94 16.94l1.42 1.42M18.36 5.64l-1.42 1.42M7.06 16.94l-1.42 1.42",
  storage: "M4 5h16v14H4zM4 9h16M8 15h.01M12 15h.01",
  devices: "M7 3h10v18H7zM10 6h4M11 18h2M3 7v10M21 7v10",
  help: "M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3M12 17h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
  logout:
    "M10 17l5-5-5-5M15 12H3M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6",
  bell:
    "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4",
  more: "M5 12h.01M12 12h.01M19 12h.01",
  edit: "m15 5 4 4M4 20l4-.8L19 8a2.1 2.1 0 0 0-3-3L5 16z",
};

function Icon({ name, size = 20, className = "" }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      height={size}
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.7"
      viewBox="0 0 24 24"
      width={size}
    >
      <path d={iconPaths[name]} />
    </svg>
  );
}

const albumTree = [
  {
    id: "family",
    name: "Familie",
    children: [
      { id: "family-2026", name: "Familienzeit", children: [] },
      { id: "children", name: "Kinder", children: [] },
    ],
  },
  {
    id: "travel",
    name: "Reisen",
    children: [
      {
        id: "tyrol",
        name: "Sommer in Tirol",
        children: [{ id: "mountain-lake", name: "Am See", children: [] }],
      },
      { id: "weekend", name: "Wochenendtrips", children: [] },
    ],
  },
  { id: "everyday", name: "Alltag", children: [] },
];

const initialPhotos = [
  {
    id: 1,
    title: "Morgen am See",
    albumId: "mountain-lake",
    date: "Heute",
    image:
      "https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1000&q=85",
    favorite: true,
  },
  {
    id: 2,
    title: "Ein ruhiger Weg",
    albumId: "everyday",
    date: "Gestern",
    image:
      "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
  },
  {
    id: 3,
    title: "Bergluft",
    albumId: "tyrol",
    date: "12. Oktober",
    image:
      "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1000&q=85",
    favorite: true,
  },
  {
    id: 4,
    title: "Goldene Stunde",
    albumId: "weekend",
    date: "10. Oktober",
    image:
      "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
    shared: true,
    sharedAlbumId: "weekend",
    sharedBy: "Lukas",
  },
  {
    id: 5,
    title: "Ein Nachmittag zusammen",
    albumId: "family-2026",
    date: "8. Oktober",
    image:
      "https://images.unsplash.com/photo-1511895426328-dc8714191300?auto=format&fit=crop&w=1000&q=85",
    favorite: true,
  },
  {
    id: 6,
    title: "Am Waldrand",
    albumId: "tyrol",
    date: "7. Oktober",
    image:
      "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
    shared: true,
    sharedAlbumId: "tyrol",
    sharedBy: "Mia",
  },
  {
    id: 7,
    title: "Kleine Entdecker",
    albumId: "children",
    date: "5. Oktober",
    image:
      "https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
  },
  {
    id: 8,
    title: "Bergpfad",
    albumId: "weekend",
    date: "3. Oktober",
    image:
      "https://images.unsplash.com/photo-1519681393784-d120267933ba?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
  },
  {
    id: 9,
    title: "Abendlicht",
    albumId: "everyday",
    date: "1. Oktober",
    image:
      "https://images.unsplash.com/photo-1473116763249-2faaef81ccda?auto=format&fit=crop&w=1000&q=85",
    favorite: true,
  },
  {
    id: 10,
    title: "Unser Lieblingsplatz",
    albumId: "family-2026",
    date: "28. September",
    image:
      "https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
    shared: true,
    sharedAlbumId: "family-2026",
    sharedBy: "Anna",
  },
  {
    id: 11,
    title: "Nebel im Tal",
    albumId: "tyrol",
    date: "24. September",
    image:
      "https://images.unsplash.com/photo-1511497584788-876760111969?auto=format&fit=crop&w=1000&q=85",
    favorite: false,
  },
  {
    id: 12,
    title: "Unterwegs",
    albumId: "weekend",
    date: "20. September",
    image:
      "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1000&q=85",
    favorite: true,
  },
];

const recentlySharedAlbums = [
  {
    id: "family-2026",
    name: "Familienzeit",
    sharedBy: "Anna",
    activity: "vor 2 Std. geteilt",
    image:
      "https://images.unsplash.com/photo-1511895426328-dc8714191300?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "weekend",
    name: "Wochenendtrips",
    sharedBy: "Lukas",
    activity: "gestern geteilt",
    image:
      "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=700&q=80",
  },
  {
    id: "tyrol",
    name: "Sommer in Tirol",
    sharedBy: "Mia",
    activity: "vor 3 Tagen geteilt",
    image:
      "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=700&q=80",
  },
];

const utilityActions = [
  { label: "Fotos hinzufügen", icon: "upload", upload: true },
  { label: "Speicher & Abo", icon: "storage" },
  { label: "Geräte & Sync", icon: "devices" },
  { label: "Einstellungen", icon: "settings" },
  { label: "Hilfe & Support", icon: "help" },
];

const latestNotification = {
  title: "Anna hat neue Fotos geteilt",
  detail: "Familienzeit · vor 2 Std.",
};

const adminMetrics = [
  { label: "Aktive Jobs", value: "2", change: "1 Upload · 1 Sync", icon: "upload" },
  { label: "Nutzer", value: "8", change: "6 aktiv · 2 offline", icon: "shared" },
  { label: "Aktive Share-Links", value: "3", change: "Alle erreichbar", icon: "albums" },
  { label: "Systemstatus", value: "Online", change: "Seit 14 Tagen aktiv", icon: "devices" },
];

const adminUsers = [
  { initials: "AK", name: "Anna Keller", email: "anna.keller@example.at", lastSeen: "Jetzt", status: "Online", color: "coral" },
  { initials: "LM", name: "Lukas Mayer", email: "lukas.mayer@example.at", lastSeen: "vor 4 Min.", status: "Synchronisiert", color: "amber" },
  { initials: "MS", name: "Mia Schneider", email: "mia.schneider@example.at", lastSeen: "vor 26 Min.", status: "Online", color: "sage" },
  { initials: "TB", name: "Tom Berger", email: "tom.berger@example.at", lastSeen: "gestern", status: "Offline", color: "plum" },
];

const adminJobs = [
  { id: "upload-anna", user: "Anna Keller", detail: "Lädt 186 Bilder hoch · Familienurlaub", amount: "1,2 GB", progress: 68, icon: "upload", color: "coral" },
  { id: "sync-lukas", user: "Lukas Mayer", detail: "Synchronisiert 248 Fotos", amount: "324 MB", progress: 42, icon: "devices", color: "amber" },
];

const adminShareLinks = [
  { album: "Familienurlaub", owner: "Anna Keller", access: "Link · Passwort", views: "12 Aufrufe", status: "Aktiv" },
  { album: "Sommer in Tirol", owner: "Mia Schneider", access: "Freunde", views: "8 Aufrufe", status: "Aktiv" },
  { album: "Wochenendtrip", owner: "Lukas Mayer", access: "Link", views: "3 Aufrufe", status: "Aktiv" },
];

const adminResources = [
  { label: "Prozessor", detail: "4 Kerne", value: "34 %", percent: 34, color: "cpu" },
  { label: "Arbeitsspeicher", detail: "10,9 von 16 GB", value: "68 %", percent: 68, color: "memory" },
  { label: "Speicherplatz", detail: "2,4 von 4 TB", value: "60 %", percent: 60, color: "disk" },
  { label: "Netzwerk", detail: "Upload / Download", value: "42 / 128 Mb/s", percent: 47, color: "network" },
];

function collectAlbumIds(album) {
  return [
    album.id,
    ...album.children.flatMap((child) => collectAlbumIds(child)),
  ];
}

function getAlbumIds(albumId, albums) {
  for (const candidate of albums) {
    if (candidate.id === albumId) {
      return collectAlbumIds(candidate);
    }

    const childIds = getAlbumIds(albumId, candidate.children);
    if (childIds.length) return childIds;
  }
  return [];
}

function getAlbumName(albumId, albums) {
  for (const album of albums) {
    if (album.id === albumId) return album.name;
    const childName = getAlbumName(albumId, album.children);
    if (childName) return childName;
  }
  return "";
}

function AlbumTree({ albums, depth = 0, onSelect, selectedAlbum }) {
  return albums.map((album) => {
    const hasChildren = album.children.length > 0;
    return (
      <div className="album-tree-branch" key={album.id}>
        <button
          aria-current={selectedAlbum === album.id ? "page" : undefined}
          className={`album-tree-item ${selectedAlbum === album.id ? "selected" : ""}`}
          onClick={() => onSelect(album.id)}
          style={{ "--tree-depth": depth }}
          type="button"
        >
          <Icon className="tree-folder" name="albums" size={16} />
          <span>{album.name}</span>
        </button>
        {hasChildren && (
          <div className="album-tree-children">
            <AlbumTree
              albums={album.children}
              depth={depth + 1}
              onSelect={onSelect}
              selectedAlbum={selectedAlbum}
            />
          </div>
        )}
      </div>
    );
  });
}

function PhotoCard({ photo, onFavorite }) {
  return (
    <article className="photo-card">
      <div className="photo-image-wrap">
        <img alt={photo.title} className="photo-image" loading="lazy" src={photo.image} />
        <button
          aria-label={
            photo.favorite
              ? `${photo.title} aus Favoriten entfernen`
              : `${photo.title} zu Favoriten hinzufügen`
          }
          className={`photo-favorite ${photo.favorite ? "is-favorite" : ""}`}
          onClick={() => onFavorite(photo.id)}
          type="button"
        >
          <Icon name="heart" size={18} />
        </button>
      </div>
    </article>
  );
}

function OverviewPhoto({ photo, onOpen, context }) {
  return (
    <button className="overview-photo" onClick={onOpen} type="button">
      <img alt="" loading="lazy" src={photo.image} />
      <span className="overview-photo-info">
        <strong>{photo.title}</strong>
        {context ? (
          <>
            <span>{context}</span>
            <span>{photo.date}</span>
          </>
        ) : (
          <span>{photo.date}</span>
        )}
      </span>
    </button>
  );
}

function SharedAlbumCard({ album, onOpen }) {
  return (
    <button
      className="shared-album-card"
      onClick={onOpen}
      style={{ "--album-cover": `url("${album.image}")` }}
      type="button"
    >
      <span aria-hidden="true" className="shared-album-cover" />
      <span className="shared-album-info">
        <strong>{album.name}</strong>
        <span>Von {album.sharedBy}</span>
        <span className="shared-album-activity">{album.activity}</span>
      </span>
      <Icon className="shared-album-chevron" name="chevron" size={16} />
    </button>
  );
}

function App() {
  const [photos, setPhotos] = useState(initialPhotos);
  const [isAdmin, setIsAdmin] = useState(false);
  const [search, setSearch] = useState("");
  const [activeView, setActiveView] = useState("overview");
  const [selectedAlbum, setSelectedAlbum] = useState(null);
  const [albumsOpen, setAlbumsOpen] = useState(false);
  const [notice, setNotice] = useState("");
  const uploadInput = useRef(null);

  const visiblePhotos = useMemo(() => {
    const query = search.trim().toLocaleLowerCase("de");
    const selectedAlbumIds = selectedAlbum
      ? getAlbumIds(selectedAlbum, albumTree)
      : null;

    return photos.filter((photo) => {
      const matchesSearch =
        !query || photo.title.toLocaleLowerCase("de").includes(query);
      const matchesAlbum =
        !selectedAlbumIds || selectedAlbumIds.includes(photo.albumId);
      const matchesView =
        activeView === "favorites"
          ? photo.favorite
          : activeView === "shared"
            ? photo.shared
            : true;

      return matchesSearch && matchesAlbum && matchesView;
    });
  }, [activeView, photos, search, selectedAlbum]);

  const showNotice = (message) => {
    setNotice(message);
    window.clearTimeout(showNotice.timeout);
    showNotice.timeout = window.setTimeout(() => setNotice(""), 3200);
  };

  const showAllPhotos = (view = "overview") => {
    setSelectedAlbum(null);
    setSearch("");
    setActiveView(view);
  };

  const openSharedAlbum = (albumId) => {
    setSelectedAlbum(albumId);
    setSearch("");
    setActiveView("shared");
  };

  const selectAlbum = (albumId) => {
    setSelectedAlbum(albumId);
    setSearch("");
    setActiveView("overview");
  };

  const toggleFavorite = (photoId) => {
    setPhotos((current) =>
      current.map((photo) =>
        photo.id === photoId ? { ...photo, favorite: !photo.favorite } : photo,
      ),
    );
  };

  const handleUpload = (event) => {
    const files = Array.from(event.target.files ?? []);
    if (!files.length) return;

    const newPhotos = files
      .filter((file) => file.type.startsWith("image/"))
      .map((file, index) => ({
        id: Date.now() + index,
        title: file.name.replace(/\.[^.]+$/, ""),
        albumId: "everyday",
        date: "Gerade eben",
        image: URL.createObjectURL(file),
        favorite: false,
      }));

    if (!newPhotos.length) {
      showNotice("Bitte wähle eine Bilddatei aus.");
    } else {
      setPhotos((current) => [...newPhotos, ...current]);
      showAllPhotos();
      showNotice(
        `${newPhotos.length} ${newPhotos.length === 1 ? "Foto" : "Fotos"} lokal hinzugefügt.`,
      );
    }

    event.target.value = "";
  };

  const handleUtilityAction = (label) => {
    showNotice(
      label === "Benachrichtigungen"
        ? "Du bist auf dem neuesten Stand."
        : `${label} ist in dieser Dashboard-Vorschau noch nicht verbunden.`,
    );
  };

  const pageTitle = selectedAlbum
    ? getAlbumName(selectedAlbum, albumTree)
    : search.trim()
      ? "Suchergebnisse"
      : activeView === "overview"
        ? "Übersicht"
        : activeView === "favorites"
          ? "Favoriten"
          : activeView === "shared"
            ? selectedAlbum
              ? getAlbumName(selectedAlbum, albumTree)
              : "Geteilte Alben"
            : "Alle Fotos";

  const showOverview = activeView === "overview" && !selectedAlbum && !search.trim();
  const recentPhotos = photos.slice(0, 4);
  const recentSharedPhotos = photos.filter((photo) => photo.shared).slice(0, 3);

  return (
    <div className="app-shell">
      <aside aria-label="Hauptnavigation" className="left-panel">
        <a
          aria-label="AirPX PhotoCloud Startseite"
          className="brand"
          href="#dashboard"
          onClick={() => showAllPhotos()}
        >
          <span aria-hidden="true" className="brand-bubbles">
            <i />
            <i />
            <i />
          </span>
          <span className="brand-name">
            airpx<span>cloud</span>
          </span>
        </a>

        {isAdmin ? (
          <nav aria-label="Administration" className="primary-nav">
            <div className="nav-item active">
              <Icon name="home" size={19} />
              <span>Admin-Dashboard</span>
            </div>
          </nav>
        ) : <nav aria-label="Bibliothek" className="primary-nav">
          <button
            aria-current={activeView === "overview" && !selectedAlbum ? "page" : undefined}
            className={`nav-item ${activeView === "overview" && !selectedAlbum ? "active" : ""}`}
            onClick={() => showAllPhotos()}
            type="button"
          >
            <Icon name="home" size={19} />
            <span>Übersicht</span>
          </button>
          <button
            aria-current={activeView === "photos" ? "page" : undefined}
            className={`nav-item ${activeView === "photos" ? "active" : ""}`}
            onClick={() => showAllPhotos("photos")}
            type="button"
          >
            <Icon name="photos" size={19} />
            <span>Fotos</span>
          </button>
          <button
            aria-current={activeView === "favorites" ? "page" : undefined}
            className={`nav-item ${activeView === "favorites" ? "active" : ""}`}
            onClick={() => showAllPhotos("favorites")}
            type="button"
          >
            <Icon name="heart" size={19} />
            <span>Favoriten</span>
          </button>
          <button
            aria-expanded={albumsOpen}
            className={`nav-item album-toggle ${albumsOpen || selectedAlbum ? "active" : ""}`}
            onClick={() => setAlbumsOpen((open) => !open)}
            type="button"
          >
            <Icon name="albums" size={19} />
            <span>Alben</span>
            <Icon
              className={`tree-chevron ${albumsOpen ? "expanded" : ""}`}
              name="chevron"
              size={15}
            />
          </button>
          {albumsOpen && (
            <div aria-label="Alben" className="album-tree">
              <AlbumTree
                albums={albumTree}
                onSelect={selectAlbum}
                selectedAlbum={selectedAlbum}
              />
            </div>
          )}
        </nav>}

        {!isAdmin && <div className="sidebar-bottom">
          <button
            aria-current={activeView === "shared" ? "page" : undefined}
            className={`nav-item share-link ${activeView === "shared" ? "active" : ""}`}
            onClick={() => showAllPhotos("shared")}
            type="button"
          >
            <Icon name="shared" size={19} />
            <span>Teilen</span>
            <span className="nav-count">{recentlySharedAlbums.length}</span>
          </button>
        </div>}
      </aside>

      <main className="main-panel" id="dashboard">
        <header className="topbar">
          <h1>{isAdmin ? "Admin-Dashboard" : pageTitle}</h1>
          <div className="topbar-actions">
            {!isAdmin && (
              <label className="search-field">
                <Icon name="search" size={18} />
                <input
                  aria-label="Fotos suchen"
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Fotos suchen"
                  type="search"
                  value={search}
                />
              </label>
            )}
            <div aria-label="Dashboard-Vorschau wechseln" className="role-switch" role="group">
              <button
                aria-pressed={!isAdmin}
                className={!isAdmin ? "selected" : ""}
                onClick={() => setIsAdmin(false)}
                type="button"
              >
                Nutzer
              </button>
              <button
                aria-pressed={isAdmin}
                className={isAdmin ? "selected" : ""}
                onClick={() => setIsAdmin(true)}
                type="button"
              >
                Admin
              </button>
            </div>
          </div>
        </header>

        {isAdmin ? (
          <section aria-label="Admin-Übersicht" className="admin-content">
            <div className="admin-welcome">
              <div>
                <p>PRIVATER PHOTO-CLOUD-HOST</p>
                <h2>Dein Server läuft.</h2>
                <span>Systemstatus und aktuelle Aktivitäten auf einen Blick.</span>
              </div>
              <span className="host-online"><i /> Online <span>· seit 14 Tagen</span></span>
            </div>

            <div className="admin-metrics">
              {adminMetrics.map((metric) => (
                <article className="admin-metric-card" key={metric.label}>
                  <span className="admin-metric-icon">
                    <Icon name={metric.icon} size={19} />
                  </span>
                  <span className="admin-metric-label">{metric.label}</span>
                  <strong>{metric.value}</strong>
                  <span className="admin-metric-change">{metric.change}</span>
                </article>
              ))}
            </div>

            <div className="admin-overview-grid">
              <section aria-labelledby="admin-jobs-title" className="admin-panel admin-jobs-panel">
                <div className="admin-panel-heading">
                  <div>
                    <h2 id="admin-jobs-title">Aktuelle Jobs</h2>
                    <span>Laufende Uploads und Synchronisierungen</span>
                  </div>
                  <span className="admin-panel-count">{adminJobs.length} aktiv</span>
                </div>
                <div className="admin-jobs-list">
                  {adminJobs.map((job) => (
                    <div className="admin-job-row" key={job.id}>
                      <span className={`admin-job-icon ${job.color}`}>
                        <Icon name={job.icon} size={17} />
                      </span>
                      <span className="admin-job-main">
                        <span className="admin-job-heading">
                          <strong>{job.user}</strong>
                          <span>{job.amount}</span>
                        </span>
                        <span className="admin-job-detail">{job.detail}</span>
                        <span
                          aria-label={`${job.progress} Prozent abgeschlossen`}
                          className="admin-job-progress"
                          role="progressbar"
                          aria-valuemax="100"
                          aria-valuemin="0"
                          aria-valuenow={job.progress}
                        >
                          <i className={job.color} style={{ width: `${job.progress}%` }} />
                        </span>
                        <span className="admin-job-percent">{job.progress}% abgeschlossen</span>
                      </span>
                    </div>
                  ))}
                </div>
              </section>

              <section aria-labelledby="admin-shares-title" className="admin-panel admin-shares-panel">
                <div className="admin-panel-heading">
                  <div>
                    <h2 id="admin-shares-title">Aktive Share-Links</h2>
                    <span>Freigaben auf deinem Host</span>
                  </div>
                  <span className="admin-panel-count">{adminShareLinks.length} aktiv</span>
                </div>
                <div className="admin-share-list">
                  {adminShareLinks.map((share) => (
                    <div className="admin-share-row" key={share.album}>
                      <span className="admin-share-icon"><Icon name="shared" size={16} /></span>
                      <span className="admin-share-main">
                        <strong>{share.album}</strong>
                        <span>{share.owner} · {share.access}</span>
                      </span>
                      <span className="admin-share-meta">
                        <strong>{share.views}</strong>
                        <span><i />{share.status}</span>
                      </span>
                    </div>
                  ))}
                </div>
              </section>

              <section aria-labelledby="admin-resources-title" className="admin-panel admin-resources-panel">
                <div className="admin-panel-heading">
                  <div>
                    <h2 id="admin-resources-title">Host-Auslastung</h2>
                    <span>Ressourcenverbrauch in Echtzeit</span>
                  </div>
                  <Icon name="devices" size={19} />
                </div>
                <div className="admin-resource-list">
                  {adminResources.map((resource) => (
                    <div className="admin-resource-row" key={resource.label}>
                      <span className="admin-resource-heading">
                        <strong>{resource.label}</strong>
                        <span>{resource.value}</span>
                      </span>
                      <span className="admin-resource-detail">{resource.detail}</span>
                      <span
                        aria-label={`${resource.label}: ${resource.value}`}
                        className="admin-resource-track"
                        role="progressbar"
                        aria-valuemax="100"
                        aria-valuemin="0"
                        aria-valuenow={resource.percent}
                      >
                        <i className={resource.color} style={{ width: `${resource.percent}%` }} />
                      </span>
                    </div>
                  ))}
                </div>
              </section>
            </div>

            <section aria-labelledby="admin-users-title" className="admin-panel admin-users-panel">
              <div className="admin-panel-heading">
                <div>
                  <h2 id="admin-users-title">Nutzer</h2>
                  <span>Konten und letzter bekannter Status</span>
                </div>
                <span className="admin-panel-count">8 Konten</span>
              </div>
              <div className="admin-user-list">
                {adminUsers.map((user) => (
                  <div className="admin-user-row" key={user.email}>
                    <span className={`admin-user-avatar ${user.color}`}>{user.initials}</span>
                    <span className="admin-user-details">
                      <strong>{user.name}</strong>
                      <span>{user.email}</span>
                    </span>
                    <span className="admin-user-date">{user.lastSeen}</span>
                    <span className={`admin-user-status ${user.status === "Offline" ? "offline" : user.status === "Synchronisiert" ? "syncing" : ""}`}>
                      {user.status}
                    </span>
                  </div>
                ))}
              </div>
            </section>
          </section>
        ) : activeView === "shared" && !selectedAlbum && !search.trim() ? (
          <section aria-labelledby="all-shared-albums-title" className="shared-library">
            <div className="shared-library-heading">
              <h2 id="all-shared-albums-title">Alle geteilten Alben</h2>
              <span>{recentlySharedAlbums.length} Alben</span>
            </div>
            <div className="shared-library-grid">
              {recentlySharedAlbums.map((album) => (
                <SharedAlbumCard
                  album={album}
                  key={album.id}
                  onOpen={() => openSharedAlbum(album.id)}
                />
              ))}
            </div>
          </section>
        ) : showOverview ? (
          <section aria-label="Übersicht" className="overview-content">
            <section aria-labelledby="recent-photos-title" className="overview-section">
              <div className="overview-section-heading">
                <h2 id="recent-photos-title">Zuletzt hinzugefügte Medien</h2>
                <button
                  className="overview-link"
                  onClick={() => showAllPhotos("photos")}
                  type="button"
                >
                  Alle Fotos <Icon name="chevron" size={15} />
                </button>
              </div>
              <div className="overview-photo-grid">
                {recentPhotos.map((photo) => (
                  <OverviewPhoto
                    key={photo.id}
                    onOpen={() => showAllPhotos("photos")}
                    photo={photo}
                  />
                ))}
              </div>
            </section>

            <section
              aria-labelledby="shared-photos-title"
              className="overview-section shared-photos-section"
            >
              <div className="overview-section-heading">
                <h2 id="shared-photos-title">Neu in geteilten Alben</h2>
                <button
                  className="overview-link"
                  onClick={() => showAllPhotos("shared")}
                  type="button"
                >
                  Alle geteilten Alben <Icon name="chevron" size={15} />
                </button>
              </div>
              <div className="overview-photo-grid">
                {recentSharedPhotos.map((photo) => (
                  <OverviewPhoto
                    key={photo.id}
                    context={`${recentlySharedAlbums.find((album) => album.id === photo.sharedAlbumId)?.name ?? "Geteiltes Album"} · von ${photo.sharedBy}`}
                    onOpen={() => openSharedAlbum(photo.sharedAlbumId)}
                    photo={photo}
                  />
                ))}
              </div>
            </section>
          </section>
        ) : (
          <section aria-label={pageTitle} className="gallery">
          <input
            accept="image/*"
            aria-label="Fotos zum Hochladen auswählen"
            className="visually-hidden"
            multiple
            onChange={handleUpload}
            ref={uploadInput}
            type="file"
          />
          {visiblePhotos.length ? (
            <div className="photo-grid">
              {visiblePhotos.map((photo) => (
                <PhotoCard
                  key={photo.id}
                  onFavorite={toggleFavorite}
                  photo={photo}
                />
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <Icon name="photos" size={24} />
              <strong>Keine Fotos gefunden</strong>
            </div>
          )}
          </section>
        )}
      </main>

      <aside aria-label="Profil und seltene Aktionen" className={`right-panel ${isAdmin ? "admin-right-panel" : ""}`}>
        <section className={`profile-card ${isAdmin ? "admin-profile-card" : ""}`}>
          <div className="profile-cover">
            <span className="profile-avatar">{isAdmin ? "A" : "S"}</span>
            <button
              aria-label="Profil bearbeiten"
              className="profile-edit"
              onClick={() => handleUtilityAction("Profil bearbeiten")}
              type="button"
            >
              <Icon name="edit" size={15} />
            </button>
          </div>
          <div className="profile-info">
            <h2>Bastian M.</h2>
            <span>{isAdmin ? "Administrator" : "@bastian"}</span>
          </div>
        </section>

        {!isAdmin && <section aria-label="Benachrichtigungen" className="notification-card">
          <div className="notification-card-icon">
            <Icon name="bell" size={18} />
            {latestNotification && <span />}
          </div>
          <div className="notification-card-copy">
            <strong>Benachrichtigungen</strong>
            {latestNotification ? (
              <>
                <span>{latestNotification.title}</span>
                <span className="notification-detail">{latestNotification.detail}</span>
              </>
            ) : (
              <span>Alles auf dem neuesten Stand.</span>
            )}
          </div>
        </section>}

        {isAdmin ? (
          <section aria-label="Systemstatus" className="admin-status-card">
            <span className="admin-status-indicator" />
            <span><strong>Systemstatus</strong><small>Alle Dienste aktiv</small></span>
          </section>
        ) : <section className="utility-section">
          <h2>Weitere Optionen</h2>
          <nav aria-label="Weitere Optionen" className="utility-nav">
            {utilityActions.map((action) => (
              <button
                className="utility-item"
                key={action.label}
                onClick={() =>
                  action.upload
                    ? uploadInput.current?.click()
                    : handleUtilityAction(action.label)
                }
                type="button"
              >
                <Icon name={action.icon} size={18} />
                <span>{action.label}</span>
                <Icon className="utility-chevron" name="chevron" size={15} />
              </button>
            ))}
          </nav>
        </section>}

        <button
          className="logout-button"
          onClick={() => handleUtilityAction("Abmelden")}
          type="button"
        >
          <Icon name="logout" size={17} />
          Abmelden
        </button>
      </aside>

      {notice && (
        <div aria-live="polite" className="toast" role="status">
          {notice}
        </div>
      )}
    </div>
  );
}

export default App;
