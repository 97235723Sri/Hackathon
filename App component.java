<div class="app-container">
  <header class="app-header">
    <h1>{{ title }}</h1>
    <nav class="nav-menu">
      <a routerLink="/control-panel" routerLinkActive="active">Control Panel</a>
      <a routerLink="/button-config" routerLinkActive="active">Button Configuration</a>
    </nav>
  </header>

  <main class="app-content">
    <router-outlet></router-outlet>
  </main>

  <footer class="app-footer">
    <p>&copy; 2025 Application Control Panel. All rights reserved.</p>
  </footer>
</div>
