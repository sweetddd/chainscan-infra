FROM node:16.3.0
WORKDIR /app
COPY . .
RUN pwd
RUN ls
RUN npm cache clean --force
RUN npm config set registry https://registry.npm.taobao.org
RUN yarn add @polkadot/api@5.8.3
RUN yarn add ethers@5.5.3
RUN yarn add web3
RUN npm install

# 启动服务，默认挂载到7011端口
EXPOSE 7011
CMD ["npm", "run start"]
