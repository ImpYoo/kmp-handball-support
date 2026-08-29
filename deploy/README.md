# Handball Support public deployment

1. Replace `mydomain.tld` in `Caddyfile` with your real TLD.
2. Copy `.env.example` to `.env` and fill secrets.
3. Put `tournaments.json` and `auth-users.json` into `./data/`.
4. Run `docker compose up -d`.
5. Point DNS A records for both subdomains to the public IP of the FritzBox.
6. Forward TCP 80 and 443 on the FritzBox to this host.
