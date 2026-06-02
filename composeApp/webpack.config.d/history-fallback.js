// Enable SPA deep-link routing for Kotlin/Compose web dev server.
// This makes URLs like /phases/19696/matches/88976 serve index.html.
config.output = config.output || {};
config.output.publicPath = '/';

config.devServer = config.devServer || {};
config.devServer.historyApiFallback = {
  index: '/index.html',
};

