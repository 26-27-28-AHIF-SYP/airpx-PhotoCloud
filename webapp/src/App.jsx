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

function OverviewPhoto({ photo, onOpen }) {
  return (
    <button className="overview-photo" onClick={onOpen} type="button">
      <img alt="" loading="lazy" src={photo.image} />
      <span className="overview-photo-info">
        <strong>{photo.title}</strong>
        <span>{photo.date}</span>
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
    showNotice(`${label} ist in dieser Dashboard-Vorschau noch nicht verbunden.`);
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
            ? "Geteilte Fotos"
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

        <div className="workspace-switcher">
          <span className="workspace-avatar">S</span>
          <span>Mein Speicher</span>
          <span className="online-dot" />
        </div>

        <nav aria-label="Bibliothek" className="primary-nav">
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
          <button
            aria-current={activeView === "favorites" ? "page" : undefined}
            className={`nav-item ${activeView === "favorites" ? "active" : ""}`}
            onClick={() => showAllPhotos("favorites")}
            type="button"
          >
            <Icon name="heart" size={19} />
            <span>Favoriten</span>
          </button>
        </nav>

        <div className="sidebar-bottom">
          <button
            aria-current={activeView === "shared" ? "page" : undefined}
            className={`nav-item share-link ${activeView === "shared" ? "active" : ""}`}
            onClick={() => showAllPhotos("shared")}
            type="button"
          >
            <Icon name="shared" size={19} />
            <span>Teilen</span>
            <span className="nav-count">3</span>
          </button>
        </div>
      </aside>

      <main className="main-panel" id="dashboard">
        <header className="topbar">
          <h1>{pageTitle}</h1>
          <div className="topbar-actions">
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
            <button
              aria-label="Benachrichtigungen"
              className="icon-button notification-button"
              onClick={() => showNotice("Du bist auf dem neuesten Stand.")}
              type="button"
            >
              <Icon name="bell" size={19} />
              <span />
            </button>
          </div>
        </header>

        {showOverview ? (
          <section aria-label="Übersicht" className="overview-content">
            <section aria-labelledby="recent-photos-title" className="overview-section">
              <div className="overview-section-heading">
                <h2 id="recent-photos-title">Zuletzt hinzugefügt</h2>
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

            <section aria-labelledby="shared-albums-title" className="overview-section">
              <div className="overview-section-heading">
                <h2 id="shared-albums-title">Kürzlich geteilte Alben</h2>
                <button
                  className="overview-link"
                  onClick={() => showAllPhotos("shared")}
                  type="button"
                >
                  Alle geteilten <Icon name="chevron" size={15} />
                </button>
              </div>
              <div className="shared-album-grid">
                {recentlySharedAlbums.map((album) => (
                  <SharedAlbumCard
                    album={album}
                    key={album.id}
                    onOpen={() => openSharedAlbum(album.id)}
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
                  Alle ansehen <Icon name="chevron" size={15} />
                </button>
              </div>
              <div className="overview-photo-grid">
                {recentSharedPhotos.map((photo) => (
                  <OverviewPhoto
                    key={photo.id}
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

      <aside aria-label="Profil und seltene Aktionen" className="right-panel">
        <section className="profile-card">
          <div className="profile-cover">
            <span className="profile-avatar">S</span>
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
            <h2>Sebastian M.</h2>
            <span>@sebastian</span>
          </div>
        </section>

        <section className="utility-section">
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
        </section>

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
