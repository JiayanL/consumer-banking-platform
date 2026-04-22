FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY dist ./dist
EXPOSE 8090
CMD ["node", "dist/server.js"]
