# ══════════════════════════════════════════════════
# Dockerfile — Orivya Rice Frontend (Nginx)
# Serves HTML/CSS/JS as static files
# ══════════════════════════════════════════════════

FROM nginx:1.25-alpine

# Remove default nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy all frontend files
COPY . /usr/share/nginx/html/

# Copy our custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s \
  CMD wget -q --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
