FROM node:20-alpine
WORKDIR /app
COPY package.json package-lock.json* ./
COPY dist ./dist
RUN npm install --omit=dev --ignore-scripts
EXPOSE 8080
CMD ["node", "dist/index.js"]
