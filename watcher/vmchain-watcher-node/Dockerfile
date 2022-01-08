FROM node:16.3.0
WORKDIR /app
COPY . .
RUN pwd
RUN ls
RUN npm cache clean --force
RUN npm config set registry https://registry.npm.taobao.org
RUN yarn add @polkadot/api@5.8.3
RUN yarn add zksync@0.11.1
RUN yarn add ethers
RUN yarn add web3
RUN npm install

# 启动服务，默认挂载到7001端口
EXPOSE 7010
CMD ["npm", "run start"]
