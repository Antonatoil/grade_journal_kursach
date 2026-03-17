from node:20-alpine as build
workdir /app

copy package*.json ./
run npm install

copy . .

arg VITE_API_BASE_URL=http://localhost:8080
env VITE_API_BASE_URL=$VITE_API_BASE_URL

run npm run build

from nginx:1.27-alpine

copy nginx.conf /etc/nginx/conf.d/default.conf
copy --from=build /app/dist /usr/share/nginx/html

expose 80

cmd ["nginx", "-g", "daemon off;"]