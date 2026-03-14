# JX IP Geo MaxMind

IP geolocation using MaxMind's GeoLite2 database.

## What It Solves

- `IpGeoMaxmindExtractor` resolves IP addresses to geographic information (country, city, coordinates) using the MaxMind GeoLite2 database

## Key Details

- Requires a GeoLite2 database file
- Used by the web server's DDoS filter for IP-based geolocation in request logging
