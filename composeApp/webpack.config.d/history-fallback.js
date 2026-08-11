// Enable SPA deep-link routing for Kotlin/Compose web dev server.
// This makes URLs like /phases/19696/matches/88976 serve index.html.
config.output = config.output || {};
config.output.publicPath = '/';

config.devServer = config.devServer || {};
config.devServer.historyApiFallback = {
  index: '/index.html',
};

// Proxy API calls to the Ktor backend so the web app works same-origin
// from any host (LAN IP, tunnel URL), not just localhost.
config.devServer.proxy = [
  {
    context: ['/api', '/health'],
    target: 'http://localhost:8090',
    changeOrigin: true,
  },
];

// Allow access from LAN IPs and tunnel hostnames.
config.devServer.allowedHosts = 'all';
