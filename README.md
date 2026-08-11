<p align="center">
  <img src="app/src/main/res/drawable/logo.png" width="100" alt="Axios Logo"/>
</p>

<h1 align="center">Axios</h1>

<p align="center">
  An Android app for IIIT Lucknow — a centralized hub for club announcements, wing members, and shared resources.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/Auth-Firebase-orange?logo=firebase" />
  <img src="https://img.shields.io/badge/Min%20SDK-24-blue" />
</p>

---

## Features

| Feature | Description |
|---------|-------------|
| 🔐 **Google Sign-In** | Restricted to `@iiitl.ac.in` accounts only |
| 📢 **Announcements** | Post and view wing-specific announcements |
| 👥 **Members** | Manage members by wing, categorized by role (Coordinator, Senior Member, Member) |
| 📁 **Resources** | Upload and share files (PDFs, docs, images) per wing via Cloudinary |
| 🌙 **Dark Mode** | Toggle between light and dark themes, persisted across sessions |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Views + ViewBinding + Material Design |
| Navigation | Fragment back stack + BottomNavigationView |
| Auth | Firebase Authentication (Google Sign-In) |
| Database | Cloud Firestore |
| File Storage | Cloudinary (unsigned upload preset) |
| Local Cache | SharedPreferences (JSON serialization) |

---

## Project Structure

```
app/src/main/java/com/example/axios/
├── data/
│   └── DataRepository.kt        # Singleton — Firestore sync, local cache, Cloudinary upload
├── ui/
│   ├── home/
│   │   └── HomeFragment.kt      # Announcements feed
│   ├── members/
│   │   ├── MembersWingFragment.kt   # Wing selector for members
│   │   └── MembersFragment.kt       # Members list by wing & role
│   ├── resources/
│   │   ├── ResourcesWingFragment.kt # Wing selector for resources
│   │   └── ResourcesFragment.kt     # File list + upload for a wing
│   └── settings/
│       └── SettingsFragment.kt  # User profile, theme toggle, logout
├── adapter/
│   ├── AnnouncementAdapter.kt
│   ├── WingAdapter.kt
│   └── ResourceAdapter.kt
├── MainActivity.kt              # Host activity with bottom nav
└── LoginActivity.kt             # Google Sign-In entry point
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- A Firebase project with **Authentication** (Google provider) and **Firestore** enabled
- A [Cloudinary](https://cloudinary.com) account with an **unsigned upload preset**

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/oceankesarwani/Axios.git
   cd Axios
   ```

2. **Add `google-services.json`**

   Download it from your Firebase project console and place it at:
   ```
   app/google-services.json
   ```

3. **Configure Cloudinary credentials**

   Add the following to your `local.properties` file (never commit this file):
   ```properties
   CLOUDINARY_CLOUD_NAME=your_cloud_name
   CLOUDINARY_UPLOAD_PRESET=your_unsigned_preset_name
   ```

4. **Build and run**
   ```bash
   ./gradlew installDebug
   ```

---

## Architecture

Axios uses a **single-activity, multi-fragment** architecture with a singleton data layer:

```
MainActivity
└── BottomNavigationView
    ├── HomeFragment          ← reads DataRepository.announcements
    ├── ResourcesWingFragment → ResourcesFragment
    ├── MembersWingFragment   → MembersFragment
    └── SettingsFragment

DataRepository (object / singleton)
    ├── loadLocalData()       ← SharedPreferences on app start
    ├── syncFromFirestore()   ← parallel Firestore fetches on init
    └── save*() helpers       ← per-collection saves (race-condition safe)
```

Data flows: **Firestore → in-memory lists → SharedPreferences cache → UI adapters**

---

## Security Notes

- Google Sign-In enforces `@iiitl.ac.in` domain — unauthorized accounts are rejected immediately
- Cloudinary credentials are stored in `local.properties` (gitignored) and injected via `BuildConfig` at build time — they are **never hardcoded in source**
- No API secret is ever included in the app — only an unsigned upload preset is used

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push and open a Pull Request

---

## License

This project is for internal use at IIIT Lucknow. All rights reserved.
